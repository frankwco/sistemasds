import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:path/path.dart' as p;

import 'coleta_payload.dart';
import 'coletas_repo.dart';
import 'db.dart';
import 'espelho_firebase.dart';
import 'firebase_backup.dart';
import 'localizacao.dart';
import 'models.dart';
import 'pontos_repo.dart';

/// Registra uma coleta finalizada no aparelho.
class ColetaService {
  ColetaService(this._db, this._pontos, this._coletas, this._backup, this._espelho,
      [this._localizador = const SemLocalizador()]);

  final Localizador _localizador;
  final BackupRemoto _backup;
  final EspelhoFirebase _espelho;
  final AppDatabase _db;
  final PontosRepo _pontos;
  final ColetasRepo _coletas;

  /// Grava a assinatura em arquivo e, numa única transação, a coleta (caixa de saída) e
  /// a atualização do ponto. Ou entram as duas coisas, ou nenhuma: não existe mais a
  /// janela em que a coleta estava salva e o ponto não. O envio ao servidor é feito
  /// por quem chamou (SyncService.enviarPendentes) depois que isto retorna.
  Future<String> registrar({
    required Ponto ponto,
    required String situacao,
    required Map<String, int> valores,
    required Uint8List assinaturaPng,
    required Usuario usuario,
    Veiculo? veiculo,
    Motorista? motorista,
    String emailNovo = '',
  }) async {
    final agora = DateTime.now();
    final id = gerarIdentificador(ponto.id, agora);
    final arquivo = File(p.join(_db.pastaAssinaturas.path, '$id.png'));
    await arquivo.writeAsBytes(assinaturaPng, flush: true);
    final posicao = await _localizador.obter();

    final payload = montarPayloadColeta(
      identificador: id,
      ponto: ponto,
      situacao: situacao,
      valores: valores,
      agora: agora,
      usuario: usuario,
      veiculo: veiculo,
      motorista: motorista,
      emailNovo: emailNovo,
      posicao: posicao,
    );

    try {
      await _db.db.transaction((txn) async {
        await _coletas.inserir(
          txn,
          id: id,
          pontoId: ponto.id,
          nomePonto: ponto.nomeRazaoSocial,
          instante: agora.millisecondsSinceEpoch,
          payload: payload,
          arquivoAssinatura: arquivo.path,
          espelhar: _backup.ativo,
        );
        await _pontos.registrarColeta(txn, ponto.id, agora);
      });
    } catch (_) {
      if (arquivo.existsSync()) arquivo.deleteSync();
      rethrow;
    }
    _coletas.avisarMudanca();
    // Backup no Firebase em segundo plano; não segura a tela nem falha a coleta. Fica marcado como
    // pendente (fb = 1) no banco até o Firestore confirmar, e é repetido se falhar.
    unawaited(_espelho.espelharPendentes());
    return id;
  }
}
