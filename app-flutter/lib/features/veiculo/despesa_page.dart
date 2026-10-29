import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/conversao.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../providers.dart';
import 'componentes.dart';

/// Despesa ou manutenção/serviço de um veículo. Direto no servidor (exige internet).
class DespesaPage extends ConsumerStatefulWidget {
  const DespesaPage({super.key});

  @override
  ConsumerState<DespesaPage> createState() => _DespesaPageState();
}

class _DespesaPageState extends ConsumerState<DespesaPage> {
  final _valor = TextEditingController();
  final _observacao = TextEditingController();
  final _kmRevisao = TextEditingController();

  DateTime _data = DateTime.now();
  DateTime? _entrada;
  DateTime? _saida;
  bool _manutencao = false;
  String? _tipoManutencao;
  int? _categoriaId;
  int? _fornecedorId;
  List<Map<String, dynamic>> _categorias = [];
  List<Map<String, dynamic>> _fornecedores = [];
  Uint8List? _foto;
  bool _carregando = true;
  bool _salvando = false;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void dispose() {
    for (final c in [_valor, _observacao, _kmRevisao]) {
      c.dispose();
    }
    super.dispose();
  }

  List<Map<String, dynamic>> _lista(List<dynamic> l) =>
      [for (final i in l) if (i is Map) Map<String, dynamic>.from(i)];

  /// Categorias e fornecedores direto do servidor a cada abertura.
  Future<void> _carregar() async {
    setState(() => _carregando = true);
    try {
      final api = ref.read(apiProvider);
      final r = await Future.wait([api.categoriasDespesa(), api.fornecedores()]);
      if (!mounted) return;
      setState(() {
        _categorias = _lista(r[0]);
        _fornecedores = _lista(r[1]);
      });
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível carregar categorias/fornecedores (${e.descricao}).');
    } finally {
      if (mounted) setState(() => _carregando = false);
    }
  }

  Future<void> _cadastrarFornecedor() async {
    final nome = await pedirNome(context, 'Novo fornecedor', 'Nome do fornecedor');
    if (nome == null || !mounted) return;
    try {
      final criado = await ref.read(apiProvider).criarFornecedor(nome);
      if (!mounted) return;
      setState(() {
        _fornecedores = [..._fornecedores, criado];
        _fornecedorId = inteiro(criado['id']);
      });
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível cadastrar o fornecedor (${e.descricao}).');
    }
  }

  Future<void> _salvar() async {
    final s = ref.read(sessaoProvider);
    final valor = lerDecimal(_valor.text);
    if (s.veiculo == null) {
      mostrarAviso(context, 'Selecione um veículo na tela inicial antes de registrar.');
      return;
    }
    if (valor == null || valor == 0) {
      mostrarAviso(context, 'Informe o valor da despesa.');
      return;
    }

    final lancamento = <String, dynamic>{
      'tipo': _manutencao ? 'Serviço' : 'Despesa',
      'veiculo': {'id': s.veiculo!.id},
      'motorista': s.motorista == null ? null : {'id': s.motorista!.id},
      'categoriaDespesa': _categoriaId == null ? null : {'id': _categoriaId},
      'fornecedor': _fornecedorId == null ? null : {'id': _fornecedorId},
      'data': dataParaApi(_data),
      'valor': valor,
      'observacao': _observacao.text,
    };
    if (_manutencao) {
      lancamento['tipoManutencao'] = _tipoManutencao;
      lancamento['dataEntrada'] = _entrada == null ? null : dataParaApi(_entrada!);
      lancamento['dataSaida'] = _saida == null ? null : dataParaApi(_saida!);
      lancamento['kmProximaRevisao'] = lerDecimal(_kmRevisao.text);
    }

    setState(() => _salvando = true);
    try {
      final r = await ref.read(lancamentoServiceProvider).salvar(lancamento, foto: _foto, prefixoFoto: 'despesa');
      if (!mounted) return;
      mostrarAviso(
          context,
          r.anexoFalhou
              ? 'Registro salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.'
              : 'Registrado com sucesso!');
      Navigator.pop(context);
    } on ApiException catch (e) {
      if (mounted) {
        mostrarAviso(context, 'Não foi possível salvar (${e.descricao}). Verifique sua conexão e tente novamente.');
      }
    } finally {
      if (mounted) setState(() => _salvando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_salvando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Despesa / Manutenção')),
        body: ListView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.only(bottom: 24),
          children: [
            const CartaoVeiculoMotorista(),
            Cartao(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Row(children: [
                  IconeSelo(Icons.construction_outlined, cor: Color(0xFFFF8A00)),
                  SizedBox(width: 12),
                  Text('Dados da despesa', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 14),
                CampoEntidade(
                  rotulo: 'Categoria',
                  icone: Icons.category_outlined,
                  itens: _categorias,
                  selecionadoId: _categoriaId,
                  carregando: _carregando,
                  aoMudar: (v) => setState(() => _categoriaId = v),
                ),
                const SizedBox(height: 12),
                CampoEntidade(
                  rotulo: 'Fornecedor',
                  icone: Icons.storefront_outlined,
                  itens: _fornecedores,
                  selecionadoId: _fornecedorId,
                  carregando: _carregando,
                  aoMudar: (v) => setState(() => _fornecedorId = v),
                  aoCadastrar: _cadastrarFornecedor,
                ),
                const SizedBox(height: 12),
                CampoData(rotulo: 'Data', valor: _data, aoMudar: (d) => setState(() => _data = d ?? _data)),
                const SizedBox(height: 12),
                TextField(
                  controller: _valor,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'Valor (R\$) *', prefixIcon: Icon(Icons.payments_outlined)),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _observacao,
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Observação (opcional)', prefixIcon: Icon(Icons.notes)),
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('É manutenção/serviço?'),
                  value: _manutencao,
                  onChanged: (v) => setState(() => _manutencao = v),
                ),
                if (_manutencao) ...[
                  DropdownButtonFormField<String>(
                    initialValue: _tipoManutencao,
                    decoration: const InputDecoration(labelText: 'Tipo de manutenção', prefixIcon: Icon(Icons.build_outlined)),
                    items: const [
                      DropdownMenuItem(value: 'Preventiva', child: Text('Preventiva')),
                      DropdownMenuItem(value: 'Corretiva', child: Text('Corretiva')),
                    ],
                    onChanged: (v) => setState(() => _tipoManutencao = v),
                  ),
                  const SizedBox(height: 12),
                  CampoData(
                      rotulo: 'Data de entrada', valor: _entrada, limpavel: true, aoMudar: (d) => setState(() => _entrada = d)),
                  const SizedBox(height: 12),
                  CampoData(rotulo: 'Data de saída', valor: _saida, limpavel: true, aoMudar: (d) => setState(() => _saida = d)),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _kmRevisao,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(labelText: 'Km da próxima revisão', prefixIcon: Icon(Icons.speed_outlined)),
                  ),
                ],
              ]),
            ),
            FotoComprovante(bytes: _foto, aoMudar: (b) => setState(() => _foto = b)),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: FilledButton.icon(
                onPressed: _salvando ? null : _salvar,
                icon: _salvando
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.save_outlined),
                label: Text(_salvando ? 'Salvando...' : 'Salvar'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
