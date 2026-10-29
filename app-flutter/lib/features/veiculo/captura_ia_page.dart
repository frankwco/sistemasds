import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';
import 'package:speech_to_text/speech_recognition_error.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../data/ia_logic.dart';
import '../../providers.dart';
import 'componentes.dart';

class _Mensagem {
  _Mensagem({required this.enviada, this.texto, this.icone, this.pendente = false, this.status = false});
  final bool enviada;
  String? texto;
  final IconData? icone;
  bool pendente;
  bool status;
}

const _mensagensProcessando = ['Analisando com IA...', 'Processando...', 'Interpretando os dados...', 'Quase lá...'];

/// Lançamento por IA: o motorista anexa uma foto (cupom, nota) e/ou fala/digita um complemento e envia
/// tudo numa chamada; a IA extrai os campos e o app mostra um formulário para CONFERIR. Nada é salvo
/// antes de confirmar.
class CapturaIaPage extends ConsumerStatefulWidget {
  const CapturaIaPage({super.key});

  @override
  ConsumerState<CapturaIaPage> createState() => _CapturaIaPageState();
}

class _CapturaIaPageState extends ConsumerState<CapturaIaPage> {
  // Catálogos ao vivo.
  List<Map<String, dynamic>> _categorias = [], _fornecedores = [], _veiculos = [], _motoristas = [];
  bool _carregandoCatalogos = true;

  // Veículo e motorista desta captura (a IA pode trocá-los; não mexe na escolha da tela inicial).
  Map<String, dynamic>? _veiculo;
  Map<String, dynamic>? _motorista;

  // Chat.
  final List<_Mensagem> _mensagens = [];
  final _texto = TextEditingController();
  final _rolagem = ScrollController();
  Uint8List? _foto;
  String? _fotoNome;
  bool _enviando = false;
  bool _gravando = false;
  bool _transcrevendo = false;
  String _msgTranscrevendo = 'Transcrevendo áudio...';
  String _textoAntesDaFala = '';
  final _gravador = AudioRecorder();
  String? _caminhoAudio;
  final _fala = stt.SpeechToText();
  bool _falaPronta = false;
  bool _ditando = false;
  bool _recebeuNoCiclo = false;
  int _ciclosVazios = 0;
  String _baseDitado = '';
  String? _localeFala;
  final List<Timer> _timers = [];

  // Revisão.
  bool _revisando = false;
  bool _salvando = false;
  String? _origem;
  String _tipo = 'Despesa';
  int? _categoriaId;
  int? _fornecedorId;
  String? _fornecedorSugerido;
  String? _combustivel;
  String? _numeroDocumento;
  double? _confianca;
  String _textoBruto = '';
  bool _mostrarTextoBruto = false;
  String? _avisoDuplicidade;
  Uint8List? _fotoParaAnexo;
  final _valor = TextEditingController();
  final _litros = TextEditingController();
  final _valorLitro = TextEditingController();
  final _km = TextEditingController();
  final _observacao = TextEditingController();

  @override
  void initState() {
    super.initState();
    final s = ref.read(sessaoProvider);
    _veiculo = s.veiculo?.raw;
    _motorista = s.motorista?.raw;
    _carregarCatalogos();
  }

  @override
  void dispose() {
    for (final t in _timers) {
      t.cancel();
    }
    _gravador.dispose();
    _ditando = false;
    _fala.cancel();
    for (final c in [_texto, _valor, _litros, _valorLitro, _km, _observacao]) {
      c.dispose();
    }
    _rolagem.dispose();
    super.dispose();
  }

  List<Map<String, dynamic>> _lista(List<dynamic> l) => [for (final i in l) if (i is Map) Map<String, dynamic>.from(i)];

  /// Tudo direto do servidor a cada abertura: um cadastro feito no admin aparece na hora.
  Future<void> _carregarCatalogos() async {
    try {
      final api = ref.read(apiProvider);
      final r = await Future.wait([api.categoriasDespesa(), api.fornecedores(), api.veiculos(), api.motoristas()]);
      if (!mounted) return;
      setState(() {
        _categorias = _lista(r[0]);
        _fornecedores = _lista(r[1]);
        _veiculos = _lista(r[2]);
        _motoristas = _lista(r[3]);
      });
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível carregar os catálogos (${e.descricao}). Verifique sua conexão.');
    } finally {
      if (mounted) setState(() => _carregandoCatalogos = false);
    }
  }

  void _rolarFim() => WidgetsBinding.instance.addPostFrameCallback((_) {
        if (_rolagem.hasClients) _rolagem.jumpTo(_rolagem.position.maxScrollExtent);
      });

  /// Troca a mensagem de status a cada 2,5 s enquanto a IA trabalha. Devolve quem para a rotação.
  VoidCallback _girarMensagens(void Function(String) aoMudar) {
    var i = 0;
    final t = Timer.periodic(const Duration(milliseconds: 2500), (_) {
      i = (i + 1) % _mensagensProcessando.length;
      if (mounted) setState(() => aoMudar(_mensagensProcessando[i]));
    });
    _timers.add(t);
    return t.cancel;
  }

  // ─── Foto ──────────────────────────────────────────────

  Future<void> _anexarFoto() async {
    final origem = await showModalBottomSheet<ImageSource>(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(leading: const Icon(Icons.photo_camera_outlined), title: const Text('Tirar foto'), onTap: () => Navigator.pop(ctx, ImageSource.camera)),
          ListTile(leading: const Icon(Icons.image_outlined), title: const Text('Escolher da galeria'), onTap: () => Navigator.pop(ctx, ImageSource.gallery)),
        ]),
      ),
    );
    if (origem == null) return;
    try {
      final f = await ImagePicker().pickImage(source: origem, imageQuality: 70, maxWidth: 1600, maxHeight: 1600);
      if (f == null || !mounted) return;
      final bytes = await f.readAsBytes();
      setState(() {
        _foto = bytes;
        _fotoNome = 'comprovante-${DateTime.now().millisecondsSinceEpoch}.jpg';
        _mensagens.add(_Mensagem(enviada: true, icone: Icons.image_outlined, texto: _fotoNome, pendente: true));
      });
      _rolarFim();
    } catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível abrir a câmera/galeria. Verifique as permissões do app.');
    }
  }

  void _removerFoto() {
    setState(() {
      _foto = null;
      _fotoNome = null;
      final i = _mensagens.lastIndexWhere((m) => m.pendente);
      if (i >= 0) _mensagens.removeAt(i);
    });
  }

  // ─── Ditado nativo (reconhecimento do Android, ao vivo) ─────────

  /// Liga/desliga o ditado: o texto aparece no campo enquanto a pessoa fala. O reconhecedor do Android
  /// encerra a escuta sozinho depois de uma pausa; enquanto o ditado estiver ligado, ele é religado
  /// e o texto continua de onde parou.
  Future<void> _alternarDitado() async {
    if (_ditando) {
      await _pararDitado();
      return;
    }
    try {
      if (!_falaPronta) {
        _falaPronta = await _fala.initialize(onStatus: _aoStatusFala, onError: _aoErroFala);
      }
      if (!_falaPronta) {
        if (mounted) {
          mostrarAviso(context,
              'Este celular não tem reconhecimento de voz disponível (ou o microfone não foi permitido). Use o botão de gravar para a IA transcrever.');
        }
        return;
      }
      final locais = await _fala.locales();
      _localeFala = locais.where((l) => l.localeId.replaceAll('-', '_').toLowerCase() == 'pt_br').firstOrNull?.localeId;
      _baseDitado = _texto.text.trim();
      _ciclosVazios = 0;
      if (mounted) setState(() => _ditando = true);
      await _escutar();
    } catch (e) {
      debugPrint('[CapturaIA] ditado não iniciou: $e');
      if (mounted) {
        setState(() => _ditando = false);
        mostrarAviso(context, 'Não foi possível iniciar o ditado. Tente novamente ou digite.');
      }
    }
  }

  Future<void> _escutar() async {
    if (!_ditando || !mounted) return;
    _recebeuNoCiclo = false;
    await _fala.listen(
      onResult: (r) {
        if (!mounted || !_ditando) return;
        if (r.recognizedWords.isNotEmpty) _recebeuNoCiclo = true;
        final falado = r.recognizedWords.trim();
        setState(() {
          _texto.text = _baseDitado.isEmpty ? falado : '$_baseDitado $falado'.trim();
          _texto.selection = TextSelection.collapsed(offset: _texto.text.length);
        });
        if (r.finalResult && falado.isNotEmpty) _baseDitado = _texto.text.trim();
      },
      localeId: _localeFala,
      listenFor: const Duration(minutes: 2),
      pauseFor: const Duration(seconds: 6),
      listenOptions: stt.SpeechListenOptions(partialResults: true, listenMode: stt.ListenMode.dictation, cancelOnError: false),
    );
  }

  void _aoStatusFala(String status) {
    if (!_ditando || !mounted) return;
    if (status != 'done' && status != 'notListening') return;
    _baseDitado = _texto.text.trim();
    _ciclosVazios = _recebeuNoCiclo ? 0 : _ciclosVazios + 1;
    if (_ciclosVazios >= 5) {
      // Ninguém falou por vários ciclos seguidos: desliga em vez de ficar escutando para sempre.
      _pararDitado();
      return;
    }
    Future<void>.delayed(const Duration(milliseconds: 300), _escutar);
  }

  void _aoErroFala(SpeechRecognitionError e) {
    debugPrint('[CapturaIA] erro no ditado: ${e.errorMsg} (permanente: ${e.permanent})');
    if (!_ditando || !mounted) return;
    // Silêncio e "não entendi" são normais: o status 'done' religa a escuta. O resto encerra o ditado.
    if (e.errorMsg == 'error_no_match' || e.errorMsg == 'error_speech_timeout') return;
    _pararDitado();
    final msg = switch (e.errorMsg) {
      'error_permission' => 'Permita o uso do microfone para ditar.',
      'error_network' || 'error_network_timeout' || 'error_server' =>
        'O reconhecimento de voz precisa de internet neste celular. Tente de novo ou digite.',
      _ => 'O ditado parou (${e.errorMsg}). Tente novamente ou digite.',
    };
    mostrarAviso(context, msg);
  }

  Future<void> _pararDitado() async {
    if (mounted) setState(() => _ditando = false);
    try {
      await _fala.stop();
    } catch (_) {}
  }

  // ─── Voz ───────────────────────────────────────────────

  /// Grava o áudio inteiro sem interrupção e manda para a IA transcrever ao parar. O reconhecimento ao
  /// vivo do Android corta a escuta a cada pausa e perde pedaços da fala; gravar arquivo não tem esse limite.
  Future<void> _alternarGravacao() async {
    if (_gravando) {
      await _pararETranscrever();
      return;
    }
    try {
      if (!await _gravador.hasPermission()) {
        if (mounted) mostrarAviso(context, 'Permita o uso do microfone para gravar um complemento por voz.');
        return;
      }
      _textoAntesDaFala = _texto.text.trim();
      final dir = await getTemporaryDirectory();
      _caminhoAudio = '${dir.path}/captura_audio_${DateTime.now().millisecondsSinceEpoch}.3gp';
      await _gravador.start(
        // MediaRecorder do sistema (o mesmo que o plugin Media do app antigo): o gravador novo do plugin
        // monta o 3gp por MediaCodec/Muxer e só funciona no Android 8+.
        const RecordConfig(
          encoder: AudioEncoder.amrNb,
          sampleRate: 8000,
          numChannels: 1,
          bitRate: 12200,
          androidConfig: AndroidRecordConfig(useLegacy: true),
        ),
        path: _caminhoAudio!,
      );
      if (mounted) setState(() => _gravando = true);
    } catch (e) {
      debugPrint('[CapturaIA] gravação não iniciou: $e');
      if (mounted) {
        setState(() => _gravando = false);
        mostrarAviso(context, 'Não foi possível usar o microfone. Tente novamente ou digite.');
      }
    }
  }

  Future<void> _pararETranscrever() async {
    setState(() {
      _gravando = false;
      _transcrevendo = true;
      _msgTranscrevendo = 'Transcrevendo áudio...';
    });
    final parar = _girarMensagens((t) => _msgTranscrevendo = t);
    File? arquivo;
    try {
      final caminho = await _gravador.stop() ?? _caminhoAudio;
      if (caminho == null) throw StateError('sem arquivo de áudio');
      arquivo = File(caminho);
      final bytes = await arquivo.readAsBytes();
      if (bytes.length < 1000) {
        debugPrint('[CapturaIA] áudio vazio: ${bytes.length} bytes');
        if (mounted) mostrarAviso(context, 'A gravação saiu vazia. Fale mais perto do microfone e tente de novo.');
        return;
      }
      final base64 = base64Encode(bytes);
      final r = await ref.read(apiProvider).interpretarAudio(base64, 'audio/3gpp');
      final transcrito = texto(r['textoBruto']).trim();
      if (!mounted) return;
      if (transcrito.isEmpty) {
        mostrarAviso(context, 'Não consegui transcrever o áudio. Tente novamente ou digite.');
      } else {
        _texto.text = _textoAntesDaFala.isEmpty ? transcrito : '$_textoAntesDaFala $transcrito';
      }
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível transcrever o áudio (${e.descricao}). Tente novamente ou digite.');
    } catch (e) {
      debugPrint('[CapturaIA] transcrição: $e');
      if (mounted) mostrarAviso(context, 'Não foi possível transcrever o áudio. Tente novamente ou digite.');
    } finally {
      parar();
      try {
        arquivo?.deleteSync();
      } catch (_) {}
      if (mounted) setState(() => _transcrevendo = false);
    }
  }

  // ─── Envio para a IA ───────────────────────────────────

  Future<void> _enviarParaIa() async {
    if (_veiculo == null) {
      mostrarAviso(context, 'Selecione um veículo na tela inicial antes de usar a captura por IA.');
      return;
    }
    final texto = _texto.text.trim();
    final foto = _foto;
    if (foto == null && texto.isEmpty) {
      mostrarAviso(context, 'Anexe uma foto ou fale/digite algo antes de enviar.');
      return;
    }
    for (final m in _mensagens) {
      m.pendente = false;
    }
    if (texto.isNotEmpty) _mensagens.add(_Mensagem(enviada: true, texto: texto));
    final bolha = _Mensagem(enviada: false, texto: _mensagensProcessando.first, status: true);
    _mensagens.add(bolha);
    final fotoNome = _fotoNome;
    setState(() {
      _foto = null;
      _fotoNome = null;
      _texto.clear();
      _enviando = true;
    });
    _rolarFim();
    final parar = _girarMensagens((t) => bolha.texto = t);

    try {
      final api = ref.read(apiProvider);
      final Map<String, dynamic> r;
      final String origem;
      if (foto != null) {
        r = await api.interpretarFotoTexto(base64Encode(foto), 'image/jpeg', texto.isEmpty ? null : texto);
        origem = 'IA_Foto';
      } else {
        r = await api.interpretarTexto(texto);
        origem = 'IA_Voz';
      }
      await _aplicarResultado(r, origem, foto, fotoNome);
      bolha.texto = 'Extraí os dados — confira no formulário';
    } on ApiException catch (e) {
      bolha.texto = 'Não consegui extrair os dados dessa vez (${e.descricao}). Tente novamente.';
    } catch (e) {
      debugPrint('[CapturaIA] processamento: $e');
      bolha.texto = 'Não consegui extrair os dados dessa vez. Tente novamente.';
    } finally {
      parar();
      bolha.status = false;
      if (mounted) setState(() => _enviando = false);
      _rolarFim();
    }
  }

  String _num(Object? v) {
    final n = numero(v);
    return n == null ? '' : n.toString().replaceAll('.', ',');
  }

  Future<void> _aplicarResultado(Map<String, dynamic> r, String origem, Uint8List? foto, String? fotoNome) async {
    var obs = texto(r['observacao']);
    void somarObs(String t) => obs = obs.isEmpty ? t : '$obs — $t';

    final veiculoTxt = texto(r['veiculo']);
    if (veiculoTxt.isNotEmpty) {
      final achado = casarVeiculo(_veiculos, veiculoTxt);
      if (achado != null) {
        _veiculo = achado;
      } else {
        somarObs('Veículo sugerido: $veiculoTxt');
      }
    }
    final motoristaTxt = texto(r['motorista']);
    if (motoristaTxt.isNotEmpty) {
      final achado = casarPorNome(_motoristas, motoristaTxt);
      if (achado != null) {
        _motorista = achado;
      } else {
        somarObs('Motorista sugerido: $motoristaTxt');
      }
    }
    final categoria = texto(r['categoria']).isEmpty ? null : casarCategoria(_categorias, texto(r['categoria']));
    final fornecedorTxt = texto(r['fornecedor']);
    final fornecedor = fornecedorTxt.isEmpty ? null : casarPorNome(_fornecedores, fornecedorTxt);
    String? sugerido;
    if (fornecedorTxt.isNotEmpty && fornecedor == null) {
      sugerido = fornecedorTxt;
      somarObs('Fornecedor "$fornecedorTxt" será cadastrado automaticamente');
    }

    // Completa o terceiro valor do abastecimento (litros x valor/litro = total).
    final calc = recalcularAbastecimento(
        litros: numero(r['litros']), valorLitro: numero(r['valorLitro']), valor: numero(r['valor']));

    final numDoc = texto(r['numeroDocumento']);
    final kmIa = numero(r['km']);
    String? aviso;
    final veiculoId = _veiculo == null ? null : inteiro(_veiculo!['id']);
    if (veiculoId != null && veiculoId > 0 && ((kmIa ?? 0) > 0 || numDoc.isNotEmpty)) {
      aviso = await ref.read(apiProvider).verificarDuplicidade(veiculoId: veiculoId, km: kmIa, numeroDocumento: numDoc);
    }
    if (!mounted) return;

    setState(() {
      _origem = origem;
      _tipo = texto(r['tipo']).isEmpty ? 'Despesa' : texto(r['tipo']);
      _valor.text = _num(calc.valor);
      _litros.text = _num(r['litros']);
      _valorLitro.text = _num(calc.valorLitro);
      _km.text = kmIa == null ? '' : kmIa.round().toString();
      _combustivel = normalizarCombustivel(texto(r['combustivel']));
      _observacao.text = obs;
      _numeroDocumento = numDoc.isEmpty ? null : numDoc;
      _confianca = numero(r['confianca']);
      _textoBruto = texto(r['textoBruto']);
      _categoriaId = categoria == null ? null : inteiro(categoria['id']);
      _fornecedorId = fornecedor == null ? null : inteiro(fornecedor['id']);
      _fornecedorSugerido = sugerido;
      _avisoDuplicidade = aviso;
      _fotoParaAnexo = foto;
      _revisando = true;
    });
  }

  void _recalcular() {
    final calc = recalcularAbastecimento(
        litros: lerDecimal(_litros.text), valorLitro: lerDecimal(_valorLitro.text), valor: lerDecimal(_valor.text));
    setState(() {
      if (calc.valor != null && _valor.text.trim().isEmpty) _valor.text = _num(calc.valor);
      if (calc.valorLitro != null && _valorLitro.text.trim().isEmpty) _valorLitro.text = _num(calc.valorLitro);
    });
  }

  Future<void> _cadastrarFornecedor() async {
    final nome = await pedirNome(context, 'Novo fornecedor', 'Nome do fornecedor');
    if (nome == null || !mounted) return;
    try {
      final criado = await ref.read(apiProvider).criarFornecedor(nome, tipo: _tipo == 'Abastecimento' ? 'Posto' : null);
      if (!mounted) return;
      setState(() {
        _fornecedores = [..._fornecedores, criado];
        _fornecedorId = inteiro(criado['id']);
        _fornecedorSugerido = null;
      });
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível cadastrar o fornecedor (${e.descricao}).');
    }
  }

  // ─── Confirmação ───────────────────────────────────────

  Future<void> _confirmar() async {
    final valor = lerDecimal(_valor.text);
    final litros = lerDecimal(_litros.text);
    final valorLitro = lerDecimal(_valorLitro.text);
    final km = lerDecimal(_km.text);

    String? erro;
    if (_veiculo == null) {
      erro = 'Selecione um veículo na tela inicial.';
    } else if (valor == null || valor == 0) {
      erro = 'Informe o valor antes de confirmar.';
    } else if (_tipo == 'Abastecimento') {
      erro = _motorista == null
          ? 'Selecione um motorista antes de confirmar o abastecimento.'
          : (km == null || km == 0)
              ? 'Informe o km do odômetro antes de confirmar o abastecimento.'
              : (litros == null || litros == 0)
                  ? 'Informe os litros antes de confirmar o abastecimento.'
                  : (valorLitro == null || valorLitro == 0)
                      ? 'Informe o valor por litro antes de confirmar o abastecimento.'
                      : _combustivel == null
                          ? 'Selecione o combustível antes de confirmar o abastecimento.'
                          : null;
    }
    if (erro != null) {
      mostrarAviso(context, erro);
      return;
    }

    final agora = DateTime.fromMillisecondsSinceEpoch(DateTime.now().millisecondsSinceEpoch, isUtc: true);
    final lancamento = <String, dynamic>{
      'tipo': _tipo,
      'veiculo': {'id': inteiro(_veiculo!['id'])},
      'motorista': _motorista == null ? null : {'id': inteiro(_motorista!['id'])},
      'categoriaDespesa': _categoriaId == null ? null : {'id': _categoriaId},
      'fornecedor': _fornecedorId == null ? null : {'id': _fornecedorId},
      'fornecedorSugerido': _fornecedorId == null ? _fornecedorSugerido : null,
      'data': agora.toIso8601String(),
      'valor': valor,
      'litros': litros,
      'valorLitro': valorLitro,
      'km': km,
      'combustivel': _combustivel,
      'observacao': _observacao.text,
      'numeroDocumento': _numeroDocumento,
      'origemLancamento': _origem,
      'confiancaIA': _confianca,
      'textoBrutoIA': _textoBruto,
    };

    setState(() => _salvando = true);
    try {
      final r = await ref.read(lancamentoServiceProvider).salvar(lancamento, foto: _fotoParaAnexo, prefixoFoto: 'comprovante');
      if (!mounted) return;
      mostrarAviso(
          context,
          r.anexoFalhou
              ? 'Lançamento salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.'
              : 'Lançamento salvo com sucesso!');
      Navigator.pop(context);
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível salvar (${e.descricao}). Verifique sua conexão e tente novamente.');
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  void _descartar() => setState(() {
        _revisando = false;
        _origem = null;
        _fotoParaAnexo = null;
        _avisoDuplicidade = null;
      });

  // ─── Telas ─────────────────────────────────────────────

  Widget _cabecalho() {
    final v = _veiculo;
    final m = _motorista;
    return Cartao(
      child: Column(children: [
        Row(children: [
          const IconeSelo(Icons.local_shipping_outlined),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Veículo', style: TextStyle(color: Colors.black54, fontSize: 12)),
              Text(v == null ? 'Nenhum selecionado' : '${texto(v['descricao'])} ${texto(v['placa'])}',
                  style: const TextStyle(fontWeight: FontWeight.w600)),
            ]),
          ),
        ]),
        if (m != null || _tipo == 'Abastecimento') ...[
          const SizedBox(height: 10),
          Row(children: [
            const IconeSelo(Icons.person_outline, cor: corSucesso),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Motorista', style: TextStyle(color: Colors.black54, fontSize: 12)),
                Text(m == null ? 'Nenhum selecionado (obrigatório)' : texto(m['nome']),
                    style: TextStyle(fontWeight: FontWeight.w600, color: m == null ? corPerigo : null)),
              ]),
            ),
          ]),
        ],
      ]),
    );
  }

  Widget _chat() {
    final ocupado = _enviando || _gravando || _transcrevendo || _ditando;
    return Column(children: [
      Expanded(
        child: _mensagens.isEmpty
            ? const Center(
                child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Text('Anexe uma foto e/ou fale ou digite algo para começar', textAlign: TextAlign.center)))
            : ListView.builder(
                controller: _rolagem,
                padding: const EdgeInsets.all(12),
                itemCount: _mensagens.length,
                itemBuilder: (_, i) {
                  final m = _mensagens[i];
                  return Align(
                    alignment: m.enviada ? Alignment.centerRight : Alignment.centerLeft,
                    child: Container(
                      constraints: BoxConstraints(maxWidth: MediaQuery.sizeOf(context).width * 0.8),
                      margin: const EdgeInsets.symmetric(vertical: 4),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: m.enviada ? const Color(0xFFD9E7FF) : Colors.white,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: Colors.black12),
                      ),
                      child: Row(mainAxisSize: MainAxisSize.min, children: [
                        if (m.icone != null) ...[Icon(m.icone, size: 18), const SizedBox(width: 6)],
                        if (m.status) ...[
                          const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                          const SizedBox(width: 8),
                        ],
                        Flexible(child: Text(m.texto ?? '')),
                        if (m.pendente) ...[
                          const SizedBox(width: 8),
                          InkWell(onTap: _removerFoto, child: const Text('Remover', style: TextStyle(color: corPerigo, fontSize: 12))),
                        ],
                      ]),
                    ),
                  );
                },
              ),
      ),
      Padding(
        padding: const EdgeInsets.fromLTRB(8, 4, 8, 4),
        child: Text(
          _ditando
              ? 'Ouvindo... fale e o texto aparece. Toque no microfone de novo para parar.'
              : _gravando
              ? 'Gravando... toque no microfone de novo para parar.'
              : _transcrevendo
                  ? _msgTranscrevendo
                  : 'Anexe uma foto ou fale/digite um complemento — pelo menos um dos dois é necessário.',
          style: const TextStyle(color: Colors.black54, fontSize: 12),
        ),
      ),
      Padding(
        padding: const EdgeInsets.fromLTRB(8, 0, 8, 8),
        child: Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
          IconButton(
              tooltip: 'Anexar foto', icon: const Icon(Icons.photo_camera_outlined), onPressed: ocupado ? null : _anexarFoto),
          IconButton(
            tooltip: _ditando ? 'Parar ditado' : 'Ditar (texto ao vivo)',
            color: _ditando ? corPerigo : corPrimaria,
            onPressed: (_enviando || _transcrevendo || _gravando) ? null : _alternarDitado,
            icon: Icon(_ditando ? Icons.stop_circle : Icons.mic_none),
          ),
          IconButton(
            tooltip: _gravando ? 'Parar gravação' : 'Gravar áudio para a IA transcrever',
            color: _gravando ? corPerigo : corPrimaria,
            onPressed: (_enviando || _transcrevendo || _ditando) ? null : _alternarGravacao,
            icon: _transcrevendo
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : Icon(_gravando ? Icons.stop_circle : Icons.record_voice_over_outlined),
          ),
          Expanded(
            child: _gravando
                ? const Padding(
                    padding: EdgeInsets.symmetric(vertical: 14),
                    child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                      Icon(Icons.graphic_eq, color: corPerigo),
                      SizedBox(width: 8),
                      Text('Gravando áudio'),
                    ]))
                : TextField(
                    controller: _texto,
                    enabled: !_enviando && !_transcrevendo && !_ditando,
                    minLines: 1,
                    maxLines: 4,
                    decoration: const InputDecoration(hintText: 'Digite ou fale uma mensagem...'),
                  ),
          ),
          IconButton.filled(
            tooltip: 'Enviar para a IA',
            onPressed: ocupado ? null : _enviarParaIa,
            icon: const Icon(Icons.send),
          ),
        ]),
      ),
    ]);
  }

  Widget _revisao() {
    final abastecimento = _tipo == 'Abastecimento';
    return ListView(
      keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        if (_avisoDuplicidade != null)
          Cartao(
            cor: const Color(0xFFFFF4D6),
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Icon(Icons.warning_amber_rounded, color: Color(0xFFE0A800)),
              const SizedBox(width: 8),
              Expanded(child: Text(_avisoDuplicidade!)),
            ]),
          ),
        Cartao(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Row(children: [
              IconeSelo(Icons.auto_awesome, cor: Color(0xFFB45CFF)),
              SizedBox(width: 12),
              Expanded(child: Text('Confira os dados extraídos', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
            ]),
            if (_confianca != null)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text('Confiança da IA: ${(_confianca! * 100).round()}%', style: const TextStyle(color: Colors.black54, fontSize: 12)),
              ),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              initialValue: _tipo,
              decoration: const InputDecoration(labelText: 'Tipo', prefixIcon: Icon(Icons.label_outline)),
              items: const [
                DropdownMenuItem(value: 'Abastecimento', child: Text('Abastecimento')),
                DropdownMenuItem(value: 'Despesa', child: Text('Despesa')),
                DropdownMenuItem(value: 'Serviço', child: Text('Serviço')),
              ],
              onChanged: (v) => setState(() => _tipo = v ?? _tipo),
            ),
            if (!abastecimento) ...[
              const SizedBox(height: 12),
              CampoEntidade(
                rotulo: 'Categoria',
                icone: Icons.category_outlined,
                itens: _categorias,
                selecionadoId: _categoriaId,
                carregando: _carregandoCatalogos,
                aoMudar: (v) => setState(() => _categoriaId = v),
              ),
            ],
            const SizedBox(height: 12),
            CampoEntidade(
              rotulo: abastecimento ? 'Posto / fornecedor' : 'Fornecedor',
              icone: Icons.storefront_outlined,
              itens: _fornecedores,
              selecionadoId: _fornecedorId,
              carregando: _carregandoCatalogos,
              aoMudar: (v) => setState(() {
                _fornecedorId = v;
                if (v != null) _fornecedorSugerido = null;
              }),
              aoCadastrar: _cadastrarFornecedor,
            ),
            if (_fornecedorId == null && _fornecedorSugerido != null)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text('Sugerido pela IA: "$_fornecedorSugerido" (será cadastrado automaticamente)',
                    style: const TextStyle(color: Colors.black54, fontSize: 12)),
              ),
            const SizedBox(height: 12),
            Focus(
              onFocusChange: (f) => f ? null : _recalcular(),
              child: TextField(
                controller: _valor,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'Valor (R\$) *', prefixIcon: Icon(Icons.payments_outlined)),
              ),
            ),
            if (abastecimento) ...[
              const SizedBox(height: 12),
              Row(children: [
                Expanded(
                  child: Focus(
                    onFocusChange: (f) => f ? null : _recalcular(),
                    child: TextField(
                      controller: _litros,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Litros *', prefixIcon: Icon(Icons.science_outlined)),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Focus(
                    onFocusChange: (f) => f ? null : _recalcular(),
                    child: TextField(
                      controller: _valorLitro,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Valor/litro *', prefixIcon: Icon(Icons.sell_outlined)),
                    ),
                  ),
                ),
              ]),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                key: ValueKey(_combustivel),
                initialValue: _combustivel,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Combustível *', prefixIcon: Icon(Icons.local_fire_department_outlined)),
                items: [for (final c in combustiveis) DropdownMenuItem(value: c, child: Text(c))],
                onChanged: (v) => setState(() => _combustivel = v),
              ),
            ],
            const SizedBox(height: 12),
            TextField(
              controller: _km,
              keyboardType: TextInputType.number,
              decoration: InputDecoration(labelText: 'Km do odômetro${abastecimento ? ' *' : ''}', prefixIcon: const Icon(Icons.speed_outlined)),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _observacao,
              maxLines: 2,
              decoration: const InputDecoration(labelText: 'Observação', prefixIcon: Icon(Icons.notes)),
            ),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(_origem == 'IA_Voz' ? 'Ver transcrição' : 'Ver texto lido na foto'),
              trailing: Icon(_mostrarTextoBruto ? Icons.expand_less : Icons.expand_more),
              onTap: () => setState(() => _mostrarTextoBruto = !_mostrarTextoBruto),
            ),
            if (_mostrarTextoBruto) Text(_textoBruto.isEmpty ? 'Nenhum texto retornado.' : _textoBruto, style: const TextStyle(color: Colors.black54)),
          ]),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: FilledButton.icon(
            onPressed: _salvando ? null : _confirmar,
            icon: _salvando
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.check_circle_outline),
            label: Text(_salvando ? 'Salvando...' : 'Confirmar e salvar'),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: TextButton(onPressed: _salvando ? null : _descartar, child: const Text('Descartar e voltar ao chat')),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_salvando && !_gravando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Lançamento por IA')),
        body: Column(children: [
          _cabecalho(),
          Expanded(child: _revisando ? _revisao() : _chat()),
        ]),
      ),
    );
  }
}
