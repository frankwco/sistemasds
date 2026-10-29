import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../data/coleta_item.dart';
import '../../data/models.dart';
import '../../providers.dart';

/// Coletas de um ponto nos últimos 365 dias, consultadas ao vivo no servidor
/// (o aparelho só guarda as coletas recentes).
class ColetasDoPontoPage extends ConsumerStatefulWidget {
  const ColetasDoPontoPage({super.key, required this.ponto});

  final Ponto ponto;

  @override
  ConsumerState<ColetasDoPontoPage> createState() => _ColetasDoPontoPageState();
}

class _ColetasDoPontoPageState extends ConsumerState<ColetasDoPontoPage> {
  late Future<List<ColetaItem>> _futuro = _buscar();

  Future<List<ColetaItem>> _buscar() async {
    final desde = DateTime.now().subtract(const Duration(days: 365));
    final lista = await ref
        .read(apiProvider)
        .todasColetas(widget.ponto.id, DateFormat('yyyy-MM-dd HH:mm:ss').format(desde));
    final itens = <ColetaItem>[];
    for (final e in lista) {
      if (e is! Map) continue;
      final json = Map<String, dynamic>.from(e);
      itens.add(ColetaItem.deJson(id: '${json['identificadorApp'] ?? itens.length}', json: json, sincronizado: true));
    }
    itens.sort((a, b) => b.instante.compareTo(a.instante));
    return itens;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Coletas do ponto')),
      body: FutureBuilder<List<ColetaItem>>(
        future: _futuro,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final erro = snap.error;
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(erro is ApiException ? 'Não foi possível buscar: ${erro.descricao}' : 'Não foi possível buscar as coletas.',
                      textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: () => setState(() => _futuro = _buscar()), child: const Text('Tentar de novo')),
                ]),
              ),
            );
          }
          final itens = snap.data!;
          if (itens.isEmpty) {
            return const Center(child: Text('Nenhuma coleta nos últimos 365 dias.'));
          }
          return ListView.builder(
            padding: const EdgeInsets.only(bottom: 24),
            itemCount: itens.length + 1,
            itemBuilder: (_, i) {
              if (i == 0) {
                return Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: Text('Últimos 365 dias - ${itens.length} coleta${itens.length > 1 ? 's' : ''}',
                      style: const TextStyle(color: Colors.black54)),
                );
              }
              final c = itens[i - 1];
              return Cartao(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    const Icon(Icons.calendar_today_outlined, size: 16, color: Colors.black54),
                    const SizedBox(width: 6),
                    Text(c.data, style: const TextStyle(fontWeight: FontWeight.w600)),
                    const Spacer(),
                    Text('Total ${c.total}', style: const TextStyle(fontWeight: FontWeight.bold)),
                  ]),
                  if ((c.json['emailUsuario'] ?? '').toString().isNotEmpty ||
                      (c.json['situacao'] ?? '').toString().isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text([c.json['emailUsuario'], c.json['situacao']]
                        .where((s) => s != null && s.toString().isNotEmpty)
                        .join(' - '),
                        style: const TextStyle(color: Colors.black54, fontSize: 12)),
                  ],
                  const SizedBox(height: 8),
                  Wrap(spacing: 6, runSpacing: 6, children: [
                    for (final g in c.grupos)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(color: g.cor, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.black12)),
                        child: Text('${g.nome}  ${g.valor}', style: TextStyle(color: g.texto, fontSize: 12)),
                      ),
                  ]),
                ]),
              );
            },
          );
        },
      ),
    );
  }
}
