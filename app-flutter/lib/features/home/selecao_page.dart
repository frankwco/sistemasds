import 'package:flutter/material.dart';

import '../../core/formato.dart';
import '../../core/ui.dart';

/// Lista de itens em que o operador toca em um (cidade, veículo, motorista). Devolve o
/// item escolhido com Navigator.pop; sem busca para listas curtas.
class SelecaoPage<T> extends StatefulWidget {
  const SelecaoPage({
    super.key,
    required this.titulo,
    required this.itens,
    required this.rotulo,
    this.subtitulo,
    this.buscavel = false,
    this.mensagemConfirmacao,
    this.vazio = 'Nada para mostrar. Sincronize e tente de novo.',
  });

  final String titulo;
  final List<T> itens;
  final String Function(T) rotulo;
  final String Function(T)? subtitulo;
  final bool buscavel;

  /// Se informado, pede confirmação antes de devolver o item.
  final String Function(T)? mensagemConfirmacao;
  final String vazio;

  @override
  State<SelecaoPage<T>> createState() => _SelecaoPageState<T>();
}

class _SelecaoPageState<T> extends State<SelecaoPage<T>> {
  String _busca = '';

  Future<void> _escolher(T item) async {
    final msg = widget.mensagemConfirmacao;
    if (msg != null) {
      final ok = await confirmar(context, titulo: 'Confirmação', conteudo: Text(msg(item)));
      if (!ok || !mounted) return;
    }
    Navigator.pop(context, item);
  }

  @override
  Widget build(BuildContext context) {
    final termo = normalizar(_busca).trim();
    final itens = termo.isEmpty
        ? widget.itens
        : widget.itens.where((i) => normalizar(widget.rotulo(i)).contains(termo)).toList();

    return Scaffold(
      appBar: AppBar(title: Text(widget.titulo)),
      body: Column(
        children: [
          if (widget.buscavel)
            Padding(
              padding: const EdgeInsets.all(12),
              child: TextField(
                decoration: const InputDecoration(hintText: 'Pesquisar', prefixIcon: Icon(Icons.search)),
                onChanged: (v) => setState(() => _busca = v),
              ),
            ),
          Expanded(
            child: widget.itens.isEmpty
                ? Center(child: Padding(padding: const EdgeInsets.all(24), child: Text(widget.vazio, textAlign: TextAlign.center)))
                : ListView.separated(
                    itemCount: itens.length,
                    separatorBuilder: (_, __) => const Divider(height: 1),
                    itemBuilder: (_, i) {
                      final item = itens[i];
                      final sub = widget.subtitulo?.call(item);
                      return ListTile(
                        title: Text(widget.rotulo(item)),
                        subtitle: sub == null || sub.isEmpty ? null : Text(sub),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => _escolher(item),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
