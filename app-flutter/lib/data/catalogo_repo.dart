import 'dart:convert';

import 'package:sqflite/sqflite.dart';

import '../core/conversao.dart';
import 'db.dart';
import 'models.dart';

/// Cidades, veículos e motoristas: listas pequenas que o servidor devolve inteiras.
class CatalogoRepo {
  CatalogoRepo(this._db);
  final AppDatabase _db;

  static const cidades = 'cidades';
  static const veiculos = 'veiculos';
  static const motoristas = 'motoristas';

  static const _campoNome = {cidades: 'nome', veiculos: 'descricao', motoristas: 'nome'};

  /// Troca a lista do [tipo] pela que o servidor mandou, numa transação só.
  Future<void> substituir(String tipo, List<dynamic> itens) async {
    final campo = _campoNome[tipo]!;
    await _db.db.transaction((txn) async {
      await txn.delete('catalogo', where: 'tipo = ?', whereArgs: [tipo]);
      final b = txn.batch();
      for (final item in itens) {
        if (item is! Map) continue;
        final m = Map<String, dynamic>.from(item);
        b.insert(
          'catalogo',
          {'tipo': tipo, 'id': inteiro(m['id']), 'nome': texto(m[campo]), 'json': jsonEncode(m)},
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      await b.commit(noResult: true);
    });
  }

  Future<List<String>> _jsons(String tipo) async {
    final r = await _db.db.query('catalogo',
        columns: ['json'], where: 'tipo = ?', whereArgs: [tipo], orderBy: 'nome COLLATE NOCASE');
    return [for (final linha in r) linha['json'] as String];
  }

  Future<List<Cidade>> listarCidades() async => [for (final j in await _jsons(cidades)) Cidade.deJson(j)];
  Future<List<Veiculo>> listarVeiculos() async => [for (final j in await _jsons(veiculos)) Veiculo.deJson(j)];
  Future<List<Motorista>> listarMotoristas() async =>
      [for (final j in await _jsons(motoristas)) Motorista.deJson(j)];

  Future<bool> vazio(String tipo) async {
    final n = Sqflite.firstIntValue(
        await _db.db.rawQuery('SELECT COUNT(*) FROM catalogo WHERE tipo = ?', [tipo]));
    return (n ?? 0) == 0;
  }
}
