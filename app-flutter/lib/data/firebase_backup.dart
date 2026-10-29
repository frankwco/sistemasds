import 'dart:convert';
import 'dart:typed_data';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/foundation.dart';

/// Cópia de segurança das coletas no Firestore, como o app Ionic já fazia:
/// `coleta/veiculo/<d-m-aaaa>/<idPonto>`. A exclusão é lógica (`status: false`) para o
/// serviço de reconciliação do backend (consumirFirebase) enxergar a remoção.
///
/// Nunca atrapalha a coleta: qualquer falha (sem internet, regra, login) só vai ao log e devolve
/// `false`; quem chama (EspelhoFirebase) mantém a coleta marcada como "backup pendente" e tenta
/// de novo. Sem internet, o próprio Firestore também guarda a escrita e a envia quando voltar.
class FirebaseBackup implements BackupRemoto {
  FirebaseBackup._(this._ativo);

  /// Desligado (ex.: Firebase não inicializou): todas as operações viram no-op.
  FirebaseBackup.desligado() : _ativo = false;

  final bool _ativo;

  // Mesmo projeto do app Ionic (environment.ts). Valores sobrescrevíveis no build.
  static const _opcoes = FirebaseOptions(
    apiKey: String.fromEnvironment('GF_FB_API_KEY', defaultValue: 'AIzaSyACkVUvBjgnyUBrBMst0_fwekfC-PW_biA'),
    appId: String.fromEnvironment('GF_FB_APP_ID', defaultValue: '1:613904238647:web:d0e45391b9d31494b3d7b2'),
    messagingSenderId: '613904238647',
    projectId: 'gestaofacil-a6251',
    storageBucket: 'gestaofacil-a6251.appspot.com',
    authDomain: 'gestaofacil-a6251.firebaseapp.com',
  );

  // Usuário técnico do Firebase (o Firestore exige login). Não fica no código: vem do
  // build, via `--dart-define-from-file=env.json` (arquivo local, fora do Git; ver README).
  static const _email = String.fromEnvironment('GF_FB_EMAIL');
  static const _senha = String.fromEnvironment('GF_FB_SENHA');

  static Future<FirebaseBackup> iniciar() async {
    if (_email.isEmpty || _senha.isEmpty) {
      debugPrint('[Firebase] GF_FB_EMAIL/GF_FB_SENHA não informados no build: backup desligado.');
      return FirebaseBackup.desligado();
    }
    try {
      await Firebase.initializeApp(options: _opcoes);
      return FirebaseBackup._(true);
    } catch (e) {
      debugPrint('[Firebase] não inicializou, backup desligado: $e');
      return FirebaseBackup.desligado();
    }
  }

  Future<void> _garantirLogin() async {
    final auth = FirebaseAuth.instance;
    if (auth.currentUser == null) {
      await auth.signInWithEmailAndPassword(email: _email, password: _senha);
    }
  }

  CollectionReference<Map<String, dynamic>> _dia(int dia, int mes, int ano) => FirebaseFirestore.instance
      .collection('coleta')
      .doc('veiculo')
      .collection('$dia-$mes-$ano');

  /// Tempo máximo esperando o servidor confirmar uma escrita (sem internet ela fica na fila local
  /// do Firestore e só confirma quando a conexão volta).
  static const _esperaConfirmacao = Duration(seconds: 15);

  /// Grava a coleta (com a assinatura em data URL, como antes). `true` = o Firestore confirmou.
  @override
  Future<bool> gravarColeta(Map<String, dynamic> payload, Uint8List assinaturaPng) async {
    if (!_ativo) return false;
    try {
      await _garantirLogin();
      final ponto = payload['pontoColetaSelecionado'] as Map;
      final doc = Map<String, dynamic>.from(payload)
        ..['assinatura'] = 'data:image/png;base64,${base64Encode(assinaturaPng)}';
      await _dia(payload['diaUltimaColeta'] as int, payload['mesUltimaColeta'] as int, payload['anoUltimaColeta'] as int)
          .doc('${ponto['id']}')
          .set(doc)
          .timeout(_esperaConfirmacao);
      return true;
    } catch (e) {
      debugPrint('[Firebase] coleta ainda não confirmada no backup: $e');
      return false;
    }
  }

  /// Sobe o anexo (foto JPEG) para o Storage em `anexos/aaaa/mm/<lancamento>/<ts>-<nome>` e devolve o
  /// caminho. Diferente do backup das coletas, aqui a falha SOBE (quem chama decide a mensagem).
  Future<String> subirAnexo(int lancamentoId, Uint8List bytes, String nomeArquivo) async {
    if (!_ativo) throw StateError('Firebase desligado neste build');
    await _garantirLogin();
    final agora = DateTime.now();
    final mes = agora.month.toString().padLeft(2, '0');
    final caminho = 'anexos/${agora.year}/$mes/$lancamentoId/${agora.millisecondsSinceEpoch}-$nomeArquivo';
    await FirebaseStorage.instance.ref(caminho).putData(bytes, SettableMetadata(contentType: 'image/jpeg'));
    return caminho;
  }

  @override
  bool get ativo => _ativo;

  /// Documentos de coleta de um dia (para o relatório), sem os excluídos. A falha SOBE.
  Future<List<Map<String, dynamic>>> coletasDoDia(DateTime dia) async {
    if (!_ativo) throw StateError('Firebase desligado neste build');
    await _garantirLogin();
    final r = await _dia(dia.day, dia.month, dia.year).get();
    return [
      for (final d in r.docs)
        if (d.data()['status'] != false) d.data(),
    ];
  }

  /// Documentos do dia excluídos (`status: false`), com o conteúdo intacto: a exclusão no app é
  /// só lógica. Usado pela tela provisória de recuperação. A falha SOBE.
  Future<List<Map<String, dynamic>>> coletasExcluidasDoDia(DateTime dia) async {
    if (!_ativo) throw StateError('Firebase desligado neste build');
    await _garantirLogin();
    final r = await _dia(dia.day, dia.month, dia.year).get();
    return [
      for (final d in r.docs)
        if (d.data()['status'] == false) d.data(),
    ];
  }

  /// Espera o Firestore enviar o que ficou na fila local (escritas feitas sem internet). Usado
  /// pela tarefa em segundo plano, que termina logo depois e mataria o envio no meio.
  @override
  Future<void> descarregarPendentes() async {
    if (!_ativo) return;
    try {
      await FirebaseFirestore.instance.waitForPendingWrites().timeout(const Duration(seconds: 25));
    } catch (e) {
      debugPrint('[Firebase] fila local não esvaziou: $e');
    }
  }

  /// Marca a coleta como excluída (soft-delete, `merge`). `true` = o Firestore confirmou.
  @override
  Future<bool> marcarColetaExcluida({required int pontoId, required int dia, required int mes, required int ano}) async {
    if (!_ativo) return false;
    try {
      await _garantirLogin();
      await _dia(dia, mes, ano)
          .doc('$pontoId')
          .set(
            {'status': false, 'dataExclusaoApp': DateTime.now().toUtc().toIso8601String()},
            SetOptions(merge: true),
          )
          .timeout(_esperaConfirmacao);
      return true;
    } catch (e) {
      debugPrint('[Firebase] exclusão ainda não confirmada no backup: $e');
      return false;
    }
  }
}

/// O que o espelho de coletas precisa do Firebase (separado para poder ser testado sem ele).
abstract class BackupRemoto {
  bool get ativo;
  Future<bool> gravarColeta(Map<String, dynamic> payload, Uint8List assinaturaPng);
  Future<bool> marcarColetaExcluida({required int pontoId, required int dia, required int mes, required int ano});
  Future<void> descarregarPendentes();
}
