import 'dart:convert';

import 'package:sqflite/sqflite.dart';

import '../core/conversao.dart';
import '../core/formato.dart';
import 'db.dart';
import 'models.dart';

/// Pontos de coleta no SQLite. A lista de uma cidade é filtrada, ordenada e paginada
/// pelo banco: abrir "Pesquisar pontos" lê 30 linhas, não o catálogo inteiro.
class PontosRepo {
  PontosRepo(this._db);
  final AppDatabase _db;

  Database get _d => _db.db;

  Future<int> total() async => Sqflite.firstIntValue(await _d.rawQuery('SELECT COUNT(*) FROM pontos')) ?? 0;

  // ─── Consulta ──────────────────────────────────────────

  /// Cláusula WHERE dos pontos ativos de uma cidade. [coletados] true/false separa os
  /// já coletados dos a coletar; null devolve todos. "Coleta Única" fica coletada até o
  /// contrato ser renovado (o backend zera a data), não só no dia da coleta.
  ({String where, List<Object?> args}) _filtro({
    required int cidadeId,
    required DateTime hoje,
    bool? coletados,
    String nome = '',
    String endereco = '',
  }) {
    final where = StringBuffer('id_cidade = ? AND ativo = 1');
    final args = <Object?>[cidadeId];

    if (coletados != null) {
      const jaColetado = '(CASE WHEN coleta_unica = 1 THEN (dia > 0 AND mes > 0 AND ano > 0) '
          'ELSE (dia = ? AND mes = ? AND ano = ?) END)';
      where.write(coletados ? ' AND $jaColetado' : ' AND NOT $jaColetado');
      args.addAll([hoje.day, hoje.month, hoje.year]);
    }
    final n = normalizar(nome).trim();
    if (n.isNotEmpty) {
      where.write(r" AND busca_nome LIKE ? ESCAPE '\'");
      args.add('%${escaparLike(n)}%');
    }
    final e = normalizar(endereco).trim();
    if (e.isNotEmpty) {
      where.write(r" AND busca_endereco LIKE ? ESCAPE '\'");
      args.add('%${escaparLike(e)}%');
    }
    return (where: where.toString(), args: args);
  }

  Future<int> contar({
    required int cidadeId,
    required DateTime hoje,
    bool? coletados,
    String nome = '',
    String endereco = '',
  }) async {
    final f = _filtro(cidadeId: cidadeId, hoje: hoje, coletados: coletados, nome: nome, endereco: endereco);
    return Sqflite.firstIntValue(await _d.rawQuery('SELECT COUNT(*) FROM pontos WHERE ${f.where}', f.args)) ?? 0;
  }

  Future<List<Ponto>> pagina({
    required int cidadeId,
    required DateTime hoje,
    bool? coletados,
    String nome = '',
    String endereco = '',
    required int limite,
    required int offset,
  }) async {
    final f = _filtro(cidadeId: cidadeId, hoje: hoje, coletados: coletados, nome: nome, endereco: endereco);
    final r = await _d.rawQuery(
      'SELECT json FROM pontos WHERE ${f.where} ORDER BY seq LIMIT ? OFFSET ?',
      [...f.args, limite, offset],
    );
    return [for (final linha in r) Ponto.deJson(linha['json'] as String)];
  }

  /// Pontos da cidade com aviso (para o alerta geral ao abrir a lista).
  Future<List<Ponto>> comAviso(int cidadeId) async {
    final r = await _d.rawQuery(
      'SELECT json FROM pontos WHERE id_cidade = ? AND ativo = 1 AND aviso = 1 ORDER BY seq',
      [cidadeId],
    );
    return [for (final linha in r) Ponto.deJson(linha['json'] as String)];
  }

  /// Todos os pontos com aviso, de qualquer cidade (tela "Pontos com avisos").
  Future<List<Ponto>> todosComAviso() async {
    final r = await _d.rawQuery('SELECT json FROM pontos WHERE aviso = 1 ORDER BY busca_nome');
    return [for (final linha in r) Ponto.deJson(linha['json'] as String)];
  }

  // ─── Escrita ───────────────────────────────────────────

  Map<String, Object?>? _colunas(Map<String, dynamic> p) {
    final id = inteiro(p['id']);
    if (id <= 0) return null;
    final cidade = p['cidade'];
    final idCidade = p['idCidade'] ?? (cidade is Map ? cidade['id'] : null);
    final inativo = zeradoOuFalso(p['status']) || zeradoOuFalso(p['contratoAtivo']);
    return {
      'id': id,
      'id_cidade': idCidade == null ? null : inteiro(idCidade),
      'ativo': inativo ? 0 : 1,
      'coleta_unica': texto(p['periodicidadeColeta']) == 'Coleta Única' ? 1 : 0,
      'aviso': verdadeiro(p['situacaoAviso']) ? 1 : 0,
      'dia': inteiro(p['diaUltimaColeta']),
      'mes': inteiro(p['mesUltimaColeta']),
      'ano': inteiro(p['anoUltimaColeta']),
      'busca_nome': normalizar([p['nomeRazaoSocial'], p['nomeFantasia'], p['apelido']].map(texto).join('\n')),
      'busca_endereco': normalizar(p['enderecoCompleto']),
      'json': jsonEncode(p),
    };
  }

  /// Aplica o que o servidor mandou, numa transação.
  ///
  /// [completo] (primeira carga ou "sincronizar tudo"): a resposta é a lista inteira, então
  /// a ordem do servidor vira a ordem da tela e pontos que sumiram são removidos.
  /// Incremental: só atualiza/insere o que veio (pontos novos só entram se o objeto tiver
  /// cidade e grupos, para não aparecer um ponto pela metade).
  ///
  /// Pontos com coleta local ainda não enviada ficam como estão: a data da última coleta
  /// foi atualizada na hora no aparelho e um retrato do servidor que ainda não processou
  /// essa coleta a desfaria.
  Future<void> mesclar(List<dynamic> recebidos, {required bool completo}) async {
    await _d.transaction((txn) async {
      final protegidos = {
        for (final r in await txn.rawQuery('SELECT DISTINCT ponto_id FROM coletas WHERE sincronizado = 0'))
          r['ponto_id'] as int,
      };
      final existentes = {
        for (final r in await txn.rawQuery('SELECT id FROM pontos')) r['id'] as int,
      };
      // Resposta vazia numa carga completa é quase certamente falha do servidor, não "não há
      // mais pontos": não apaga a base local por causa disso.
      if (completo && recebidos.isEmpty && existentes.isNotEmpty) return;
      var proximaSeq =
          (Sqflite.firstIntValue(await txn.rawQuery('SELECT MAX(seq) FROM pontos')) ?? 0) + 1;

      final vistos = <int>{};
      final b = txn.batch();
      var posicao = 0;
      for (final item in recebidos) {
        posicao++;
        if (item is! Map) continue;
        final p = Map<String, dynamic>.from(item);
        final cols = _colunas(p);
        if (cols == null) continue;
        final id = cols['id'] as int;
        vistos.add(id);

        if (existentes.contains(id)) {
          if (protegidos.contains(id)) {
            if (completo) b.update('pontos', {'seq': posicao}, where: 'id = ?', whereArgs: [id]);
            continue;
          }
          final atualizacao = Map<String, Object?>.from(cols)..remove('id');
          if (atualizacao['id_cidade'] == null) atualizacao.remove('id_cidade');
          if (completo) atualizacao['seq'] = posicao;
          b.update('pontos', atualizacao, where: 'id = ?', whereArgs: [id]);
        } else {
          if (!completo && (cols['id_cidade'] == null || texto(p['gruposColetados']).isEmpty)) continue;
          b.insert('pontos', {...cols, 'seq': completo ? posicao : proximaSeq++});
        }
      }

      if (completo) {
        for (final id in existentes.difference(vistos).difference(protegidos)) {
          b.delete('pontos', where: 'id = ?', whereArgs: [id]);
        }
      }
      await b.commit(noResult: true);
    });
  }

  /// Registra no ponto a coleta recém-feita (dia/mês/ano e contador do mês), dentro da
  /// transação que grava a própria coleta.
  Future<void> registrarColeta(DatabaseExecutor txn, int pontoId, DateTime quando) =>
      _reescrever(txn, pontoId, (p) => p.comUltimaColeta(quando));

  /// Depois de excluir uma coleta: a "última coleta" do ponto volta à mais recente que
  /// sobrou (ou zera, e o ponto volta para "a coletar"). [decrementarMes] desfaz o +1 do
  /// contador quando a coleta excluída é do mês corrente.
  Future<void> recalcularUltimaColeta(DatabaseExecutor txn, int pontoId, {required bool decrementarMes}) async {
    final r = await txn.rawQuery(
      'SELECT instante FROM coletas WHERE ponto_id = ? AND status = 1 ORDER BY instante DESC LIMIT 1',
      [pontoId],
    );
    await _reescrever(txn, pontoId, (p) {
      final delta = decrementarMes ? -1 : 0;
      if (r.isEmpty) {
        final m = Map<String, dynamic>.from(p.raw)
          ..['diaUltimaColeta'] = 0
          ..['mesUltimaColeta'] = 0
          ..['anoUltimaColeta'] = 0
          ..['quantidadeColetadaMes'] = (p.quantidadeColetadaMes + delta).clamp(0, 1 << 30);
        return Ponto(m);
      }
      return p.comUltimaColeta(DateTime.fromMillisecondsSinceEpoch(r.first['instante'] as int), delta: delta);
    });
  }

  Future<void> _reescrever(DatabaseExecutor txn, int pontoId, Ponto Function(Ponto) alterar) async {
    final r = await txn.query('pontos', columns: ['json'], where: 'id = ?', whereArgs: [pontoId], limit: 1);
    if (r.isEmpty) return;
    final novo = alterar(Ponto.deJson(r.first['json'] as String));
    await txn.update(
      'pontos',
      {
        'dia': inteiro(novo.raw['diaUltimaColeta']),
        'mes': inteiro(novo.raw['mesUltimaColeta']),
        'ano': inteiro(novo.raw['anoUltimaColeta']),
        'json': jsonEncode(novo.raw),
      },
      where: 'id = ?',
      whereArgs: [pontoId],
    );
  }
}
