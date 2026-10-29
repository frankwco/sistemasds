import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:intl/intl.dart';

import '../core/config.dart';
import 'api_client.dart';
import 'catalogo_repo.dart';
import 'coletas_repo.dart';
import 'espelho_firebase.dart';
import 'pontos_repo.dart';
import 'sessao_repo.dart';

class SyncEstado {
  const SyncEstado({this.sincronizando = false, this.enviando = false, this.pendentes = 0, this.ultimaSync, this.erro});

  /// Rodada completa em andamento (envio + coletas + pontos + catálogos).
  final bool sincronizando;

  /// Enviando coletas ao servidor agora.
  final bool enviando;

  /// Coletas (e exclusões) ainda não confirmadas pelo servidor.
  final int pendentes;
  final DateTime? ultimaSync;

  /// Motivo da última falha, ou null se a última rodada foi bem.
  final String? erro;

  SyncEstado copiar({bool? sincronizando, bool? enviando, int? pendentes, DateTime? ultimaSync, String? erro, bool limparErro = false}) =>
      SyncEstado(
        sincronizando: sincronizando ?? this.sincronizando,
        enviando: enviando ?? this.enviando,
        pendentes: pendentes ?? this.pendentes,
        ultimaSync: ultimaSync ?? this.ultimaSync,
        erro: limparErro ? null : (erro ?? this.erro),
      );
}

/// Único ponto que fala com o servidor sobre coletas e pontos.
///
/// Substitui o conjunto de timers, flags de pausa e "pedido adiado" do app Ionic. Como o
/// banco é transacional e a assinatura é desenhada sem tocar nele, não há o que pausar:
/// cada tarefa roda uma de cada vez e cada gravação é atômica.
class SyncService {
  SyncService({
    required ApiClient api,
    required ColetasRepo coletas,
    required PontosRepo pontos,
    required CatalogoRepo catalogos,
    required SessaoRepo sessao,
    required SessaoEstado Function() sessaoAtual,
    EspelhoFirebase? espelho,
  })  : _api = api,
        _coletas = coletas,
        _pontos = pontos,
        _catalogos = catalogos,
        _sessao = sessao,
        _sessaoAtual = sessaoAtual,
        _espelho = espelho;

  final ApiClient _api;
  final ColetasRepo _coletas;
  final PontosRepo _pontos;
  final CatalogoRepo _catalogos;
  final SessaoRepo _sessao;
  final SessaoEstado Function() _sessaoAtual;
  final EspelhoFirebase? _espelho;

  final ValueNotifier<SyncEstado> estado = ValueNotifier(const SyncEstado());

  Timer? _timer;
  StreamSubscription<List<ConnectivityResult>>? _conexao;
  bool _estavaOffline = false;

  Timer? _timerReenvio;
  int _falhasSeguidas = 0;

  Future<void>? _envioAtual;
  bool _repetirEnvio = false;
  Future<void>? _rodadaAtual;

  final DateFormat _fmtApi = DateFormat('yyyy-MM-dd HH:mm:ss');

  // ─── Agenda ────────────────────────────────────────────

  /// Liga a sincronização automática: a cada 2 minutos e assim que a internet voltar.
  /// Seguro chamar mais de uma vez.
  void iniciar() {
    _timer ??= Timer.periodic(Config.intervaloSync, (_) => sincronizar());
    _conexao ??= Connectivity().onConnectivityChanged.listen((resultados) {
      final offline = resultados.isEmpty || resultados.every((r) => r == ConnectivityResult.none);
      if (!offline && _estavaOffline) {
        sincronizar();
      }
      _estavaOffline = offline;
    });
    atualizarPendentes();
  }

  void parar() {
    _timer?.cancel();
    _timer = null;
    _conexao?.cancel();
    _conexao = null;
    _timerReenvio?.cancel();
    _timerReenvio = null;
  }

  Future<void> atualizarPendentes() async {
    final n = await _coletas.contarPendentes();
    estado.value = estado.value.copiar(pendentes: n);
  }

  // ─── Envio das coletas pendentes ───────────────────────

  /// Envia a caixa de saída. Chamada logo depois de finalizar uma coleta, e também no
  /// início de cada rodada. Pedidos que chegam com um envio em andamento viram uma
  /// repetição ao fim dele, então uma coleta recém-gravada nunca espera o próximo ciclo.
  Future<void> enviarPendentes() {
    final atual = _envioAtual;
    if (atual != null) {
      _repetirEnvio = true;
      return atual;
    }
    return _envioAtual = _enviarEmLoop().whenComplete(() => _envioAtual = null);
  }

  Future<void> _enviarEmLoop() async {
    estado.value = estado.value.copiar(enviando: true);
    try {
      do {
        _repetirEnvio = false;
        await _enviarLote();
      } while (_repetirEnvio);
    } finally {
      await atualizarPendentes();
      estado.value = estado.value.copiar(enviando: false);
      _agendarReenvio();
    }
  }

  /// Falhou? Tenta de novo logo (15 s, 30 s, 60 s, até 2 min) em vez de esperar o próximo
  /// ciclo da rodada completa. Zera a espera quando a caixa de saída esvazia.
  void _agendarReenvio() {
    _timerReenvio?.cancel();
    _timerReenvio = null;
    if (estado.value.pendentes == 0) {
      _falhasSeguidas = 0;
      return;
    }
    final segundos = (15 * (1 << _falhasSeguidas)).clamp(15, 120);
    if (_falhasSeguidas < 4) _falhasSeguidas++;
    _timerReenvio = Timer(Duration(seconds: segundos), () => enviarPendentes().catchError((_) {}));
  }

  Future<void> _enviarLote() async {
    final usuario = _sessaoAtual().usuario;
    final pendentes = await _coletas.pendentes();
    for (final c in pendentes) {
      if (!await _coletas.reservarEnvio(c.id)) continue; // outro envio já está com ela
      try {
        final corpo = Map<String, dynamic>.from(c.json)
          ..['status'] = c.ativa
          ..['sincronizado'] = false
          ..['acessoTotal'] = usuario?.acessoTotal
          ..['assinatura'] = await _assinaturaBase64(c.arquivoAssinatura);
        // Mais tempo que as consultas: o servidor processa a coleta (e a assinatura) antes de responder.
        await _api.cadastrarColeta([corpo], decodificar: false, limite: Config.timeoutEnvioColeta);
        if (!await _coletas.marcarEnviada(c)) {
          // Mudou durante o envio (foi excluída): manda o estado novo logo em seguida.
          _repetirEnvio = true;
        }
        await atualizarPendentes();
      } on ApiException catch (e) {
        await _coletas.registrarFalha(c.id, e.descricao);
        estado.value = estado.value.copiar(erro: 'Coleta não enviada: ${e.descricao}');
        // Sem conexão não adianta tentar as demais; erro do servidor numa coleta não
        // impede as outras (ela fica na fila e é tentada de novo no próximo ciclo).
        if (e.semConexao) break;
      } catch (e) {
        // Falha local (ex.: arquivo da assinatura ilegível) só prejudica esta coleta.
        debugPrint('[Sync] erro local ao enviar ${c.id}: $e');
        await _coletas.registrarFalha(c.id, 'erro no aparelho: $e');
      }
    }
  }

  Future<String> _assinaturaBase64(String? arquivo) async {
    if (arquivo == null) return '';
    final f = File(arquivo);
    if (!await f.exists()) return '';
    return 'data:image/png;base64,${base64Encode(await f.readAsBytes())}';
  }

  // ─── Rodada completa ───────────────────────────────────

  /// Envia pendentes, recebe a lista de coletas do servidor e atualiza os pontos.
  /// [pontosCompleto] baixa a lista inteira de pontos (a primeira carga é sempre completa);
  /// [catalogos] atualiza cidades, veículos e motoristas.
  Future<void> sincronizar({bool pontosCompleto = false, bool catalogos = false}) {
    final atual = _rodadaAtual;
    if (atual != null) {
      // Pedido comum durante uma rodada: ela já cobre. Pedido específico (ex.: botão
      // "sincronizar tudo") roda assim que a atual terminar.
      if (!pontosCompleto && !catalogos) return atual;
      return atual.then((_) => sincronizar(pontosCompleto: pontosCompleto, catalogos: catalogos));
    }
    return _rodadaAtual = _rodada(pontosCompleto: pontosCompleto, catalogos: catalogos)
        .whenComplete(() => _rodadaAtual = null);
  }

  Future<void> _rodada({required bool pontosCompleto, required bool catalogos}) async {
    estado.value = estado.value.copiar(sincronizando: true);
    String? erro;
    try {
      await enviarPendentes();
      // Backup no Firebase: refaz o que ficou pendente (sem esperar: pode demorar sem internet).
      unawaited(_espelho?.espelharPendentes() ?? Future<void>.value());
      await _baixarColetas();
      final completo = pontosCompleto || await _pontos.total() == 0;
      await _baixarPontos(completo: completo);
      if (catalogos || completo || await _catalogos.vazio(CatalogoRepo.cidades)) {
        await _baixarCatalogos();
      }
    } on ApiException catch (e) {
      erro = e.descricao;
    } catch (e) {
      debugPrint('[Sync] falha inesperada: $e');
      erro = 'falha ao sincronizar';
    } finally {
      await atualizarPendentes();
      estado.value = estado.value.copiar(
        sincronizando: false,
        ultimaSync: erro == null ? DateTime.now() : estado.value.ultimaSync,
        erro: erro,
        limparErro: erro == null,
      );
    }
  }

  /// Pedido "lista": o servidor devolve as coletas do veículo para mesclar no aparelho.
  Future<void> _baixarColetas() async {
    final sessao = _sessaoAtual();
    final agora = DateTime.now();
    final lista = await _api.cadastrarColeta([
      {
        'acessoTotal': sessao.usuario?.acessoTotal,
        'tipo': 'lista',
        'identificadorApp': '',
        'status': false,
        'veiculo': sessao.veiculo?.raw ?? {'id': null, 'descricao': '', 'placa': ''},
        'diaUltimaColeta': agora.day,
        'mesUltimaColeta': agora.month,
        'anoUltimaColeta': agora.year,
        'horaUltimaColeta': agora.hour,
        'minutoUltimaColeta': agora.minute,
        'segundoUltimaColeta': agora.second,
      }
    ]);
    await _coletas.mesclarDoServidor(lista);
  }

  Future<void> _baixarPontos({required bool completo}) async {
    // Marca o instante ANTES da chamada: o que mudar no servidor durante o download entra
    // na próxima rodada (marcar depois perderia essas mudanças).
    final inicio = DateTime.now();
    String parametro;
    if (completo) {
      parametro = 'nao';
    } else {
      final ultima = DateTime.tryParse((await _sessao.lerUltimaAtualizacaoPontos())?.replaceFirst(' ', 'T') ?? '');
      parametro = ultima == null
          ? '2000-03-14 22:38:05'
          : _fmtApi.format(ultima.subtract(const Duration(minutes: 3)));
    }
    final pontos = await _api.pontosReduzidos(parametro);
    if (completo || pontos.isNotEmpty) {
      await _pontos.mesclar(pontos, completo: completo);
    }
    await _sessao.gravarUltimaAtualizacaoPontos(_fmtApi.format(inicio));
  }

  Future<void> _baixarCatalogos() async {
    // Cada lista é independente: uma falha não impede as outras (e só a primeira é relatada).
    Future<ApiException?> baixar(Future<List<dynamic>> Function() buscar, String tipo) async {
      try {
        await _catalogos.substituir(tipo, await buscar());
        return null;
      } on ApiException catch (e) {
        return e;
      }
    }

    final falhas = await Future.wait([
      baixar(_api.cidades, CatalogoRepo.cidades),
      baixar(_api.veiculos, CatalogoRepo.veiculos),
      baixar(_api.motoristas, CatalogoRepo.motoristas),
    ]);
    for (final f in falhas) {
      if (f != null) throw f;
    }
  }

  void dispose() {
    parar();
    estado.dispose();
  }
}
