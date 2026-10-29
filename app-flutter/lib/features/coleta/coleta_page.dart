import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/config.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/coleta_payload.dart';
import '../../data/models.dart';
import '../../providers.dart';
import '../home/selecao_page.dart';
import 'assinatura_page.dart';
import 'pesquisar_coletas_page.dart';
import 'pesquisar_pontos_page.dart';

class ColetaPage extends ConsumerStatefulWidget {
  const ColetaPage({super.key});

  @override
  ConsumerState<ColetaPage> createState() => _ColetaPageState();
}

class _ColetaPageState extends ConsumerState<ColetaPage> {
  Ponto? _ponto;
  String _situacao = 'Coletado';

  /// Um campo de texto por grupo; o valor de verdade é o texto convertido em milésimos.
  late final Map<String, TextEditingController> _campos = {
    for (final g in gruposColeta) g.campo: TextEditingController(text: formatarPeso(0)),
  };

  Map<String, int> get _valores => {for (final g in gruposColeta) g.campo: pesoParaMilesimos(_campos[g.campo]!.text)};

  @override
  void dispose() {
    for (final c in _campos.values) {
      c.dispose();
    }
    super.dispose();
  }

  void _zerarFormulario() {
    for (final c in _campos.values) {
      c.text = formatarPeso(0);
    }
    _situacao = 'Coletado';
  }

  Future<void> _escolherCidade() async {
    final cidades = await ref.read(catalogoRepoProvider).listarCidades();
    if (!mounted) return;
    final c = await Navigator.push<Cidade>(
      context,
      MaterialPageRoute(
        builder: (_) => SelecaoPage<Cidade>(
          titulo: 'Cidade',
          itens: cidades,
          rotulo: (c) => c.nome,
          buscavel: true,
        ),
      ),
    );
    if (c == null) return;
    await ref.read(sessaoProvider.notifier).escolherCidade(c);
    // O ponto escolhido era de outra cidade.
    setState(() {
      _ponto = null;
      _zerarFormulario();
    });
  }

  Future<void> _escolherPonto() async {
    final cidade = ref.read(sessaoProvider).cidade;
    if (cidade == null) return;
    final p = await Navigator.push<Ponto>(
      context,
      MaterialPageRoute(builder: (_) => PesquisarPontosPage(cidade: cidade)),
    );
    if (p == null) return;
    setState(() {
      _ponto = p;
      _zerarFormulario();
    });
  }

  Future<void> _abrirWhatsapp() async {
    final p = _ponto;
    if (p == null) return;
    final ok = await abrirWhatsapp(
      Config.whatsappSuporte,
      '${p.nomeRazaoSocial}/${p.nomeFantasia} - ${ref.read(sessaoProvider).cidade?.nome ?? ''}',
    );
    if (!ok && mounted) mostrarAviso(context, 'Não foi possível abrir o WhatsApp.');
  }

  Future<void> _concluir() async {
    final ponto = _ponto;
    if (ponto == null) return;
    final sessao = ref.read(sessaoProvider);
    final valores = valoresEfetivos(_situacao, _valores);

    if (sessao.veiculo == null || sessao.motorista == null) {
      final seguir = await confirmar(
        context,
        titulo: 'Veículo ou motorista',
        conteudo: Text(sessao.veiculo == null && sessao.motorista == null
            ? 'Nenhum veículo e nenhum motorista foram selecionados na tela inicial.'
            : sessao.veiculo == null
                ? 'Nenhum veículo foi selecionado na tela inicial.'
                : 'Nenhum motorista foi selecionado na tela inicial.'),
        confirmar: 'Continuar assim mesmo',
        cancelar: 'Voltar',
      );
      if (!seguir || !mounted) return;
    }
    if (_situacao == 'Coletado') {
      if (temPesoSuspeito(valores)) {
        final seguir = await confirmar(
          context,
          titulo: 'Confirmar!',
          conteudo: Text('Verifique se os valores estão corretos, percebi um peso maior que ${Config.pesoSuspeitoKg} kg. Deseja continuar?'),
          confirmar: 'Sim',
          cancelar: 'Não/Cancelar',
        );
        if (!seguir || !mounted) return;
      } else if (somarMilesimos(valores) == 0) {
        final seguir = await confirmar(
          context,
          titulo: 'Total zerado',
          conteudo: const Text('Nenhum peso foi informado. Deseja continuar mesmo assim?'),
          confirmar: 'Continuar',
          cancelar: 'Voltar',
        );
        if (!seguir || !mounted) return;
      }
    }

    final salvou = await Navigator.push<bool>(
      context,
      MaterialPageRoute(builder: (_) => AssinaturaPage(ponto: ponto, situacao: _situacao, valores: valores)),
    );
    if (salvou == true && mounted) {
      mostrarAviso(context, 'Coleta inserida com sucesso!');
      // Pronto para a próxima: a cidade continua, o ponto e os pesos zeram.
      setState(() {
        _ponto = null;
        _zerarFormulario();
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final cidade = ref.watch(sessaoProvider.select((s) => s.cidade));
    final ponto = _ponto;
    final coletando = _situacao == 'Coletado';

    return Scaffold(
      appBar: AppBar(
        title: Text('Coleta - ${formatarData(DateTime.now())}'),
        actions: [
          if (ponto != null) IconButton(tooltip: 'WhatsApp', icon: const Icon(Icons.chat), onPressed: _abrirWhatsapp),
          IconButton(
            tooltip: 'Pesquisar coletas',
            icon: const Icon(Icons.search),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const PesquisarColetasPage())),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
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
                      Text(ponto.apelido.isEmpty ? ponto.nomeFantasia : '${ponto.nomeFantasia} - ${ponto.apelido}',
                          style: const TextStyle(color: Colors.black54)),
                    if (ponto != null && ponto.endereco.isNotEmpty)
                      Text(ponto.endereco, style: const TextStyle(color: Colors.black45, fontSize: 12)),
                  ]),
                ),
                const Icon(Icons.chevron_right),
              ]),
            ),
          if (ponto != null && ponto.situacaoAviso)
            Cartao(
              cor: const Color(0xFFFFF4D6),
              child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const IconeSelo(Icons.warning_amber_rounded, cor: Color(0xFFE0A800)),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('Aviso', style: TextStyle(fontWeight: FontWeight.bold)),
                    Text(ponto.aviso),
                  ]),
                ),
              ]),
            ),
          if (ponto != null) ...[
            Cartao(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Row(children: [
                  IconeSelo(Icons.assignment_outlined),
                  SizedBox(width: 12),
                  Text('Dados da coleta', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                ]),
                const SizedBox(height: 14),
                DropdownButtonFormField<String>(
                  key: ValueKey(_situacao),
                  initialValue: _situacao,
                  decoration: const InputDecoration(labelText: 'Situação', prefixIcon: Icon(Icons.flag_outlined)),
                  items: [for (final s in situacoesColeta) DropdownMenuItem(value: s, child: Text(s))],
                  onChanged: (v) => setState(() => _situacao = v ?? 'Coletado'),
                ),
                if (coletando)
                  for (final g in ponto.gruposDoPonto)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Row(children: [
                        Container(
                          width: 52,
                          height: 48,
                          alignment: Alignment.center,
                          decoration: BoxDecoration(color: g.cor, borderRadius: BorderRadius.circular(10), border: Border.all(color: Colors.black12)),
                          child: Text(g.nome, style: TextStyle(color: g.texto, fontWeight: FontWeight.bold)),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: TextField(
                            controller: _campos[g.campo],
                            keyboardType: TextInputType.number,
                            inputFormatters: [PesoInputFormatter()],
                            textAlign: TextAlign.end,
                            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
                            onTap: () => _campos[g.campo]!.selection =
                                TextSelection.collapsed(offset: _campos[g.campo]!.text.length),
                            onChanged: (_) => setState(() {}),
                            decoration: const InputDecoration(suffixText: 'kg'),
                          ),
                        ),
                      ]),
                    ),
              ]),
            ),
            if (coletando)
              Cartao(
                child: Row(children: [
                  const IconeSelo(Icons.calculate_outlined, cor: corSucesso),
                  const SizedBox(width: 12),
                  const Expanded(child: Text('Total coletado', style: TextStyle(color: Colors.black54))),
                  Text('${formatarPeso(somarMilesimos(_valores))} kg',
                      style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                ]),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: FilledButton.icon(
                onPressed: _concluir,
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Concluir / Assinar'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
