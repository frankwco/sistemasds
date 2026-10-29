import 'dart:typed_data';

import 'package:flutter/foundation.dart';

import 'api_client.dart';
import 'firebase_backup.dart';

/// Resultado do salvamento: o lançamento já existe no servidor; [anexoFalhou] avisa que só a
/// foto não subiu (dá para anexar depois pela tela administrativa).
class LancamentoSalvo {
  const LancamentoSalvo(this.dados, {this.anexoFalhou = false});
  final Map<String, dynamic> dados;
  final bool anexoFalhou;
}

/// Abastecimento, despesa e serviço: vão direto ao servidor, sem fila offline, de propósito
/// (só valem com confirmação real). Exceções de [ApiException] sobem para a tela mostrar.
class LancamentoService {
  LancamentoService(this._api, this._backup);

  final ApiClient _api;
  final FirebaseBackup _backup;

  Future<LancamentoSalvo> salvar(Map<String, dynamic> lancamento, {Uint8List? foto, String prefixoFoto = 'lancamento'}) async {
    final salvo = await _api.salvarLancamento(lancamento);
    final id = salvo['id'];
    if (id is! num) throw ApiException(200, 'resposta inválida do servidor');

    var anexoFalhou = false;
    if (foto != null) {
      try {
        // O caminho no Storage depende do id, por isso a foto só sobe depois do lançamento salvo.
        final nome = '$prefixoFoto-${DateTime.now().millisecondsSinceEpoch}.jpg';
        final caminho = await _backup.subirAnexo(id.toInt(), foto, nome);
        await _api.registrarAnexo(lancamentoId: id.toInt(), nomeArquivo: nome, caminho: caminho, tamanho: foto.length);
      } catch (e) {
        debugPrint('[Lançamento] salvo, mas o anexo falhou: $e');
        anexoFalhou = true;
      }
    }
    return LancamentoSalvo(salvo, anexoFalhou: anexoFalhou);
  }
}
