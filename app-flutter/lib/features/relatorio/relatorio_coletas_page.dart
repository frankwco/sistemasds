import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../data/relatorio.dart';
import '../../providers.dart';
import '../veiculo/componentes.dart';

/// Relatório de coletas por período, a partir do backup no Firestore (um documento por ponto e dia).
class RelatorioColetasPage extends ConsumerStatefulWidget {
  const RelatorioColetasPage({super.key});

  @override
  ConsumerState<RelatorioColetasPage> createState() => _RelatorioColetasPageState();
}

class _RelatorioColetasPageState extends ConsumerState<RelatorioColetasPage> {
  static const _maxDias = 62;

  DateTime _inicial = DateTime.now();
  DateTime _final = DateTime.now();
  bool _buscando = false;
  List<LinhaRelatorio> _todas = [];
  String? _placa;
  bool _pesquisou = false;

  String _iso(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> _pesquisar() async {
    final ini = DateTime(_inicial.year, _inicial.month, _inicial.day);
    final fim = DateTime(_final.year, _final.month, _final.day);
    if (fim.isBefore(ini)) {
      mostrarAviso(context, 'A data final é anterior à inicial.');
      return;
    }
    final dias = [for (var d = ini; !d.isAfter(fim); d = DateTime(d.year, d.month, d.day + 1)) d];
    if (dias.length > _maxDias) {
      mostrarAviso(context, 'Consulte no máximo $_maxDias dias por vez.');
      return;
    }
    final backup = ref.read(firebaseBackupProvider);
    if (!backup.ativo) {
      mostrarAviso(context, 'Firebase não configurado neste build (falta o env.json).');
      return;
    }

    setState(() {
      _buscando = true;
      _pesquisou = true;
      _todas = [];
      _placa = null;
    });

    // Um dia pode falhar sem derrubar os outros; os dias com erro são avisados no final.
    final resultados = await Future.wait(dias.map((dia) async {
      try {
        return (dia: dia, docs: await backup.coletasDoDia(dia), erro: false);
      } catch (e) {
        debugPrint('[Relatório] falha em ${_iso(dia)}: $e');
        return (dia: dia, docs: <Map<String, dynamic>>[], erro: true);
      }
    }));

    final linhas = <LinhaRelatorio>[];
    for (final r in resultados) {
      if (!r.erro) linhas.addAll(agregarRelatorio(r.docs, _iso(r.dia)));
    }
    final comErro = [for (final r in resultados) if (r.erro) formatarData(r.dia)];
    if (!mounted) return;
    setState(() {
      _todas = linhas;
      _buscando = false;
    });
    if (comErro.isNotEmpty) {
      mostrarAviso(context, 'Erro ao consultar os dias: ${comErro.join(', ')}');
    }
  }

  Widget _grade(LinhaRelatorio l) => Wrap(spacing: 6, runSpacing: 6, children: [
        for (final g in gruposColeta)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(color: g.cor, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.black12)),
            child: Column(children: [
              Text(g.nome, style: TextStyle(color: g.texto, fontSize: 11, fontWeight: FontWeight.bold)),
              Text(formatarPesoServidor(l.pesos[g.campo]), style: TextStyle(color: g.texto, fontSize: 12)),
            ]),
          ),
      ]);

  String _dataBr(String iso) => iso.length >= 10 ? '${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}' : iso;

  @override
  Widget build(BuildContext context) {
    final linhas = filtrarPorPlaca(_todas, _placa);
    final totais = totaisGerais(linhas);
    final placas = placasDoRelatorio(_todas);

    return Scaffold(
      appBar: AppBar(title: const Text('Relatório de coletas')),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          Cartao(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Período da consulta', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 12),
              CampoData(rotulo: 'Data inicial', valor: _inicial, aoMudar: (d) => setState(() => _inicial = d ?? _inicial)),
              const SizedBox(height: 12),
              CampoData(rotulo: 'Data final', valor: _final, aoMudar: (d) => setState(() => _final = d ?? _final)),
              const SizedBox(height: 12),
              FilledButton.icon(
                onPressed: _buscando ? null : _pesquisar,
                icon: const Icon(Icons.search),
                label: Text(_buscando ? 'Buscando...' : 'Pesquisar'),
              ),
              if (_buscando) const Padding(padding: EdgeInsets.only(top: 12), child: LinearProgressIndicator()),
            ]),
          ),
          if (!_buscando && _pesquisou && _todas.isEmpty)
            const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Nenhuma coleta no período.'))),
          if (_todas.isNotEmpty) ...[
            Cartao(
              child: DropdownButtonFormField<String?>(
                key: ValueKey(_placa),
                initialValue: _placa,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Filtrar por veículo', prefixIcon: Icon(Icons.local_shipping_outlined)),
                items: [
                  const DropdownMenuItem<String?>(value: null, child: Text('Todos')),
                  for (final p in placas) DropdownMenuItem<String?>(value: p, child: Text(p)),
                ],
                onChanged: (v) => setState(() => _placa = v),
              ),
            ),
            for (final l in linhas)
              Cartao(
                cor: l.totalDoDia ? const Color(0xFFEAF1FF) : null,
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Text(l.totalDoDia ? 'TOTAIS DO DIA ${_dataBr(l.data)}' : _dataBr(l.data),
                        style: const TextStyle(fontWeight: FontWeight.w600)),
                    const Spacer(),
                    if (!l.totalDoDia) Text(l.placa, style: const TextStyle(color: Colors.black54)),
                  ]),
                  const SizedBox(height: 8),
                  _grade(l),
                  const SizedBox(height: 6),
                  Text('Total: ${formatarPesoServidor(l.total)}', style: const TextStyle(fontWeight: FontWeight.bold)),
                ]),
              ),
            Cartao(
              cor: const Color(0xFFEAF8F0),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Totais gerais', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                for (final g in gruposColeta)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(children: [
                      Text(g.nome),
                      const Spacer(),
                      Text(formatarPesoServidor(totais.pesos[g.campo])),
                    ]),
                  ),
                const Divider(),
                Row(children: [
                  const Text('Total', style: TextStyle(fontWeight: FontWeight.bold)),
                  const Spacer(),
                  Text(formatarPesoServidor(totais.total), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                ]),
              ]),
            ),
          ],
        ],
      ),
    );
  }
}
