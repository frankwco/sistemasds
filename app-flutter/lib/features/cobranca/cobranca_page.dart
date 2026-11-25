import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/estado_consulta.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../data/cobranca_repo.dart';
import '../../providers.dart';

enum _Resposta { enviada, semWhatsapp, naoEnviada }

/// Cobrança por WhatsApp em campo (mesma lógica da tela web, sem importação de CSV nem e-mail em
/// massa). Só aparece para quem tem `acessoSistema`; o backend também confere a cada chamada.
class CobrancaPage extends ConsumerStatefulWidget {
  const CobrancaPage({super.key});

  @override
  ConsumerState<CobrancaPage> createState() => _CobrancaPageState();
}

class _CobrancaPageState extends ConsumerState<CobrancaPage> {
  late Future<Map<String, dynamic>> _futuro = _buscar();
  Map<String, dynamic>? _resumo;
  List<Map<String, dynamic>> _clientes = [];
  int? _expandido;
  final Set<int> _anterioresVisiveis = {};
  String? _ocupado; // "<cliente>-<telefone>" em confirmação ou envio
  FiltroCobranca _filtro = FiltroCobranca.todos;
  Set<String> _semWhatsapp = {};
  ScrollController _rolagem = ScrollController();
  bool _enviando = false; // abrindo o WhatsApp ou esperando a resposta de "foi enviada?"

  int get _usuarioId => ref.read(sessaoProvider).usuario?.id ?? 0;
  CobrancaRepo get _repo => ref.read(cobrancaRepoProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_retomarPendente());
  }

  @override
  void dispose() {
    _rolagem.dispose();
    super.dispose();
  }

  Future<Map<String, dynamic>> _buscar() async {
    final dados = await ref.read(apiProvider).cobrancas(_usuarioId);
    _semWhatsapp = await _repo.lerSemWhatsapp();
    _resumo = dados['resumo'] is Map ? Map<String, dynamic>.from(dados['resumo'] as Map) : null;
    _clientes = [
      for (final c in (dados['clientes'] as List? ?? const [])) if (c is Map) Map<String, dynamic>.from(c),
    ];
    return dados;
  }

  void _recarregar() => setState(() => _futuro = _buscar());

  /// Tela reaberta depois de o Android encerrar o app com o WhatsApp na frente: volta ao mesmo
  /// ponto da lista e pergunta do envio que ficou sem resposta.
  Future<void> _retomarPendente() async {
    final p = await _repo.lerPendente();
    if (p == null || !mounted) return;
    setState(() {
      _filtro = p.filtro;
      if (!_rolagem.hasClients) {
        _rolagem.dispose();
        _rolagem = ScrollController(initialScrollOffset: p.rolagem);
      }
    });
    try {
      await _futuro;
    } catch (_) {
      return; // sem internet: continua pendente e pergunta na próxima abertura
    }
    if (!mounted || _enviando) return;
    _enviando = true;
    try {
      await _perguntarEnvio(p);
    } finally {
      _enviando = false;
    }
  }

  String _chave(int clienteId, int telefone) => '$clienteId-$telefone';

  void _substituir(Map<String, dynamic>? atualizado) {
    if (atualizado == null || !mounted) return;
    final i = _clientes.indexWhere((c) => c['clienteId'] == atualizado['clienteId']);
    if (i >= 0) setState(() => _clientes[i] = atualizado);
  }

  Future<void> _marcarSemWhatsapp(Map<String, dynamic> c, int numero, bool semWhatsapp) async {
    final chave = CobrancaRepo.chaveTelefone(inteiro(c['clienteId']), texto(c['telefone$numero']));
    final mudou = semWhatsapp ? _semWhatsapp.add(chave) : _semWhatsapp.remove(chave);
    if (!mudou) return;
    if (mounted) setState(() {});
    await _repo.gravarSemWhatsapp(_semWhatsapp);
  }

  /// Chama o servidor; se falhar oferece tentar de novo. Devolve se foi salvo.
  Future<bool> _salvar(String acao, Map<String, dynamic> c, int numero, {bool manterHistorico = false}) async {
    final clienteId = inteiro(c['clienteId']);
    final api = ref.read(apiProvider);
    final usuarioId = _usuarioId;
    while (mounted) {
      setState(() => _ocupado = _chave(clienteId, numero));
      final String erro;
      try {
        _substituir(await api.acaoWhatsapp(acao, usuarioId, clienteId, numero, manterHistorico: manterHistorico));
        return true;
      } on ApiException catch (e) {
        erro = e.descricao;
      } finally {
        if (mounted) setState(() => _ocupado = null);
      }
      if (!mounted) return false;
      final repetir = await confirmar(
        context,
        titulo: 'Não foi possível salvar',
        conteudo: Text('${texto(c['nome'])}: $erro.'),
        confirmar: 'Tentar de novo',
        cancelar: 'Cancelar',
      );
      if (!repetir) return false;
    }
    return false;
  }

  Future<void> _confirmarOuDesmarcar(Map<String, dynamic> c, int numero) async {
    final confirmado = verdadeiro(c['telefone${numero}Whatsapp']);
    final ok = await confirmar(
      context,
      titulo: confirmado ? 'Desfazer confirmação?' : 'Confirmar WhatsApp?',
      conteudo: Text(confirmado
          ? 'Desfazer a confirmação deste número como WhatsApp? O último envio por WhatsApp sai do histórico.'
          : 'Confirmar que este número é WhatsApp e registrar a cobrança como enviada?'),
      confirmar: confirmado ? 'Desfazer' : 'Confirmar',
      cancelar: 'Cancelar',
    );
    if (!ok || !mounted) return;
    if (await _salvar(confirmado ? 'desmarcar' : 'confirmar', c, numero) && !confirmado) {
      await _marcarSemWhatsapp(c, numero, false);
    }
  }

  Future<void> _enviar(Map<String, dynamic> c, int numero) async {
    final telefone = texto(c['telefone$numero']);
    if (telefone.isEmpty || _enviando) return;
    _enviando = true;
    try {
      final pendente = EnvioPendente(
        clienteId: inteiro(c['clienteId']),
        numero: numero,
        instante: DateTime.now().millisecondsSinceEpoch,
        rolagem: _rolagem.hasClients ? _rolagem.offset : 0,
        filtro: _filtro,
      );
      // Gravado antes de abrir: se o Android encerrar o app, a tela reabre neste ponto.
      await _repo.gravarPendente(pendente);
      final aberto = await abrirWhatsapp(telefone, texto(c['mensagemWhatsapp']));
      if (!aberto) {
        await _repo.resolverPendente(pendente);
        if (mounted) mostrarAviso(context, 'Não foi possível abrir o WhatsApp.');
        return;
      }
      // A pergunta fica atrás do WhatsApp, esperando o operador voltar.
      if (mounted) await _perguntarEnvio(pendente);
    } finally {
      _enviando = false;
    }
  }

  /// Só vira "enviado" se o operador confirmar: o WhatsApp não avisa se a mensagem saiu nem se
  /// o número tem conta (quando não tem, ele só oferece enviar convite).
  Future<void> _perguntarEnvio(EnvioPendente p) async {
    final c = _clientes.where((c) => inteiro(c['clienteId']) == p.clienteId).firstOrNull;
    if (c == null) {
      await _repo.resolverPendente(p);
      return;
    }
    Widget botao(ButtonStyleButton b) => SizedBox(width: double.infinity, height: 46, child: b);
    final resposta = await showDialog<_Resposta>(
          context: context,
          barrierDismissible: false,
          builder: (ctx) => AlertDialog(
            title: const Text('A cobrança foi enviada?'),
            content: SingleChildScrollView(
              child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(texto(c['nome']), style: const TextStyle(fontWeight: FontWeight.w600)),
                Text(texto(c['telefone${p.numero}'])),
                const SizedBox(height: 16),
                botao(FilledButton(
                  onPressed: () => Navigator.pop(ctx, _Resposta.enviada),
                  child: const Text('Sim, enviei'),
                )),
                const SizedBox(height: 8),
                botao(OutlinedButton(
                  style: OutlinedButton.styleFrom(foregroundColor: corPerigo),
                  onPressed: () => Navigator.pop(ctx, _Resposta.semWhatsapp),
                  child: const Text('Número não é WhatsApp'),
                )),
                const SizedBox(height: 8),
                botao(TextButton(
                  onPressed: () => Navigator.pop(ctx, _Resposta.naoEnviada),
                  child: const Text('Não enviei'),
                )),
              ]),
            ),
          ),
        ) ??
        _Resposta.naoEnviada;
    if (!mounted || !await _repo.resolverPendente(p) || !mounted) return;

    final confirmado = verdadeiro(c['telefone${p.numero}Whatsapp']);
    switch (resposta) {
      case _Resposta.enviada:
        // Confirmar o número já registra o envio no histórico; não registra duas vezes.
        if (await _salvar(confirmado ? 'registrarEnvio' : 'confirmar', c, p.numero)) {
          await _marcarSemWhatsapp(c, p.numero, false);
        }
      case _Resposta.semWhatsapp:
        if (!confirmado || await _salvar('desmarcar', c, p.numero, manterHistorico: true)) {
          await _marcarSemWhatsapp(c, p.numero, true);
        }
      case _Resposta.naoEnviada:
        break;
    }
  }

  Map<String, dynamic>? _ultimoHistorico(Map<String, dynamic> c) {
    final atual = c['historicoAtual'] as List?;
    if (atual != null && atual.isNotEmpty) return Map<String, dynamic>.from(atual.last as Map);
    final ant = c['historicoAnterior'] as List?;
    if (ant != null && ant.isNotEmpty) return Map<String, dynamic>.from(ant.last as Map);
    return null;
  }

  Widget _linhaHistorico(Object? h) {
    final m = Map<String, dynamic>.from(h as Map);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Icon(historicoDeWhatsapp(m) ? Icons.chat : Icons.mail_outline, size: 16, color: Colors.black45),
        const SizedBox(width: 6),
        Expanded(child: Text(texto(m['texto']), style: const TextStyle(fontSize: 12))),
      ]),
    );
  }

  Widget _telefone(Map<String, dynamic> c, int numero) {
    final tel = texto(c['telefone$numero']);
    if (tel.isEmpty) return const SizedBox.shrink();
    final chave = _chave(inteiro(c['clienteId']), numero);
    final confirmado = verdadeiro(c['telefone${numero}Whatsapp']);
    final ocupado = _ocupado == chave;
    final semWhatsapp = !confirmado && _semWhatsapp.contains(CobrancaRepo.chaveTelefone(inteiro(c['clienteId']), tel));
    return Row(children: [
      Expanded(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(tel, style: const TextStyle(fontSize: 15)),
          if (semWhatsapp) const Text('Não é WhatsApp', style: TextStyle(fontSize: 12, color: corPerigo)),
        ]),
      ),
      if (ocupado)
        const Padding(padding: EdgeInsets.all(12), child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
      else ...[
        IconButton(tooltip: 'Enviar WhatsApp', color: corSucesso, icon: const Icon(Icons.chat), onPressed: () => _enviar(c, numero)),
        IconButton(
          tooltip: confirmado ? 'WhatsApp confirmado' : 'Confirmar WhatsApp',
          color: confirmado ? corSucesso : (semWhatsapp ? corPerigo : Colors.black45),
          icon: Icon(confirmado ? Icons.check_circle : (semWhatsapp ? Icons.cancel_outlined : Icons.help_outline)),
          onPressed: () => _confirmarOuDesmarcar(c, numero),
        ),
      ],
    ]);
  }

  Widget _situacao(String? enviadoEm) {
    final cor = enviadoEm != null ? const Color(0xFF1B8A47) : const Color(0xFFB08400);
    return Row(children: [
      Icon(enviadoEm != null ? Icons.check_circle : Icons.schedule_send_outlined, size: 16, color: cor),
      const SizedBox(width: 6),
      Expanded(
        child: Text(enviadoEm != null ? 'WhatsApp enviado em $enviadoEm' : 'WhatsApp não enviado',
            style: TextStyle(color: cor, fontSize: 12, fontWeight: FontWeight.w600)),
      ),
    ]);
  }

  Widget _filtros(int enviados) {
    Widget chip(FiltroCobranca f, String rotulo, int total) => ChoiceChip(
          label: Text('$rotulo ($total)'),
          selected: _filtro == f,
          onSelected: (_) => setState(() => _filtro = f),
        );
    return Wrap(spacing: 8, children: [
      chip(FiltroCobranca.todos, 'Todos', _clientes.length),
      chip(FiltroCobranca.naoEnviados, 'Não enviados', _clientes.length - enviados),
      chip(FiltroCobranca.enviados, 'Enviados', enviados),
    ]);
  }

  Widget _cabecalho(int enviados, bool vazio) {
    final r = _resumo;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (r != null) ...[
          if (texto(r['dataArquivoFormatada']).isNotEmpty)
            Text('Relatório de ${texto(r['dataArquivoFormatada'])}', style: const TextStyle(color: Colors.black54)),
          const SizedBox(height: 6),
          Wrap(spacing: 8, children: [
            Chip(label: Text('${texto(r['totalClientesEmAberto'])} clientes')),
            Chip(
              backgroundColor: const Color(0xFFFFE9E9),
              label: Text('${texto(r['valorTotalEmAbertoFormatado'])} em aberto'),
            ),
          ]),
        ],
        _filtros(enviados),
        if (vazio)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Center(child: Text('Nenhum cliente neste filtro.')),
          ),
      ]),
    );
  }

  List<Map<String, dynamic>> _visiveis() => switch (_filtro) {
        FiltroCobranca.todos => _clientes,
        FiltroCobranca.naoEnviados => [for (final c in _clientes) if (whatsappEnviadoEm(c) == null) c],
        FiltroCobranca.enviados => [for (final c in _clientes) if (whatsappEnviadoEm(c) != null) c],
      };

  @override
  Widget build(BuildContext context) {
    final logado = ref.watch(sessaoProvider.select((s) => s.usuario != null));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Cobrança'),
        actions: [IconButton(tooltip: 'Atualizar', icon: const Icon(Icons.refresh), onPressed: _recarregar)],
      ),
      body: !logado
          ? const Center(child: Text('Saia e faça login novamente para ver a Cobrança.'))
          : RefreshIndicator(
              onRefresh: () async {
                _recarregar();
                await _futuro.catchError((_) => <String, dynamic>{});
              },
              child: CorpoConsulta<Map<String, dynamic>>(
                futuro: _futuro,
                aoTentarDeNovo: _recarregar,
                vazio: (_) => _clientes.isEmpty,
                textoVazio: 'Nenhum cliente com título em atraso no momento.',
                construir: (context, _) {
                  // Aqui dentro (e não no build): quando a busca termina só este trecho é refeito.
                  final visiveis = _visiveis();
                  final enviados = _clientes.where((c) => whatsappEnviadoEm(c) != null).length;
                  return ListView.builder(
                    controller: _rolagem,
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.only(bottom: 24),
                    itemCount: visiveis.length + 1,
                    itemBuilder: (_, i) {
                      if (i == 0) return _cabecalho(enviados, visiveis.isEmpty);
                      final c = visiveis[i - 1];
                      final id = inteiro(c['clienteId']);
                      final ultimo = _ultimoHistorico(c);
                      final aberto = _expandido == id;
                      return Cartao(
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          InkWell(
                            onTap: () => setState(() => _expandido = aberto ? null : id),
                            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Expanded(
                                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                  Text(texto(c['nome']), style: const TextStyle(fontWeight: FontWeight.w600)),
                                  if (texto(c['cidade']).isNotEmpty)
                                    Text(texto(c['cidade']), style: const TextStyle(color: Colors.black54, fontSize: 12)),
                                ]),
                              ),
                              Text(texto(c['valorTotalFormatado']), style: const TextStyle(fontWeight: FontWeight.bold, color: corPerigo)),
                            ]),
                          ),
                          if (verdadeiro(c['temNomeDivergente']))
                            Padding(
                              padding: const EdgeInsets.only(top: 6),
                              child: Text('Nome no relatório do banco diverge: ${texto(c['nomesSacadoRelatorio'])}',
                                  style: const TextStyle(color: Color(0xFFB08400), fontSize: 12)),
                            ),
                          const SizedBox(height: 6),
                          Text(
                            ultimo != null ? 'Última notificação: ${texto(ultimo['texto'])}' : 'Nenhuma notificação enviada ainda',
                            style: const TextStyle(color: Colors.black54, fontSize: 12),
                          ),
                          const SizedBox(height: 2),
                          _situacao(whatsappEnviadoEm(c)),
                          _telefone(c, 1),
                          _telefone(c, 2),
                          TextButton(
                            onPressed: () => setState(() => _expandido = aberto ? null : id),
                            child: Text(aberto ? 'Ver menos' : 'Ver parcelas e histórico'),
                          ),
                          if (aberto) ...[
                            const Text('Parcelas em aberto', style: TextStyle(fontWeight: FontWeight.bold)),
                            for (final p in (c['parcelasFormatadas'] as List? ?? const []))
                              Padding(padding: const EdgeInsets.symmetric(vertical: 1), child: Text('$p', style: const TextStyle(fontSize: 12))),
                            const SizedBox(height: 8),
                            const Text('Histórico (rodada atual)', style: TextStyle(fontWeight: FontWeight.bold)),
                            if ((c['historicoAtual'] as List? ?? const []).isEmpty)
                              const Text('Nenhuma notificação registrada ainda nesta rodada.', style: TextStyle(fontSize: 12, color: Colors.black54))
                            else
                              for (final h in (c['historicoAtual'] as List)) _linhaHistorico(h),
                            if ((c['historicoAnterior'] as List? ?? const []).isNotEmpty)
                              TextButton(
                                onPressed: () => setState(() => _anterioresVisiveis.contains(id) ? _anterioresVisiveis.remove(id) : _anterioresVisiveis.add(id)),
                                child: Text(_anterioresVisiveis.contains(id) ? 'Ocultar anteriores' : 'Ver notificações anteriores'),
                              ),
                            if (_anterioresVisiveis.contains(id))
                              for (final h in (c['historicoAnterior'] as List)) _linhaHistorico(h),
                          ],
                        ]),
                      );
                    },
                  );
                },
              ),
            ),
    );
  }
}
