import 'dart:io';
import 'dart:typed_data';

import 'coletas_repo.dart';
import 'firebase_backup.dart';

/// Mantém o backup das coletas no Firebase em dia, com a mesma garantia da caixa de saída do servidor:
/// cada coleta guarda se falta gravá-la (fb = 1) ou gravar a exclusão (fb = 2), e isso só zera quando
/// o Firestore CONFIRMA. Falhou (sem internet, login, regra)? Fica pendente e tenta de novo na próxima
/// rodada de sincronização ou na tarefa em segundo plano. A assinatura fica no aparelho até o servidor
/// e o Firebase terem recebido.
class EspelhoFirebase {
  EspelhoFirebase(this._coletas, this._backup);

  final ColetasRepo _coletas;
  final BackupRemoto _backup;

  Future<void>? _atual;
  bool _repetir = false;

  /// Backup ligado neste build? (Sem `env.json` ele fica desligado e nada é marcado como pendente.)
  bool get ativo => _backup.ativo;

  /// Processa os pendentes. Chamadas durante uma execução viram uma repetição ao final dela.
  /// Quem chama durante uma execução em andamento recebe essa mesma execução (e ela repete ao final).
  Future<void> espelharPendentes() {
    if (!_backup.ativo) return Future<void>.value();
    final atual = _atual;
    if (atual != null) {
      _repetir = true;
      return atual;
    }
    return _atual = _processar().whenComplete(() => _atual = null);
  }

  Future<void> _processar() async {
    do {
      _repetir = false;
      var falhasSeguidas = 0;
      for (final item in await _coletas.pendentesEspelho()) {
        final ok = item.fb == 2 ? await _excluir(item) : await _gravar(item);
        if (ok) {
          falhasSeguidas = 0;
          await _coletas.espelhoConfirmado(item.id, item.fb);
        } else if (++falhasSeguidas >= 2) {
          break; // provavelmente sem internet: não espera o tempo limite de cada uma
        }
      }
    } while (_repetir);
  }

  Future<bool> _gravar(ItemEspelho item) async {
    var png = Uint8List(0);
    final arquivo = item.arquivoAssinatura;
    if (arquivo != null && File(arquivo).existsSync()) {
      png = await File(arquivo).readAsBytes();
    }
    return _backup.gravarColeta(item.json, png);
  }

  Future<bool> _excluir(ItemEspelho item) {
    final quando = DateTime.fromMillisecondsSinceEpoch(item.instante);
    return _backup.marcarColetaExcluida(pontoId: item.pontoId, dia: quando.day, mes: quando.month, ano: quando.year);
  }
}
