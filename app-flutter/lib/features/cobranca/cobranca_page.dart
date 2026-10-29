import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/estado_consulta.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../providers.dart';

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

  int get _usuarioId => ref.read(sessaoProvider).usuario?.id ?? 0;

  Future<Map<String, dynamic>> _buscar() async {
    final dados = await ref.read(apiProvider).cobrancas(_usuarioId);
    _resumo = dados['resumo'] is Map ? Map<String, dynamic>.from(dados['resumo'] as Map) : null;
    _clientes = [
      for (final c in (dados['clientes'] as List? ?? const [])) if (c is Map) Map<String, dynamic>.from(c),
    ];
    return dados;
  }

  void _recarregar() => setState(() => _futuro = _buscar());

  String _chave(int clienteId, int telefone) => '$clienteId-$telefone';

  void _substituir(Map<String, dynamic>? atualizado) {
    if (atualizado == null) return;
    final i = _clientes.indexWhere((c) => c['clienteId'] == atualizado['clienteId']);
    if (i >= 0) setState(() => _clientes[i] = atualizado);
  }

  Future<bool> _alterar(Map<String, dynamic> c, int numero, bool confirmarNumero) async {
    final chave = _chave(inteiro(c['clienteId']), numero);
    setState(() => _ocupado = chave);
    try {
      _substituir(await ref
          .read(apiProvider)
          .acaoWhatsapp(confirmarNumero ? 'confirmar' : 'desmarcar', _usuarioId, inteiro(c['clienteId']), numero));
      return true;
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível salvar (${e.descricao}). Tente novamente.');
      return false;
    } finally {
      if (mounted) setState(() => _ocupado = null);
    }
  }

  Future<void> _confirmarOuDesmarcar(Map<String, dynamic> c, int numero) async {
    final confirmado = verdadeiro(c['telefone${numero}Whatsapp']);
    final ok = await confirmar(
      context,
      titulo: confirmado ? 'Desfazer confirmação?' : 'Confirmar WhatsApp?',
      conteudo: Text(confirmado ? 'Desfazer a confirmação deste número como WhatsApp?' : 'Confirmar que este número é WhatsApp?'),
      confirmar: confirmado ? 'Desfazer' : 'Confirmar',
      cancelar: 'Cancelar',
    );
    if (ok && mounted) await _alterar(c, numero, !confirmado);
  }

  Future<void> _enviar(Map<String, dynamic> c, int numero) async {
    final telefone = texto(c['telefone$numero']);
    if (telefone.isEmpty) return;
    if (!verdadeiro(c['telefone${numero}Whatsapp'])) {
      final ok = await confirmar(
        context,
        titulo: 'Número ainda não confirmado',
        conteudo: const Text('Esse número ainda não foi confirmado como WhatsApp. Confirme antes de enviar, caso seja WhatsApp mesmo.'),
        confirmar: 'Confirmar e enviar',
        cancelar: 'Cancelar',
      );
      if (!ok || !mounted || !await _alterar(c, numero, true)) return;
    }
    final aberto = await abrirWhatsapp(telefone, texto(c['mensagemWhatsapp']));
    if (!mounted) return;
    if (!aberto) {
      mostrarAviso(context, 'Não foi possível abrir o WhatsApp.');
      return;
    }
    // WhatsApp aberto: registra o envio no histórico do cliente.
    try {
      _substituir(await ref.read(apiProvider).acaoWhatsapp('registrarEnvio', _usuarioId, inteiro(c['clienteId']), numero));
    } on ApiException catch (e) {
      debugPrint('[Cobrança] envio não registrado: ${e.descricao}');
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
        Icon(verdadeiro(m['whatsapp']) ? Icons.chat : Icons.mail_outline, size: 16, color: Colors.black45),
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
    return Row(children: [
      Expanded(child: Text(tel, style: const TextStyle(fontSize: 15))),
      if (ocupado)
        const Padding(padding: EdgeInsets.all(12), child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
      else ...[
        IconButton(tooltip: 'Enviar WhatsApp', color: corSucesso, icon: const Icon(Icons.chat), onPressed: () => _enviar(c, numero)),
        IconButton(
          tooltip: confirmado ? 'WhatsApp confirmado' : 'Confirmar WhatsApp',
          color: confirmado ? corSucesso : Colors.black45,
          icon: Icon(confirmado ? Icons.check_circle : Icons.help_outline),
          onPressed: () => _confirmarOuDesmarcar(c, numero),
        ),
      ],
    ]);
  }

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
                construir: (context, _) => ListView.builder(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.only(bottom: 24),
                  itemCount: _clientes.length + 1,
                  itemBuilder: (_, i) {
                    if (i == 0) {
                      final r = _resumo;
                      if (r == null) return const SizedBox.shrink();
                      return Padding(
                        padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
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
                        ]),
                      );
                    }
                    final c = _clientes[i - 1];
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
                ),
              ),
            ),
    );
  }
}
