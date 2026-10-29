import '../core/conversao.dart';
import 'models.dart';

/// Linha do relatório: total de um veículo num dia, ou total do dia.
class LinhaRelatorio {
  LinhaRelatorio({required this.data, required this.placa, required this.totalDoDia}) : pesos = {for (final g in gruposColeta) g.campo: 0.0};

  final String data; // aaaa-mm-dd
  final String placa; // vazio no total do dia
  final bool totalDoDia;
  final Map<String, double> pesos;

  double get total => pesos.values.fold(0.0, (a, b) => a + b);

  void somar(Map<String, double> outros) {
    for (final e in outros.entries) {
      pesos[e.key] = (pesos[e.key] ?? 0) + e.value;
    }
  }
}

String placaDoDocumento(Map<String, dynamic> doc) {
  final v = doc['veiculo'];
  final placa = v is Map ? texto(v['placa']) : '';
  return placa.isEmpty ? 'Sem veículo' : placa;
}

/// Agrupa os documentos do Firestore por dia e veículo. [diaPadrao] (aaaa-mm-dd) vale para o
/// documento que não traz a data da coleta. Pesos ausentes ou inválidos contam como 0 ("1,5" = 1.5).
/// Cada dia termina com a linha de total do dia.
List<LinhaRelatorio> agregarRelatorio(List<Map<String, dynamic>> docs, String diaPadrao) {
  final porDia = <String, Map<String, LinhaRelatorio>>{};
  for (final doc in docs) {
    final str = doc['dataUltimaColetaString'];
    final data = str is String && str.length >= 10 ? str.substring(0, 10) : diaPadrao;
    final placa = placaDoDocumento(doc);
    final linha = porDia.putIfAbsent(data, () => {}).putIfAbsent(placa, () => LinhaRelatorio(data: data, placa: placa, totalDoDia: false));
    linha.somar({for (final g in gruposColeta) g.campo: numero(doc[g.campo]) ?? 0});
  }

  final saida = <LinhaRelatorio>[];
  final datas = porDia.keys.toList()..sort();
  for (final data in datas) {
    final totalDia = LinhaRelatorio(data: data, placa: '', totalDoDia: true);
    for (final linha in porDia[data]!.values) {
      saida.add(linha);
      totalDia.somar(linha.pesos);
    }
    saida.add(totalDia);
  }
  return saida;
}

/// Mantém só o veículo escolhido (null = todos) e recalcula o total do dia com o que sobrou.
List<LinhaRelatorio> filtrarPorPlaca(List<LinhaRelatorio> linhas, String? placa) {
  if (placa == null) return linhas;
  final saida = <LinhaRelatorio>[];
  for (final l in linhas) {
    if (l.totalDoDia || l.placa != placa) continue;
    saida.add(l);
    saida.add(LinhaRelatorio(data: l.data, placa: '', totalDoDia: true)..somar(l.pesos));
  }
  return saida;
}

/// Totais gerais: soma dos totais dos dias.
LinhaRelatorio totaisGerais(List<LinhaRelatorio> linhas) {
  final t = LinhaRelatorio(data: '', placa: '', totalDoDia: true);
  for (final l in linhas) {
    if (l.totalDoDia) t.somar(l.pesos);
  }
  return t;
}

/// Placas presentes, em ordem alfabética.
List<String> placasDoRelatorio(List<LinhaRelatorio> linhas) =>
    ({for (final l in linhas) if (!l.totalDoDia) l.placa}.toList()..sort());
