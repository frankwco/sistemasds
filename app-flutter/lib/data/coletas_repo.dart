import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:sqflite/sqflite.dart';

import '../core/config.dart';
import '../core/conversao.dart';
import '../core/formato.dart';
import 'coleta_item.dart';
import 'coleta_payload.dart';
import 'db.dart';
import 'pontos_repo.dart';

/// Coleta na caixa de saída, pronta para enviar.
class ColetaPendente {
  ColetaPendente({required this.id, required this.json, required this.ativa, this.arquivoAssinatura});

  final String id;
  final Map<String, dynamic> json;

  /// false = exclusão que ainda precisa ser confirmada no servidor.
  final bool ativa;
  final String? arquivoAssinatura;
}

/// Coleta cujo backup no Firebase está pendente: [fb] 1 = gravar a coleta, 2 = marcar a exclusão.
class ItemEspelho {
  ItemEspelho({
    required this.id,
    required this.pontoId,
    required this.instante,
    required this.fb,
    required this.json,
    this.arquivoAssinatura,
  });

  final String id;
  final int pontoId;
  final int instante;
  final int fb;
  final Map<String, dynamic> json;
  final String? arquivoAssinatura;
}

/// Coletas locais: as feitas neste aparelho (caixa de saída) e as que o servidor devolve
/// na "lista". Uma linha por coleta; a exclusão é lógica (status = 0) até o servidor confirmar.
class ColetasRepo {
  ColetasRepo(this._db, this._pontos);

  final AppDatabase _db;
  final PontosRepo _pontos;

  Database get _d => _db.db;

  /// Muda a cada alteração nas coletas (gravar, excluir, enviar, falhar, mesclar), para as
  /// telas abertas se atualizarem sozinhas em vez de mostrarem o estado de quando abriram.
  final ValueNotifier<int> mudou = ValueNotifier(0);
  void avisarMudanca() => mudou.value++;

  // ─── Gravação ──────────────────────────────────────────

  /// Chamada dentro da transação que também atualiza o ponto.
  Future<void> inserir(
    DatabaseExecutor txn, {
    required String id,
    required int pontoId,
    required String nomePonto,
    required int instante,
    required Map<String, dynamic> payload,
    required String arquivoAssinatura,
    bool espelhar = false,
  }) =>
      txn.insert('coletas', {
        'id': id,
        'ponto_id': pontoId,
        'busca': normalizar(nomePonto),
        'status': 1,
        'sincronizado': 0,
        'instante': instante,
        'assinatura': arquivoAssinatura,
        'fb': espelhar ? 1 : 0,
        'json': jsonEncode(payload),
      });

  /// Exclusão lógica: some da lista na hora, o envio ao servidor vem na sequência, e o
  /// ponto volta a ficar "a coletar" se não sobrou outra coleta dele.
  /// Devolve ponto e dia da coleta excluída (para o backup no Firebase), ou null se não existe.
  /// [espelhar]: também falta marcar a exclusão no backup do Firebase (fb = 2).
  Future<({int pontoId, int dia, int mes, int ano})?> excluir(String id, {bool espelhar = false}) async {
    return _d.transaction((txn) async {
      final r = await txn.query('coletas',
          columns: ['ponto_id', 'instante'], where: 'id = ?', whereArgs: [id], limit: 1);
      if (r.isEmpty) return null;
      await txn.update('coletas', {'status': 0, 'sincronizado': 0, if (espelhar) 'fb': 2},
          where: 'id = ?', whereArgs: [id]);

      final quando = DateTime.fromMillisecondsSinceEpoch(r.first['instante'] as int);
      final agora = DateTime.now();
      await _pontos.recalcularUltimaColeta(
        txn,
        r.first['ponto_id'] as int,
        decrementarMes: quando.year == agora.year && quando.month == agora.month,
      );
      return (pontoId: r.first['ponto_id'] as int, dia: quando.day, mes: quando.month, ano: quando.year);
    }).whenComplete(avisarMudanca);
  }

  // ─── Envio ─────────────────────────────────────────────

  Future<int> contarPendentes() async =>
      Sqflite.firstIntValue(await _d.rawQuery('SELECT COUNT(*) FROM coletas WHERE sincronizado = 0')) ?? 0;

  Future<List<ColetaPendente>> pendentes() async {
    final r = await _d.query('coletas',
        columns: ['id', 'json', 'status', 'assinatura'], where: 'sincronizado = 0', orderBy: 'instante');
    return [
      for (final l in r)
        ColetaPendente(
          id: l['id'] as String,
          json: Map<String, dynamic>.from(jsonDecode(l['json'] as String) as Map),
          ativa: l['status'] == 1,
          arquivoAssinatura: l['assinatura'] as String?,
        ),
    ];
  }

  /// Reserva a coleta para envio por [duracao]. Devolve false se outro envio (o do app ou
  /// o da tarefa em segundo plano) já está com ela. A reserva expira sozinha, então um
  /// processo morto no meio do envio não trava a coleta.
  Future<bool> reservarEnvio(String id, {Duration duracao = const Duration(seconds: 100)}) async {
    final agora = DateTime.now().millisecondsSinceEpoch;
    final n = await _d.rawUpdate(
      'UPDATE coletas SET enviando_ate = ? WHERE id = ? AND sincronizado = 0 AND (enviando_ate IS NULL OR enviando_ate < ?)',
      [agora + duracao.inMilliseconds, id, agora],
    );
    return n == 1;
  }

  /// O servidor confirmou: marca como enviada e apaga a assinatura do aparelho.
  ///
  /// Só vale se o estado da coleta (ativa/excluída) ainda é o que foi enviado. Se ela foi excluída
  /// enquanto o envio estava no ar, o servidor recebeu a versão ativa: a coleta continua pendente
  /// (agora como exclusão) e devolve false para quem chamou enviar de novo logo em seguida.
  Future<bool> marcarEnviada(ColetaPendente c) async {
    final n = await _d.rawUpdate(
      // A assinatura só sai do aparelho se o backup do Firebase também já está em dia (fb = 0).
      'UPDATE coletas SET sincronizado = 1, ultimo_erro = NULL, enviando_ate = NULL, '
      'assinatura = CASE WHEN fb = 0 THEN NULL ELSE assinatura END '
      'WHERE id = ? AND status = ?',
      [c.id, c.ativa ? 1 : 0],
    );
    if (n == 0) {
      await _d.rawUpdate('UPDATE coletas SET enviando_ate = NULL WHERE id = ?', [c.id]);
      avisarMudanca();
      return false;
    }
    avisarMudanca();
    final fb = Sqflite.firstIntValue(await _d.rawQuery('SELECT fb FROM coletas WHERE id = ?', [c.id])) ?? 0;
    if (fb == 0) _apagarArquivo(c.arquivoAssinatura);
    return true;
  }

  void _apagarArquivo(String? arquivo) {
    if (arquivo == null) return;
    try {
      final f = File(arquivo);
      if (f.existsSync()) f.deleteSync();
    } catch (_) {
      // Arquivo órfão não atrapalha; fica para a limpeza de órfãos.
    }
  }

  // ─── Espelho no Firebase ───────────────────────────────

  Future<List<ItemEspelho>> pendentesEspelho({int limite = 20}) async {
    final r = await _d.query('coletas',
        columns: ['id', 'ponto_id', 'instante', 'fb', 'assinatura', 'json'],
        where: 'fb > 0',
        orderBy: 'instante',
        limit: limite);
    return [
      for (final l in r)
        ItemEspelho(
          id: l['id'] as String,
          pontoId: l['ponto_id'] as int,
          instante: l['instante'] as int,
          fb: l['fb'] as int,
          arquivoAssinatura: l['assinatura'] as String?,
          json: Map<String, dynamic>.from(jsonDecode(l['json'] as String) as Map),
        ),
    ];
  }

  Future<int> contarEspelhoPendente() async =>
      Sqflite.firstIntValue(await _d.rawQuery('SELECT COUNT(*) FROM coletas WHERE fb > 0')) ?? 0;

  /// O Firebase confirmou. Só vale se o que faltava ainda é o mesmo ([fbEsperado]): se a coleta foi
  /// excluída enquanto a gravação estava no ar (1 -> 2), continua pendente para gravar a exclusão.
  /// Quando servidor e backup estão em dia, a assinatura sai do aparelho.
  Future<void> espelhoConfirmado(String id, int fbEsperado) async {
    final n = await _d.rawUpdate('UPDATE coletas SET fb = 0 WHERE id = ? AND fb = ?', [id, fbEsperado]);
    if (n == 0) return;
    final r = await _d.query('coletas', columns: ['sincronizado', 'assinatura'], where: 'id = ?', whereArgs: [id]);
    if (r.isNotEmpty && r.first['sincronizado'] == 1 && r.first['assinatura'] != null) {
      await _d.update('coletas', {'assinatura': null}, where: 'id = ?', whereArgs: [id]);
      _apagarArquivo(r.first['assinatura'] as String);
    }
    avisarMudanca();
  }

  /// Guarda por que o envio falhou (aparece na lista de coletas) e conta a tentativa.
  Future<void> registrarFalha(String id, String erro) async {
    await _d.rawUpdate('UPDATE coletas SET tentativas = tentativas + 1, ultimo_erro = ?, enviando_ate = NULL WHERE id = ?', [erro, id]);
    avisarMudanca();
  }

  // ─── Retorno do servidor ───────────────────────────────

  /// Junta a resposta "lista" do servidor com as coletas locais.
  /// - Coleta local pendente ou excluída localmente NÃO é sobrescrita pelo servidor (ele
  ///   pode ainda não ter processado a pendência e devolver o estado antigo).
  /// - Coleta local que o servidor não devolveu continua guardada: "ausente da lista" não
  ///   prova que o servidor a processou.
  /// - Coleta enviada e já velha sai do aparelho (o servidor guarda o histórico).
  Future<void> mesclarDoServidor(List<dynamic> itens) async {
    await _d.transaction((txn) async {
      final bloqueados = {
        for (final r in await txn.rawQuery('SELECT id FROM coletas WHERE sincronizado = 0 OR status = 0'))
          r['id'] as String,
      };
      final b = txn.batch();
      for (final item in itens) {
        if (item is! Map) continue;
        final json = Map<String, dynamic>.from(item);
        final id = texto(json['identificadorApp']);
        if (id.isEmpty || bloqueados.contains(id)) continue;
        json.remove('assinatura'); // base64 pesado: nunca guardar o que veio do servidor
        final ponto = json['pontoColetaSelecionado'];
        final pontoMap = ponto is Map ? Map<String, dynamic>.from(ponto) : <String, dynamic>{};
        b.insert(
          'coletas',
          {
            'id': id,
            'ponto_id': inteiro(pontoMap['id']),
            'busca': normalizar(pontoMap['nomeRazaoSocial']),
            'status': verdadeiro(json['status']) ? 1 : 0,
            'sincronizado': 1,
            'instante': instanteDaColeta(json),
            'assinatura': null,
            'json': jsonEncode(json),
          },
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
      final limite = DateTime.now().subtract(Config.retencaoColetasEnviadas).millisecondsSinceEpoch;
      b.delete('coletas', where: 'sincronizado = 1 AND instante < ?', whereArgs: [limite]);
      await b.commit(noResult: true);
    });
    avisarMudanca();
  }

  // ─── Consulta ──────────────────────────────────────────

  ({String where, List<Object?> args}) _filtroLista(String busca) {
    final n = normalizar(busca).trim();
    if (n.isEmpty) return (where: 'status = 1', args: <Object?>[]);
    return (where: r"status = 1 AND busca LIKE ? ESCAPE '\'", args: <Object?>['%${escaparLike(n)}%']);
  }

  Future<int> contar(String busca) async {
    final f = _filtroLista(busca);
    return Sqflite.firstIntValue(await _d.rawQuery('SELECT COUNT(*) FROM coletas WHERE ${f.where}', f.args)) ?? 0;
  }

  /// Coletas ativas, da mais recente para a mais antiga.
  Future<List<ColetaItem>> pagina({String busca = '', required int limite, required int offset}) async {
    final f = _filtroLista(busca);
    final r = await _d.rawQuery(
      'SELECT id, json, sincronizado, tentativas, ultimo_erro FROM coletas WHERE ${f.where} ORDER BY instante DESC LIMIT ? OFFSET ?',
      [...f.args, limite, offset],
    );
    return [for (final l in r) ColetaItem.deLinha(l)];
  }

  /// Apaga arquivos de assinatura que não pertencem a nenhuma coleta pendente
  /// (sobras de uma gravação interrompida).
  Future<void> limparAssinaturasOrfas() async {
    final emUso = {
      for (final r in await _d.rawQuery('SELECT assinatura FROM coletas WHERE assinatura IS NOT NULL'))
        r['assinatura'] as String,
    };
    for (final arquivo in _db.pastaAssinaturas.listSync().whereType<File>()) {
      if (!emUso.contains(arquivo.path)) {
        try {
          arquivo.deleteSync();
        } catch (_) {}
      }
    }
  }
}
