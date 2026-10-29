import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../data/models.dart';
import '../../providers.dart';
import '../coleta/coletas_do_ponto_page.dart';
import '../coleta/pesquisar_pontos_page.dart';
import '../coleta/telefones_page.dart';
import '../home/selecao_page.dart';

/// Consulta de um ponto: endereço, e-mail, próxima e última coleta, periodicidade,
/// coletas anteriores e telefones.
class DadosPontoPage extends ConsumerStatefulWidget {
  const DadosPontoPage({super.key});

  @override
  ConsumerState<DadosPontoPage> createState() => _DadosPontoPageState();
}

class _DadosPontoPageState extends ConsumerState<DadosPontoPage> {
  Ponto? _ponto;
  Map<String, dynamic>? _proxima;
  String? _erro;
  bool _buscando = false;

  Future<void> _escolherCidade() async {
    final cidades = await ref.read(catalogoRepoProvider).listarCidades();
    if (!mounted) return;
    final c = await Navigator.push<Cidade>(
      context,
      MaterialPageRoute(
        builder: (_) => SelecaoPage<Cidade>(titulo: 'Cidade', itens: cidades, rotulo: (c) => c.nome, buscavel: true),
      ),
    );
    if (c == null) return;
    await ref.read(sessaoProvider.notifier).escolherCidade(c);
    setState(() {
      _ponto = null;
      _proxima = null;
    });
  }

  Future<void> _escolherPonto() async {
    final cidade = ref.read(sessaoProvider).cidade;
    if (cidade == null) return;
    final p = await Navigator.push<Ponto>(
      context,
      MaterialPageRoute(builder: (_) => PesquisarPontosPage(cidade: cidade, consulta: true)),
    );
    if (p == null) return;
    setState(() {
      _ponto = p;
      _proxima = null;
      _erro = null;
    });
    await _buscarProxima(p);
  }

  Future<void> _buscarProxima(Ponto p) async {
    setState(() => _buscando = true);
    try {
      final r = await ref.read(apiProvider).proximaColeta(p.id);
      if (mounted && _ponto?.id == p.id) setState(() => _proxima = r);
    } on ApiException catch (e) {
      if (mounted) setState(() => _erro = e.descricao);
    } finally {
      if (mounted) setState(() => _buscando = false);
    }
  }

  String _data(Object? v) {
    DateTime? d;
    if (v is num) d = DateTime.fromMillisecondsSinceEpoch(v.toInt());
    if (v is String && v.isNotEmpty) d = DateTime.tryParse(v);
    return d == null ? '-' : formatarData(d);
  }

  Widget _linhaData(IconData icone, String rotulo, String valor, {bool destaque = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(children: [
          Icon(icone, size: 20, color: destaque ? corPrimaria : Colors.black45),
          const SizedBox(width: 8),
          Text(rotulo),
          const Spacer(),
          Text(valor, style: TextStyle(fontWeight: destaque ? FontWeight.bold : FontWeight.w500)),
        ]),
      );

  @override
  Widget build(BuildContext context) {
    final cidade = ref.watch(sessaoProvider.select((s) => s.cidade));
    final ponto = _ponto;
    final pontoServidor = _proxima?['pontoColeta'] is Map ? Map<String, dynamic>.from(_proxima!['pontoColeta'] as Map) : null;

    return Scaffold(
      appBar: AppBar(title: const Text('Informações de ponto')),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          Cartao(
            onTap: _escolherCidade,
            child: Row(children: [
              const IconeSelo(Icons.location_on_outlined, cor: Color(0xFFFF8A00)),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  const Text('Cidade', style: TextStyle(color: Colors.black54, fontSize: 12)),
                  Text(cidade?.nome ?? 'Selecione', style: const TextStyle(fontWeight: FontWeight.w600)),
                ]),
              ),
              const Icon(Icons.chevron_right),
            ]),
          ),
          if (cidade != null)
            Cartao(
              onTap: _escolherPonto,
              child: Row(children: [
                const IconeSelo(Icons.business_outlined),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('Ponto de coleta', style: TextStyle(color: Colors.black54, fontSize: 12)),
                    Text(ponto?.nomeRazaoSocial ?? 'Selecione', style: const TextStyle(fontWeight: FontWeight.w600)),
                    if (ponto != null && ponto.nomeFantasia.isNotEmpty)
                      Text(ponto.apelido.isEmpty ? ponto.nomeFantasia : '${ponto.nomeFantasia} - ${ponto.apelido}'),
                    if (ponto != null && ponto.enderecoCompleto.isNotEmpty)
                      Text(ponto.enderecoCompleto, style: const TextStyle(color: Colors.black54, fontSize: 12)),
                    if (ponto != null && ponto.email.isNotEmpty)
                      Text(ponto.email, style: const TextStyle(color: Colors.black54, fontSize: 12)),
                  ]),
                ),
                const Icon(Icons.chevron_right),
              ]),
            ),
          if (ponto != null) ...[
            Cartao(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Row(children: [
                  IconeSelo(Icons.calendar_month_outlined, cor: corSucesso),
                  SizedBox(width: 12),
                  Text('Datas de coletas', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 8),
                if (_buscando)
                  const Padding(padding: EdgeInsets.all(12), child: Center(child: CircularProgressIndicator()))
                else if (_erro != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Column(children: [
                      Text('Não foi possível buscar ($_erro).', style: const TextStyle(color: corPerigo)),
                      TextButton(onPressed: () => _buscarProxima(ponto), child: const Text('Tentar de novo')),
                    ]),
                  )
                else ...[
                  _linhaData(Icons.arrow_forward, 'Próxima', _data(_proxima?['dataProximaColeta']), destaque: true),
                  _linhaData(Icons.arrow_back, 'Anterior', _data(pontoServidor?['dataUltimaColeta'])),
                  _linhaData(Icons.repeat, 'Periodicidade',
                      texto(pontoServidor?['periodicidadeColeta']).isEmpty ? ponto.periodicidadeColeta : texto(pontoServidor?['periodicidadeColeta'])),
                  if (texto(_proxima?['mensagem']).isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(texto(_proxima?['mensagem']), style: const TextStyle(color: Colors.black54)),
                    ),
                ],
              ]),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Row(children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.shopping_bag_outlined),
                    label: const Text('Coletas'),
                    onPressed: () => Navigator.push(
                        context, MaterialPageRoute(builder: (_) => ColetasDoPontoPage(ponto: ponto))),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.call_outlined),
                    label: const Text('Fones'),
                    onPressed: () =>
                        Navigator.push(context, MaterialPageRoute(builder: (_) => TelefonesPage(ponto: ponto))),
                  ),
                ),
              ]),
            ),
          ],
        ],
      ),
    );
  }
}
