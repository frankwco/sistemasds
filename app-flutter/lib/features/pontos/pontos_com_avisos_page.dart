import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';
import '../coleta/coletas_do_ponto_page.dart';

/// Pontos com aviso cadastrado, de todas as cidades. Lê do banco local, funciona sem internet.
class PontosComAvisosPage extends ConsumerWidget {
  const PontosComAvisosPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Pontos com avisos')),
      body: FutureBuilder<List<Ponto>>(
        future: ref.read(pontosRepoProvider).todosComAviso(),
        builder: (context, snap) {
          if (!snap.hasData) return const Center(child: CircularProgressIndicator());
          final pontos = snap.data!;
          if (pontos.isEmpty) {
            return const Center(child: Text('Nenhum aviso ativo! Todos os pontos estão sem avisos.'));
          }
          return ListView.builder(
            padding: const EdgeInsets.only(bottom: 24),
            itemCount: pontos.length + 1,
            itemBuilder: (_, i) {
              if (i == 0) {
                return Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  child: Text('${pontos.length} ponto${pontos.length > 1 ? 's' : ''} com aviso',
                      style: const TextStyle(color: Colors.black54)),
                );
              }
              final p = pontos[i - 1];
              return Cartao(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(p.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.w600)),
                  if (p.nomeFantasia.isNotEmpty) Text(p.nomeFantasia),
                  const SizedBox(height: 4),
                  Text(p.enderecoCompleto, style: const TextStyle(color: Colors.black54, fontSize: 12)),
                  const SizedBox(height: 8),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(color: const Color(0xFFFFF4D6), borderRadius: BorderRadius.circular(10)),
                    child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Icon(Icons.error_outline, color: Color(0xFFE0A800), size: 20),
                      const SizedBox(width: 8),
                      Expanded(child: Text(p.aviso)),
                    ]),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    icon: const Icon(Icons.shopping_bag_outlined),
                    label: const Text('Coletas anteriores'),
                    onPressed: () => Navigator.push(
                        context, MaterialPageRoute(builder: (_) => ColetasDoPontoPage(ponto: p))),
                  ),
                ]),
              );
            },
          );
        },
      ),
    );
  }
}
