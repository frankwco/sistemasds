import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/estado_consulta.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../providers.dart';

/// Lançamentos (abastecimento, despesa, serviço) de quem fez login, com exclusão de um feito errado.
/// Filtra pelo usuário LOGADO, não pelo motorista escolhido na tela inicial (essa escolha é local e
/// trocável por qualquer um, e deixaria ver e excluir lançamentos alheios).
class MeusLancamentosPage extends ConsumerStatefulWidget {
  const MeusLancamentosPage({super.key});

  @override
  ConsumerState<MeusLancamentosPage> createState() => _MeusLancamentosPageState();
}

class _MeusLancamentosPageState extends ConsumerState<MeusLancamentosPage> {
  late Future<List<dynamic>> _futuro = _buscar();
  List<Map<String, dynamic>>? _itens;
  int? _expandido;
  int? _excluindo;

  Future<List<dynamic>> _buscar() async {
    final id = ref.read(sessaoProvider).usuario?.id ?? 0;
    final l = await ref.read(apiProvider).lancamentosMotorista(id);
    _itens = [for (final i in l) if (i is Map) Map<String, dynamic>.from(i)];
    return _itens!;
  }

  void _recarregar() => setState(() {
        _itens = null;
        _futuro = _buscar();
      });

  DateTime? _dt(Object? v) {
    if (v is num) return DateTime.fromMillisecondsSinceEpoch(v.toInt());
    if (v is String && v.isNotEmpty) return DateTime.tryParse(v)?.toLocal();
    return null;
  }

  String _data(Object? v, [String padrao = '-']) {
    final d = _dt(v);
    return d == null ? padrao : DateFormat('dd/MM/yyyy').format(d);
  }

  String _dinheiro(Object? v, [int casas = 2]) {
    final n = numero(v);
    return n == null ? '' : NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$', decimalDigits: casas).format(n);
  }

  IconData _icone(String? tipo) => switch (tipo) {
        'Abastecimento' => Icons.water_drop_outlined,
        'Serviço' => Icons.construction_outlined,
        _ => Icons.sell_outlined,
      };

  Future<void> _confirmarExclusao(Map<String, dynamic> l) async {
    final valor = numero(l['valor']);
    final ok = await confirmar(
      context,
      titulo: 'Excluir lançamento?',
      conteudo: Text('${texto(l['tipo'])} de ${_data(l['data'], 'data desconhecida')}'
          '${valor != null ? ' — ${_dinheiro(valor)}' : ''}. Essa ação não pode ser desfeita.'),
      confirmar: 'Excluir',
      cancelar: 'Cancelar',
      destrutivo: true,
    );
    if (!ok || !mounted) return;
    final id = inteiro(l['id']);
    setState(() => _excluindo = id);
    try {
      await ref.read(apiProvider).excluirLancamento(id, ref.read(sessaoProvider).usuario?.id ?? 0);
      if (!mounted) return;
      setState(() => _itens?.removeWhere((i) => inteiro(i['id']) == id));
      mostrarAviso(context, 'Lançamento excluído.');
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível excluir (${e.descricao}). Tente novamente.');
    } finally {
      if (mounted) setState(() => _excluindo = null);
    }
  }

  Widget _linha(String rotulo, String valor) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 2),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SizedBox(width: 130, child: Text(rotulo, style: const TextStyle(color: Colors.black54, fontSize: 12))),
          Expanded(child: Text(valor, style: const TextStyle(fontSize: 13))),
        ]),
      );

  List<Widget> _detalhe(Map<String, dynamic> l) {
    String? sub(String chave, String campo) => l[chave] is Map ? texto((l[chave] as Map)[campo]) : null;
    final linhas = <(String, String?)>[
      ('Motorista', sub('motorista', 'nome')),
      ('Categoria', sub('categoriaDespesa', 'nome')),
      ('Fornecedor', sub('fornecedor', 'nome')),
      if (texto(sub('fornecedor', 'nome')).isEmpty) ('Fornecedor sugerido', texto(l['fornecedorSugerido'])),
      ('Km do odômetro', l['km'] == null ? null : texto(l['km'])),
      ('Litros', l['litros'] == null ? null : texto(l['litros'])),
      ('Valor/litro', l['valorLitro'] == null ? null : _dinheiro(l['valorLitro'], 3)),
      ('Combustível', texto(l['combustivel'])),
      ('Tipo de manutenção', texto(l['tipoManutencao'])),
      ('Entrada', l['dataEntrada'] == null ? null : _data(l['dataEntrada'])),
      ('Saída', l['dataSaida'] == null ? null : _data(l['dataSaida'])),
      ('Km da próxima revisão', l['kmProximaRevisao'] == null ? null : texto(l['kmProximaRevisao'])),
      ('Nº documento', texto(l['numeroDocumento'])),
      ('Origem', texto(l['origemLancamento'])),
      ('Confiança da IA', l['confiancaIA'] == null ? null : '${((numero(l['confiancaIA']) ?? 0) * 100).round()}%'),
      ('Observação', texto(l['observacao'])),
      ('Texto bruto da IA', texto(l['textoBrutoIA'])),
      ('Lançado em', _dt(l['criadoEm']) == null ? null : DateFormat('dd/MM/yyyy HH:mm').format(_dt(l['criadoEm'])!)),
    ];
    return [for (final (r, v) in linhas) if (v != null && v.isNotEmpty) _linha(r, v)];
  }

  @override
  Widget build(BuildContext context) {
    final logado = ref.watch(sessaoProvider.select((s) => s.usuario != null));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Meus lançamentos'),
        actions: [IconButton(tooltip: 'Atualizar', icon: const Icon(Icons.refresh), onPressed: _recarregar)],
      ),
      body: !logado
          ? const Center(child: Text('Saia e faça login novamente para ver seus lançamentos.'))
          : CorpoConsulta<List<dynamic>>(
              futuro: _futuro,
              aoTentarDeNovo: _recarregar,
              vazio: (_) => (_itens ?? const []).isEmpty,
              textoVazio: 'Nenhum lançamento encontrado.',
              construir: (context, _) {
                final itens = _itens ?? const <Map<String, dynamic>>[];
                return ListView.builder(
                  padding: const EdgeInsets.only(bottom: 24),
                  itemCount: itens.length,
                  itemBuilder: (_, i) {
                    final l = itens[i];
                    final id = inteiro(l['id']);
                    final veiculo = l['veiculo'] is Map ? '${texto((l['veiculo'] as Map)['descricao'])} ${texto((l['veiculo'] as Map)['placa'])}' : '';
                    return Cartao(
                      padding: EdgeInsets.zero,
                      onTap: () => setState(() => _expandido = _expandido == id ? null : id),
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Row(children: [
                            IconeSelo(_icone(texto(l['tipo']))),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text(texto(l['tipo']), style: const TextStyle(fontWeight: FontWeight.w600)),
                                if (veiculo.trim().isNotEmpty) Text(veiculo.trim(), style: const TextStyle(color: Colors.black54, fontSize: 12)),
                                Text(_data(l['data']), style: const TextStyle(color: Colors.black54, fontSize: 12)),
                              ]),
                            ),
                            if (l['valor'] != null) Text(_dinheiro(l['valor']), style: const TextStyle(fontWeight: FontWeight.bold)),
                            if (_excluindo == id)
                              const Padding(
                                  padding: EdgeInsets.all(12),
                                  child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
                            else
                              IconButton(
                                tooltip: 'Excluir',
                                color: corPerigo,
                                icon: const Icon(Icons.delete_outline),
                                onPressed: () => _confirmarExclusao(l),
                              ),
                          ]),
                          if (_expandido == id) ...[const Divider(), ..._detalhe(l)],
                        ]),
                      ),
                    );
                  },
                );
              },
            ),
    );
  }
}
