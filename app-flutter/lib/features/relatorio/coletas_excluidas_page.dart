import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';

/// TELA PROVISÓRIA: lista as coletas de 29/10/2026 que estão no Firebase com `status: false`
/// (excluídas por engano), para conferir os dados e lançá-las de novo. Remover depois.
class ColetasExcluidasPage extends ConsumerStatefulWidget {
  const ColetasExcluidasPage({super.key});

  @override
  ConsumerState<ColetasExcluidasPage> createState() => _ColetasExcluidasPageState();
}

class _ColetasExcluidasPageState extends ConsumerState<ColetasExcluidasPage> {
  static final _dia = DateTime(2026, 10, 29);

  late Future<List<Map<String, dynamic>>> _futuro;

  @override
  void initState() {
    super.initState();
    _futuro = _carregar();
  }

  Future<List<Map<String, dynamic>>> _carregar() async {
    final docs = await ref.read(firebaseBackupProvider).coletasExcluidasDoDia(_dia);
    docs.sort((a, b) => _hora(a).compareTo(_hora(b)));
    return docs;
  }

  String _hora(Map<String, dynamic> c) =>
      '${_dois(c['horaUltimaColeta'])}:${_dois(c['minutoUltimaColeta'])}:${_dois(c['segundoUltimaColeta'])}';

  String _dois(Object? v) => (v is num ? v.toInt() : int.tryParse('$v') ?? 0).toString().padLeft(2, '0');

  void _recarregar() => setState(() => _futuro = _carregar());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Coletas excluídas'),
        actions: [IconButton(tooltip: 'Recarregar', icon: const Icon(Icons.refresh), onPressed: _recarregar)],
      ),
      body: FutureBuilder<List<Map<String, dynamic>>>(
        future: _futuro,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  ref.read(firebaseBackupProvider).ativo
                      ? 'Não foi possível consultar o Firebase.\n${snap.error}'
                      : 'Firebase não configurado neste build (falta o env.json).',
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }
          final docs = snap.data!;
          return ListView(
            padding: const EdgeInsets.symmetric(vertical: 8),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                child: Text('${formatarData(_dia)} — ${docs.length} coleta(s) com status false',
                    style: const TextStyle(color: Colors.black54)),
              ),
              if (docs.isEmpty)
                const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Nenhuma coleta excluída neste dia.'))),
              for (final c in docs) _Cartao(c, _hora(c)),
            ],
          );
        },
      ),
    );
  }
}

class _Cartao extends StatelessWidget {
  const _Cartao(this.c, this.hora);

  final Map<String, dynamic> c;
  final String hora;

  @override
  Widget build(BuildContext context) {
    final ponto = Ponto(Map<String, dynamic>.from((c['pontoColetaSelecionado'] as Map?) ?? {}));
    final veiculo = (c['veiculo'] as Map?) ?? {};
    final pesos = [
      for (final g in gruposColeta)
        if (((c[g.campo] as num?) ?? 0) > 0) '${g.nome}: ${c[g.campo]} kg',
    ];
    Widget linha(String rotulo, String valor) => Padding(
          padding: const EdgeInsets.only(top: 2),
          child: Text.rich(TextSpan(children: [
            TextSpan(text: '$rotulo: ', style: const TextStyle(color: Colors.black54)),
            TextSpan(text: valor),
          ])),
        );

    return Cartao(
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(ponto.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.bold)),
        if (ponto.endereco.isNotEmpty) Text(ponto.endereco, style: const TextStyle(color: Colors.black54, fontSize: 12)),
        const SizedBox(height: 6),
        linha('Hora da coleta', hora),
        linha('Situação', '${c['situacao'] ?? ''}'),
        if (pesos.isNotEmpty) linha('Pesos', pesos.join('  ')),
        linha('Total', '${c['totalColetado'] ?? 0} kg'),
        linha('Veículo', '${veiculo['descricao'] ?? ''} ${veiculo['placa'] ?? ''}'.trim()),
        linha('Motorista', '${c['nomeMotorista'] ?? ''}'),
        linha('Usuário', '${c['emailUsuario'] ?? ''}'),
        if (c['latitude'] != null) linha('Local', '${c['latitude']}, ${c['longitude']}'),
        linha('Excluída em', '${c['dataExclusaoApp'] ?? '—'}'),
        linha('Identificador', '${c['identificadorApp'] ?? ''}'),
      ]),
    );
  }
}
