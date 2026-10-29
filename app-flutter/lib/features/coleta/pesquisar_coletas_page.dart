import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/ui.dart';
import '../../data/coleta_item.dart';
import '../../providers.dart';
import '../../background/envio_em_segundo_plano.dart';

/// Coletas recentes deste aparelho (as do servidor mescladas), da mais nova para a mais
/// antiga, com busca e exclusão. Lê 30 por vez do banco.
class PesquisarColetasPage extends ConsumerStatefulWidget {
  const PesquisarColetasPage({super.key});

  @override
  ConsumerState<PesquisarColetasPage> createState() => _PesquisarColetasPageState();
}

class _PesquisarColetasPageState extends ConsumerState<PesquisarColetasPage> {
  static const _tamanhoPagina = 30;

  final _rolagem = ScrollController();
  final List<ColetaItem> _itens = [];
  String _busca = '';
  int _total = 0;
  bool _carregando = true;
  bool _buscandoMais = false;
  bool _excluindo = false;
  int _geracao = 0;
  Timer? _debounce;
  late final ValueNotifier<int> _mudancas = ref.read(coletasRepoProvider).mudou;

  @override
  void initState() {
    super.initState();
    _mudancas.addListener(_atualizarSilencioso);
    _rolagem.addListener(() {
      if (_rolagem.position.pixels >= _rolagem.position.maxScrollExtent - 300) _carregarMais();
    });
    _recarregar();
  }

  @override
  void dispose() {
    _mudancas.removeListener(_atualizarSilencioso);
    _debounce?.cancel();
    _rolagem.dispose();
    super.dispose();
  }

  Future<void> _recarregar() async {
    final minha = ++_geracao;
    final repo = ref.read(coletasRepoProvider);
    setState(() => _carregando = true);
    final total = await repo.contar(_busca);
    final primeiros = await repo.pagina(busca: _busca, limite: _tamanhoPagina, offset: 0);
    if (!mounted || minha != _geracao) return;
    setState(() {
      _total = total;
      _itens
        ..clear()
        ..addAll(primeiros);
      _carregando = false;
    });
  }

  /// Uma coleta mudou (foi enviada, falhou, chegou do servidor): refaz o que já está na
  /// tela, sem spinner nem voltar ao topo.
  Future<void> _atualizarSilencioso() async {
    if (_carregando || !mounted) return;
    final minha = _geracao;
    final repo = ref.read(coletasRepoProvider);
    final total = await repo.contar(_busca);
    final itens = await repo.pagina(busca: _busca, limite: _itens.length < _tamanhoPagina ? _tamanhoPagina : _itens.length, offset: 0);
    if (!mounted || minha != _geracao) return;
    setState(() {
      _total = total;
      _itens
        ..clear()
        ..addAll(itens);
    });
  }

  Future<void> _carregarMais() async {
    if (_buscandoMais || _carregando || _itens.length >= _total) return;
    _buscandoMais = true;
    final minha = _geracao;
    try {
      final lote = await ref.read(coletasRepoProvider).pagina(busca: _busca, limite: _tamanhoPagina, offset: _itens.length);
      if (!mounted || minha != _geracao) return;
      setState(() => _itens.addAll(lote));
    } finally {
      _buscandoMais = false;
    }
  }

  Future<void> _confirmarExclusao(ColetaItem item) async {
    // Conta de somar para evitar exclusão por toque acidental (igual ao app anterior).
    final r = Random();
    final a = r.nextInt(10) + 1;
    final b = r.nextInt(10) + 1;
    final campo = TextEditingController();

    final resposta = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirmação!'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Deseja EXCLUIR a coleta de ${item.nomePonto}?'),
            const SizedBox(height: 6),
            Wrap(spacing: 6, children: [
              for (final g in item.grupos) Text('${g.nome}: ${g.valor}', style: const TextStyle(fontWeight: FontWeight.bold)),
            ]),
            const SizedBox(height: 14),
            Text('Qual a soma de $a + $b?', style: const TextStyle(color: corPerigo, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            TextField(
              controller: campo,
              keyboardType: TextInputType.number,
              autofocus: true,
              decoration: const InputDecoration(hintText: 'Insira o resultado'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('CANCELAR')),
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(100, 40), backgroundColor: corPerigo),
            onPressed: () => Navigator.pop(ctx, campo.text.trim()),
            child: const Text('Excluir'),
          ),
        ],
      ),
    );
    campo.dispose();
    if (resposta == null || !mounted) return;
    if (resposta != '${a + b}') {
      mostrarAviso(context, 'O resultado não corresponde. Tente novamente!');
      return;
    }
    await _excluir(item);
  }

  Future<void> _excluir(ColetaItem item) async {
    setState(() => _excluindo = true);
    try {
      // A exclusão também fica marcada como pendente no backup do Firebase (fb = 2) até ele confirmar.
      await ref.read(coletasRepoProvider).excluir(item.id, espelhar: ref.read(firebaseBackupProvider).ativo);
      unawaited(ref.read(espelhoProvider).espelharPendentes());
      // A exclusão fica pendente e vai ao servidor agora; se falhar, o próximo ciclo reenvia.
      final sync = ref.read(syncProvider);
      unawaited(sync.atualizarPendentes());
      unawaited(sync.enviarPendentes().catchError((_) {}));
      unawaited(AgendadorEnvio.agendarEnvio());
      if (!mounted) return;
      setState(() {
        _itens.removeWhere((i) => i.id == item.id);
        _total = max(0, _total - 1);
      });
      mostrarAviso(context, 'Excluído!');
    } catch (e) {
      debugPrint('[Coleta] falha ao excluir: $e');
      if (mounted) {
        mostrarAviso(context, 'Erro ao excluir a coleta, tente novamente.');
        unawaited(_recarregar());
      }
    } finally {
      if (mounted) setState(() => _excluindo = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_excluindo,
      child: Scaffold(
        appBar: AppBar(title: const Text('Pesquisar coletas')),
        body: Stack(
          children: [
            Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(12),
                  child: TextField(
                    decoration: const InputDecoration(hintText: 'Pesquisar coleta', prefixIcon: Icon(Icons.search)),
                    onChanged: (v) {
                      _busca = v;
                      _debounce?.cancel();
                      _debounce = Timer(const Duration(milliseconds: 250), _recarregar);
                    },
                  ),
                ),
                Expanded(
                  child: _carregando
                      ? const Center(child: CircularProgressIndicator())
                      : _itens.isEmpty
                          ? const Center(child: Text('Nenhuma coleta encontrada'))
                          : ListView.builder(
                              controller: _rolagem,
                              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
                              itemCount: _itens.length,
                              itemBuilder: (_, i) {
                                final item = _itens[i];
                                return Cartao(
                                  onTap: () => _confirmarExclusao(item),
                                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                    Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                      Expanded(child: Text(item.titulo, style: const TextStyle(fontWeight: FontWeight.w600))),
                                      const SizedBox(width: 6),
                                      Icon(
                                        item.sincronizado ? Icons.cloud_done : Icons.cloud_upload_outlined,
                                        size: 20,
                                        color: item.sincronizado ? corPrimaria : corPerigo,
                                      ),
                                    ]),
                                    const SizedBox(height: 8),
                                    Wrap(spacing: 6, runSpacing: 6, children: [
                                      for (final g in item.grupos)
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                          decoration: BoxDecoration(
                                              color: g.cor, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.black12)),
                                          child: Text('${g.nome}  ${g.valor}', style: TextStyle(color: g.texto, fontSize: 12)),
                                        ),
                                    ]),
                                    const SizedBox(height: 6),
                                    Text('Total: ${item.total} - ${item.data}', style: const TextStyle(color: Colors.black54, fontSize: 12)),
                                    if (!item.sincronizado)
                                      Padding(
                                        padding: const EdgeInsets.only(top: 4),
                                        child: Text(
                                          item.erro == null
                                              ? 'Aguardando envio...'
                                              : 'Falha no envio (${item.tentativas}x): ${item.erro}',
                                          style: const TextStyle(color: corPerigo, fontSize: 12),
                                        ),
                                      ),
                                  ]),
                                );
                              },
                            ),
                ),
              ],
            ),
            if (_excluindo) const SobreposicaoOcupado(mensagem: 'Excluindo coleta...'),
          ],
        ),
      ),
    );
  }
}
