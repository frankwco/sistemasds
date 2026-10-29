import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../providers.dart';
import 'componentes.dart';

const _combustiveis = ['Diesel S10', 'Diesel S500', 'Gasolina', 'Etanol', 'GNV', 'Arla 32', 'Outro'];

/// Registro de abastecimento. Direto no servidor (exige internet), com foto do comprovante opcional.
class AbastecimentoPage extends ConsumerStatefulWidget {
  const AbastecimentoPage({super.key});

  @override
  ConsumerState<AbastecimentoPage> createState() => _AbastecimentoPageState();
}

class _AbastecimentoPageState extends ConsumerState<AbastecimentoPage> {
  final _km = TextEditingController();
  final _litros = TextEditingController();
  final _valorLitro = TextEditingController();
  final _observacao = TextEditingController();

  DateTime _data = DateTime.now();
  String? _combustivel;
  int? _postoId;
  List<Map<String, dynamic>> _postos = [];
  Uint8List? _foto;
  bool _carregandoPostos = true;
  bool _salvando = false;

  @override
  void initState() {
    super.initState();
    _carregarPostos();
  }

  @override
  void dispose() {
    for (final c in [_km, _litros, _valorLitro, _observacao]) {
      c.dispose();
    }
    super.dispose();
  }

  /// Postos direto do servidor a cada abertura (o cadastro feito no admin aparece na hora).
  Future<void> _carregarPostos() async {
    setState(() => _carregandoPostos = true);
    try {
      final todos = await ref.read(apiProvider).fornecedores();
      if (!mounted) return;
      setState(() => _postos = [
            for (final f in todos)
              if (f is Map && f['tipo'] == 'Posto') Map<String, dynamic>.from(f),
          ]);
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível carregar os postos (${e.descricao}).');
    } finally {
      if (mounted) setState(() => _carregandoPostos = false);
    }
  }

  Future<void> _cadastrarPosto() async {
    final nome = await pedirNome(context, 'Novo posto', 'Nome do posto');
    if (nome == null || !mounted) return;
    try {
      final criado = await ref.read(apiProvider).criarFornecedor(nome, tipo: 'Posto');
      if (!mounted) return;
      setState(() {
        _postos = [..._postos, criado];
        _postoId = inteiro(criado['id']);
      });
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível cadastrar o posto (${e.descricao}).');
    }
  }

  double? get _total {
    final l = lerDecimal(_litros.text);
    final v = lerDecimal(_valorLitro.text);
    if (l == null || v == null || l == 0 || v == 0) return null;
    return (l * v * 100).round() / 100;
  }

  Future<void> _salvar() async {
    final s = ref.read(sessaoProvider);
    final km = lerDecimal(_km.text);
    final litros = lerDecimal(_litros.text);
    final valorLitro = lerDecimal(_valorLitro.text);

    final erro = s.veiculo == null
        ? 'Selecione um veículo na tela inicial antes de registrar o abastecimento.'
        : s.motorista == null
            ? 'Selecione um motorista na tela inicial antes de registrar o abastecimento.'
            : (km == null || km == 0)
                ? 'Informe o km do odômetro.'
                : (litros == null || litros == 0)
                    ? 'Informe os litros abastecidos.'
                    : (valorLitro == null || valorLitro == 0)
                        ? 'Informe o valor por litro.'
                        : _combustivel == null
                            ? 'Selecione o combustível.'
                            : null;
    if (erro != null) {
      mostrarAviso(context, erro);
      return;
    }

    final lancamento = {
      'tipo': 'Abastecimento',
      'veiculo': {'id': s.veiculo!.id},
      'motorista': {'id': s.motorista!.id},
      'data': dataParaApi(_data),
      'km': km,
      'litros': litros,
      'valorLitro': valorLitro,
      'valor': _total,
      'combustivel': _combustivel,
      'fornecedor': _postoId == null ? null : {'id': _postoId},
      'observacao': _observacao.text,
    };

    setState(() => _salvando = true);
    try {
      final r = await ref.read(lancamentoServiceProvider).salvar(lancamento, foto: _foto, prefixoFoto: 'abastecimento');
      if (!mounted) return;
      mostrarAviso(
          context,
          r.anexoFalhou
              ? 'Abastecimento salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.'
              : 'Abastecimento salvo com sucesso!');
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
    final total = _total;
    return PopScope(
      canPop: !_salvando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Abastecimento')),
        body: ListView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.only(bottom: 24),
          children: [
            const CartaoVeiculoMotorista(motoristaObrigatorio: true),
            Cartao(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Row(children: [
                  IconeSelo(Icons.water_drop_outlined),
                  SizedBox(width: 12),
                  Text('Dados do abastecimento', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 14),
                CampoData(rotulo: 'Data', valor: _data, aoMudar: (d) => setState(() => _data = d ?? _data)),
                const SizedBox(height: 12),
                TextField(
                  controller: _km,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: 'Km do odômetro *', prefixIcon: Icon(Icons.speed_outlined)),
                ),
                const SizedBox(height: 12),
                Row(children: [
                  Expanded(
                    child: TextField(
                      controller: _litros,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      onChanged: (_) => setState(() {}),
                      decoration: const InputDecoration(labelText: 'Litros *', prefixIcon: Icon(Icons.science_outlined)),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: TextField(
                      controller: _valorLitro,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      onChanged: (_) => setState(() {}),
                      decoration: const InputDecoration(labelText: 'Valor/litro (R\$) *', prefixIcon: Icon(Icons.sell_outlined)),
                    ),
                  ),
                ]),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _combustivel,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Combustível *', prefixIcon: Icon(Icons.local_fire_department_outlined)),
                  items: [for (final c in _combustiveis) DropdownMenuItem(value: c, child: Text(c))],
                  onChanged: (v) => setState(() => _combustivel = v),
                ),
                const SizedBox(height: 12),
                CampoEntidade(
                  rotulo: 'Posto',
                  icone: Icons.business_outlined,
                  itens: _postos,
                  selecionadoId: _postoId,
                  carregando: _carregandoPostos,
                  aoMudar: (v) => setState(() => _postoId = v),
                  aoCadastrar: _cadastrarPosto,
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _observacao,
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Observação (opcional)', prefixIcon: Icon(Icons.notes)),
                ),
              ]),
            ),
            if (total != null)
              Cartao(
                child: Row(children: [
                  const IconeSelo(Icons.payments_outlined, cor: corSucesso),
                  const SizedBox(width: 12),
                  const Expanded(child: Text('Valor total', style: TextStyle(color: Colors.black54))),
                  Text('R\$ ${total.toStringAsFixed(2).replaceAll('.', ',')}',
                      style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
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
