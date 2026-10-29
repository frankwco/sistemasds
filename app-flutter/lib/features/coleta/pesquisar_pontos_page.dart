import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../app.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';
import 'coletas_do_ponto_page.dart';
import 'telefones_page.dart';

/// Escolha do ponto da cidade: "a coletar" primeiro, "coletados" depois. A busca e a
/// paginação são feitas pelo banco (30 pontos por vez), não em memória.
class PesquisarPontosPage extends ConsumerStatefulWidget {
  const PesquisarPontosPage({super.key, required this.cidade, this.consulta = false});

  final Cidade cidade;

  /// Modo consulta (tela "Dados de pontos"): lista todos os pontos da cidade juntos, sem separar
  /// coletados e sem os atalhos de coletas/telefones.
  final bool consulta;

  @override
  ConsumerState<PesquisarPontosPage> createState() => _PesquisarPontosPageState();
}

class _PesquisarPontosPageState extends ConsumerState<PesquisarPontosPage> {
  static const _tamanhoPagina = 30;

  final _rolagem = ScrollController();
  final _hoje = DateTime.now();

  String _nome = '';
  String _endereco = '';
  Timer? _debounce;

  final List<Ponto> _coletar = [];
  final List<Ponto> _coletados = [];
  int _totalColetar = 0;
  int _totalColetados = 0;
  bool _carregando = true;
  bool _buscandoMais = false;

  /// Incrementa a cada nova busca: respostas de buscas antigas são descartadas.
  int _geracao = 0;

  @override
  void initState() {
    super.initState();
    _rolagem.addListener(() {
      if (_rolagem.position.pixels >= _rolagem.position.maxScrollExtent - 300) {
        _carregarMais();
      }
    });
    _recarregar().then((_) => _avisarPontosComAviso());
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _rolagem.dispose();
    super.dispose();
  }

  bool get _temMais => _coletar.length < _totalColetar || _coletados.length < _totalColetados;

  Future<void> _recarregar() async {
    final minha = ++_geracao;
    final repo = ref.read(pontosRepoProvider);
    setState(() => _carregando = true);
    final cid = widget.cidade.id;
    final totalColetar = await repo.contar(cidadeId: cid, hoje: _hoje, coletados: widget.consulta ? null : false, nome: _nome, endereco: _endereco);
    final totalColetados = widget.consulta
        ? 0
        : await repo.contar(cidadeId: cid, hoje: _hoje, coletados: true, nome: _nome, endereco: _endereco);
    final primeiros = await repo.pagina(
        cidadeId: cid, hoje: _hoje, coletados: widget.consulta ? null : false, nome: _nome, endereco: _endereco, limite: _tamanhoPagina, offset: 0);
    if (!mounted || minha != _geracao) return;
    setState(() {
      _totalColetar = totalColetar;
      _totalColetados = totalColetados;
      _coletar
        ..clear()
        ..addAll(primeiros);
      _coletados.clear();
      _carregando = false;
    });
    // Poucos a coletar: a primeira página não enche a tela, já traz os coletados.
    if (primeiros.length < _tamanhoPagina) await _carregarMais();
  }

  Future<void> _carregarMais() async {
    if (_buscandoMais || _carregando || !_temMais) return;
    _buscandoMais = true;
    final minha = _geracao;
    final repo = ref.read(pontosRepoProvider);
    final cid = widget.cidade.id;
    try {
      final aColetar = _coletar.length < _totalColetar;
      final lote = await repo.pagina(
        cidadeId: cid,
        hoje: _hoje,
        coletados: widget.consulta ? null : !aColetar,
        nome: _nome,
        endereco: _endereco,
        limite: _tamanhoPagina,
        offset: aColetar ? _coletar.length : _coletados.length,
      );
      if (!mounted || minha != _geracao) return;
      setState(() => (aColetar ? _coletar : _coletados).addAll(lote));
    } finally {
      _buscandoMais = false;
    }
  }

  void _aoDigitar({String? nome, String? endereco}) {
    _nome = nome ?? _nome;
    _endereco = endereco ?? _endereco;
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 250), _recarregar);
  }

  /// Aviso geral da cidade: no máximo 2 vezes por dia.
  Future<void> _avisarPontosComAviso() async {
    if (!mounted) return;
    final sessao = ref.read(sessaoRepoProvider);
    final dia = DateFormat('yyyyMMdd').format(_hoje);
    final vezes = await sessao.avisosHoje(widget.cidade.id, dia);
    if (vezes >= 2 || !mounted) return;
    final comAviso = await ref.read(pontosRepoProvider).comAviso(widget.cidade.id);
    if (comAviso.isEmpty || !mounted) return;
    await sessao.registrarAvisoHoje(widget.cidade.id, dia, vezes + 1);
    if (!mounted) return;
    await informar(
      context,
      titulo: 'ATENÇÃO: pontos com aviso, verifique antes de coletar',
      botao: 'Ciente!!',
      conteudo: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (final p in comAviso) ...[Text(p.nomeRazaoSocial), const Divider()],
        ],
      ),
    );
  }

  Future<void> _selecionar(Ponto ponto) async {
    if (ponto.situacaoAviso && !widget.consulta) {
      final coletar = await confirmar(
        context,
        titulo: '${ponto.nomeRazaoSocial} ${ponto.apelido}',
        conteudo: Text('AVISO: ${ponto.aviso}'),
        confirmar: 'Coletar!!',
        cancelar: 'NÃO COLETAR',
      );
      if (!coletar || !mounted) return;
    }
    Navigator.pop(context, ponto);
  }

  Future<void> _mostrarAviso(Ponto ponto) => informar(
        context,
        titulo: '${ponto.nomeRazaoSocial} ${ponto.apelido}',
        conteudo: Text('AVISO: ${ponto.aviso}'),
      );

  void _abrirColetasDoPonto(Ponto p) =>
      Navigator.push(context, MaterialPageRoute(builder: (_) => ColetasDoPontoPage(ponto: p)));

  void _abrirTelefones(Ponto p) => Navigator.push(context, MaterialPageRoute(builder: (_) => TelefonesPage(ponto: p)));

  Widget _cabecalho(IconData icone, Color cor, String titulo, int total) => Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
        child: Row(children: [
          IconeSelo(icone, cor: cor),
          const SizedBox(width: 10),
          Text(titulo, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          const Spacer(),
          if (total > 0) Chip(label: Text('$total'), visualDensity: VisualDensity.compact),
        ]),
      );

  Widget _cartaoPonto(Ponto p, {required bool coletado}) {
    return Cartao(
      cor: coletado ? const Color(0xFFEAF8F0) : null,
      onTap: () => coletado ? Navigator.pop(context) : _selecionar(p),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          IconeSelo(coletado ? Icons.check : Icons.business_outlined, cor: coletado ? corSucesso : corPrimaria),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(p.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.w600)),
              if (p.nomeFantasia.isNotEmpty) Text('(${p.nomeFantasia}) ${p.apelido}'),
              Text(p.enderecoCompleto, style: const TextStyle(color: Colors.black54, fontSize: 12)),
            ]),
          ),
        ]),
        if (!widget.consulta) const SizedBox(height: 8),
        if (!widget.consulta) Wrap(spacing: 6, runSpacing: 4, children: [
          if (p.coletaUnica)
            const Chip(label: Text('COLETA ÚNICA'), visualDensity: VisualDensity.compact)
          else if (p.dataUltimaColeta == null)
            Chip(label: Text('1ª coleta - ${p.periodicidadeColeta}'), visualDensity: VisualDensity.compact),
          if (p.dataUltimaColeta != null)
            Chip(label: Text('Última: ${formatarData(p.dataUltimaColeta!)}'), visualDensity: VisualDensity.compact),
          ActionChip(
            avatar: const Icon(Icons.shopping_bag_outlined, size: 16),
            label: Text('${p.quantidadeColetadaMes}/${p.quantidadeColetaMes}'),
            visualDensity: VisualDensity.compact,
            onPressed: () => _abrirColetasDoPonto(p),
          ),
          ActionChip(
            avatar: const Icon(Icons.schedule, size: 16),
            label: Text(p.periodicidadeColeta),
            visualDensity: VisualDensity.compact,
            onPressed: () => _abrirColetasDoPonto(p),
          ),
          ActionChip(
            avatar: const Icon(Icons.call_outlined, size: 16),
            label: const Text('Fones'),
            visualDensity: VisualDensity.compact,
            onPressed: () => _abrirTelefones(p),
          ),
          if (p.situacaoAviso)
            ActionChip(
              avatar: const Icon(Icons.error_outline, size: 16, color: corPerigo),
              label: const Text('Aviso'),
              visualDensity: VisualDensity.compact,
              onPressed: () => _mostrarAviso(p),
            ),
        ]),
      ]),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Pontos - ${widget.cidade.nome}')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
            child: TextField(
              decoration: const InputDecoration(hintText: 'Pesquisar por ponto', prefixIcon: Icon(Icons.search)),
              onChanged: (v) => _aoDigitar(nome: v),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
            child: TextField(
              decoration: const InputDecoration(hintText: 'Pesquisar por endereço', prefixIcon: Icon(Icons.map_outlined)),
              onChanged: (v) => _aoDigitar(endereco: v),
            ),
          ),
          Expanded(
            child: _carregando
                ? const Center(child: CircularProgressIndicator())
                : ListView(
                    controller: _rolagem,
                    keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                    padding: const EdgeInsets.only(bottom: 24),
                    children: [
                      _cabecalho(Icons.location_on_outlined, corPrimaria, widget.consulta ? 'Pontos de coleta' : 'Pontos para coletar', _totalColetar),
                      if (_totalColetar == 0)
                        const Padding(padding: EdgeInsets.all(16), child: Text('Nenhum ponto a coletar.')),
                      for (final p in _coletar) _cartaoPonto(p, coletado: false),
                      if (_totalColetados > 0) ...[
                        _cabecalho(Icons.check_circle_outline, corSucesso, 'Pontos coletados', _totalColetados),
                        for (final p in _coletados) _cartaoPonto(p, coletado: true),
                      ],
                      if (_temMais)
                        const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator())),
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}
