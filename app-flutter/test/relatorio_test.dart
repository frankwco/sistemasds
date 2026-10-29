import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/data/relatorio.dart';

Map<String, dynamic> _doc(String placa, num a, {Object? e, String? data}) => {
      'veiculo': {'placa': placa},
      'a': a,
      'e': e,
      if (data != null) 'dataUltimaColetaString': data,
    };

void main() {
  test('agrupa por dia e veículo e fecha cada dia com o total', () {
    final linhas = agregarRelatorio([
      _doc('AAA1', 1.5, data: '2026-10-27 10:00:00'),
      _doc('AAA1', 2, e: '0,5', data: '2026-10-27 11:00:00'),
      _doc('BBB2', 4, data: '2026-10-27 12:00:00'),
    ], '2026-10-27');

    expect(linhas.map((l) => l.totalDoDia ? 'DIA' : l.placa), ['AAA1', 'BBB2', 'DIA']);
    expect(linhas[0].pesos['a'], 3.5);
    expect(linhas[0].pesos['e'], 0.5);
    expect(linhas[0].total, 4.0);
    expect(linhas.last.total, 8.0);
  });

  test('documento sem data usa o dia da consulta e sem veículo vira "Sem veículo"', () {
    final linhas = agregarRelatorio([
      {'a': 1}
    ], '2026-10-20');
    expect(linhas.first.data, '2026-10-20');
    expect(linhas.first.placa, 'Sem veículo');
  });

  test('pesos inválidos contam como zero', () {
    final linhas = agregarRelatorio([_doc('X', 1, e: 'abc')], '2026-10-27');
    expect(linhas.first.pesos['e'], 0);
    expect(linhas.first.total, 1);
  });

  test('filtro por placa recalcula o total do dia e os totais gerais', () {
    final todas = agregarRelatorio([_doc('A', 1), _doc('B', 2)], '2026-10-27');
    expect(placasDoRelatorio(todas), ['A', 'B']);
    final soB = filtrarPorPlaca(todas, 'B');
    expect(soB.length, 2);
    expect(totaisGerais(soB).total, 2);
    expect(totaisGerais(todas).total, 3);
    expect(filtrarPorPlaca(todas, null), todas);
  });
}
