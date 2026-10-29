import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/data/ia_logic.dart';

void main() {
  test('combustível: casa com as opções fixas', () {
    expect(normalizarCombustivel('diesel s10'), 'Diesel S10');
    expect(normalizarCombustivel('Gasolina comum'), 'Gasolina');
    expect(normalizarCombustivel('óleo diesel'), 'Diesel S10');
    expect(normalizarCombustivel('querosene'), isNull);
    expect(normalizarCombustivel(null), isNull);
  });

  test('placa normalizada', () {
    expect(normalizarPlaca('BCW-3C49'), 'bcw3c49');
    expect(normalizarPlaca('bcw 3c49'), 'bcw3c49');
    expect(normalizarPlaca(null), '');
  });

  test('abastecimento: calcula o terceiro valor', () {
    final a = recalcularAbastecimento(litros: 40, valorLitro: 5.5);
    expect(a.valor, 220.0);
    final b = recalcularAbastecimento(litros: 40, valor: 220);
    expect(b.valorLitro, 5.5);
    final c = recalcularAbastecimento(litros: 0, valorLitro: 5.5);
    expect(c.valor, isNull);
    final d = recalcularAbastecimento(litros: 40, valorLitro: 5.5, valor: 100);
    expect(d.valor, 100); // não sobrescreve o que já veio
  });

  test('casar veículo por placa parcial ou descrição', () {
    final v = [
      {'id': 1, 'placa': 'BCW-3C49', 'descricao': 'Caminhão Baú'},
      {'id': 2, 'placa': 'XYZ-1A23', 'descricao': 'Fiorino'},
    ];
    expect(casarVeiculo(v, 'bcw 3c')!['id'], 1);
    expect(casarVeiculo(v, 'fiorino')!['id'], 2);
    expect(casarVeiculo(v, 'moto'), isNull);
  });

  test('casar fornecedor/motorista por nome, ignorando acento e caixa', () {
    final f = [
      {'id': 1, 'nome': 'Posto São João'},
      {'id': 2, 'nome': 'Auto Peças Silva'},
    ];
    expect(casarPorNome(f, 'POSTO SAO JOAO LTDA')!['id'], 1);
    expect(casarPorNome(f, 'silva')!['id'], 2);
    expect(casarPorNome(f, 'nada'), isNull);
    expect(casarPorNome(f, ''), isNull);
  });

  test('categoria só pelo nome exato', () {
    final c = [
      {'id': 1, 'nome': 'Pedágio'},
    ];
    expect(casarCategoria(c, 'pedágio')!['id'], 1);
    expect(casarCategoria(c, 'ped'), isNull);
  });
}
