import 'package:flutter/material.dart';

import '../data/api_client.dart';

/// Tela de consulta ao vivo (sem cache): carregando, erro com "tentar de novo", vazio ou conteúdo.
/// Usada pelas telas que só fazem sentido com internet.
class CorpoConsulta<T> extends StatelessWidget {
  const CorpoConsulta({
    super.key,
    required this.futuro,
    required this.aoTentarDeNovo,
    required this.vazio,
    required this.construir,
    this.textoVazio = 'Nada encontrado.',
  });

  final Future<T> futuro;
  final VoidCallback aoTentarDeNovo;

  /// Diz se o resultado deve ser tratado como "vazio".
  final bool Function(T) vazio;
  final Widget Function(BuildContext, T) construir;
  final String textoVazio;

  static String descreverErro(Object? erro) =>
      erro is ApiException ? erro.descricao : 'falha inesperada';

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: futuro,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.cloud_off, size: 48, color: Colors.redAccent),
                const SizedBox(height: 8),
                Text('Não foi possível buscar (${descreverErro(snap.error)}).', textAlign: TextAlign.center),
                const SizedBox(height: 12),
                OutlinedButton(onPressed: aoTentarDeNovo, child: const Text('Tentar de novo')),
              ]),
            ),
          );
        }
        final dados = snap.data as T;
        if (vazio(dados)) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.check_circle_outline, size: 56, color: Color(0xFF2DD36F)),
                const SizedBox(height: 8),
                Text(textoVazio, textAlign: TextAlign.center),
              ]),
            ),
          );
        }
        return construir(context, dados);
      },
    );
  }
}
