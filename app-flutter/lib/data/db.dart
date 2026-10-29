import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqflite/sqflite.dart';

/// Banco local (SQLite) no lugar do Ionic Storage.
///
/// O app antigo guardava `coletas` e `pontosColeta` como dois arrays JSON gigantes e
/// regravava o array inteiro a cada alteração (daí a fila de gravação, as pausas de
/// sincronização e a lentidão em celular fraco). Aqui cada coleta e cada ponto é uma
/// linha: gravar uma coleta mexe numa linha, listar pontos pagina e filtra no banco,
/// e uma transação garante que coleta + atualização do ponto entram juntas ou nenhuma.
class AppDatabase {
  AppDatabase._(this.db, this.pastaAssinaturas);

  /// Para testes: usa um banco e uma pasta já abertos (sem path_provider).
  AppDatabase.comBanco(this.db, this.pastaAssinaturas);

  final Database db;

  /// Assinaturas (PNG) ficam em arquivo, não no banco: não incham as consultas e saem
  /// do aparelho assim que a coleta é confirmada pelo servidor.
  final Directory pastaAssinaturas;

  static const versaoEsquema = 4;
  static const _versao = versaoEsquema;

  static Future<AppDatabase> abrir() async {
    final pastaBanco = await getDatabasesPath();
    final db = await openDatabase(
      p.join(pastaBanco, 'gestaofacil.db'),
      version: _versao,
      onCreate: criarEsquema,
      onUpgrade: atualizarEsquema,
    );
    final docs = await getApplicationDocumentsDirectory();
    final pasta = Directory(p.join(docs.path, 'assinaturas'));
    if (!pasta.existsSync()) {
      pasta.createSync(recursive: true);
    }
    return AppDatabase._(db, pasta);
  }

  static Future<void> criarEsquema(Database db, int versao) async {
    final b = db.batch();

    // Sessão e preferências: chave -> valor.
    b.execute('CREATE TABLE kv (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)');

    // Cidades, veículos e motoristas (listas pequenas, trocadas inteiras a cada sync).
    b.execute('''
      CREATE TABLE catalogo (
        tipo TEXT NOT NULL,
        id INTEGER NOT NULL,
        nome TEXT NOT NULL,
        json TEXT NOT NULL,
        PRIMARY KEY (tipo, id)
      )''');

    // Pontos de coleta: colunas só para o que filtra/ordena; o resto fica no `json`.
    b.execute('''
      CREATE TABLE pontos (
        id INTEGER PRIMARY KEY,
        seq INTEGER NOT NULL,
        id_cidade INTEGER,
        ativo INTEGER NOT NULL,
        coleta_unica INTEGER NOT NULL,
        aviso INTEGER NOT NULL DEFAULT 0,
        dia INTEGER NOT NULL DEFAULT 0,
        mes INTEGER NOT NULL DEFAULT 0,
        ano INTEGER NOT NULL DEFAULT 0,
        busca_nome TEXT NOT NULL,
        busca_endereco TEXT NOT NULL,
        json TEXT NOT NULL
      )''');
    b.execute('CREATE INDEX idx_pontos_cidade ON pontos (id_cidade, ativo, seq)');

    // Coletas: a mesma tabela serve de caixa de saída (sincronizado = 0) e de histórico
    // recente. `id` é o identificadorApp, que torna o reenvio idempotente no servidor.
    b.execute('''
      CREATE TABLE coletas (
        id TEXT PRIMARY KEY,
        ponto_id INTEGER NOT NULL,
        busca TEXT NOT NULL,
        status INTEGER NOT NULL,
        sincronizado INTEGER NOT NULL,
        instante INTEGER NOT NULL,
        assinatura TEXT,
        tentativas INTEGER NOT NULL DEFAULT 0,
        ultimo_erro TEXT,
        enviando_ate INTEGER,
        fb INTEGER NOT NULL DEFAULT 0,
        json TEXT NOT NULL
      )''');
    b.execute('CREATE INDEX idx_coletas_pendentes ON coletas (sincronizado)');
    b.execute('CREATE INDEX idx_coletas_lista ON coletas (status, instante DESC)');
    b.execute('CREATE INDEX idx_coletas_ponto ON coletas (ponto_id, status, instante DESC)');

    await b.commit(noResult: true);
  }

  static Future<void> atualizarEsquema(Database db, int de, int para) async {
    if (de < 2) {
      // Diagnóstico de envio: quantas vezes falhou e por quê.
      await db.execute('ALTER TABLE coletas ADD COLUMN tentativas INTEGER NOT NULL DEFAULT 0');
      await db.execute('ALTER TABLE coletas ADD COLUMN ultimo_erro TEXT');
    }
    if (de < 3) {
      // Reserva de envio: o app e a tarefa em segundo plano não mandam a mesma coleta juntos.
      await db.execute('ALTER TABLE coletas ADD COLUMN enviando_ate INTEGER');
    }
    if (de < 4) {
      // Backup no Firebase com garantia: 0 = em dia, 1 = falta gravar a coleta, 2 = falta gravar a exclusão.
      await db.execute('ALTER TABLE coletas ADD COLUMN fb INTEGER NOT NULL DEFAULT 0');
    }
  }

  // ─── kv ────────────────────────────────────────────────

  Future<String?> lerKv(String chave) async {
    final r = await db.query('kv', columns: ['valor'], where: 'chave = ?', whereArgs: [chave], limit: 1);
    return r.isEmpty ? null : r.first['valor'] as String;
  }

  Future<void> gravarKv(String chave, String valor) =>
      db.insert('kv', {'chave': chave, 'valor': valor}, conflictAlgorithm: ConflictAlgorithm.replace);

  Future<void> removerKv(String chave) => db.delete('kv', where: 'chave = ?', whereArgs: [chave]);
}
