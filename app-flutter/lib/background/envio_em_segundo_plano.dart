import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:workmanager/workmanager.dart';

import '../data/api_client.dart';
import '../data/catalogo_repo.dart';
import '../data/coletas_repo.dart';
import '../data/db.dart';
import '../data/espelho_firebase.dart';
import '../data/firebase_backup.dart';
import '../data/pontos_repo.dart';
import '../data/sessao_repo.dart';
import '../data/sync_service.dart';

const _tarefaEnvio = 'enviarColetasPendentes';
const _nomeUnico = 'gf-envio-agora';
const _nomePeriodico = 'gf-envio-periodico';

/// Ponto de entrada da tarefa em segundo plano (roda com o app fechado). O Android a chama
/// quando há internet; ela envia a caixa de saída e termina.
@pragma('vm:entry-point')
void despachanteSegundoPlano() {
  Workmanager().executeTask((tarefa, entrada) async {
    try {
      // Se a tarefa rodar dentro do app aberto, abrir() devolve a mesma conexão: por isso
      // o banco NÃO é fechado aqui.
      final banco = await AppDatabase.abrir();
      final sessao = SessaoRepo(banco);
      final usuario = await sessao.lerUsuarioSalvo();
      final pontos = PontosRepo(banco);
      final coletas = ColetasRepo(banco, pontos);

      // Sem usuário (saiu do app) ou sem nada a enviar: nada a fazer; o que sobrar sobe no próximo login.
      if (usuario == null) return true;
      if (await coletas.contarPendentes() == 0 && await coletas.contarEspelhoPendente() == 0) return true;

      final api = ApiClient();
      final sync = SyncService(
        api: api,
        coletas: coletas,
        pontos: pontos,
        catalogos: CatalogoRepo(banco),
        sessao: sessao,
        sessaoAtual: () => SessaoEstado(usuario: usuario),
      );
      await sync.enviarPendentes();
      sync.dispose();
      api.close();

      // Descarrega o que o Firestore ficou devendo (escritas feitas sem internet).
      // Backup no Firebase: grava o que faltou e espera o Firestore esvaziar a fila local.
      final backup = await FirebaseBackup.iniciar();
      await EspelhoFirebase(coletas, backup).espelharPendentes();
      await backup.descarregarPendentes();

      // Se ainda sobrou algo, devolve "falha": o Android tenta de novo com espera crescente.
      return await coletas.contarPendentes() == 0 && await coletas.contarEspelhoPendente() == 0;
    } catch (e) {
      debugPrint('[Segundo plano] falha: $e');
      return false;
    }
  });
}

/// Agenda o envio em segundo plano. Android apenas.
class AgendadorEnvio {
  const AgendadorEnvio._();

  static bool get _suportado => !kIsWeb && Platform.isAndroid;

  static Future<void> iniciar() async {
    if (!_suportado) return;
    try {
      await Workmanager().initialize(despachanteSegundoPlano);
      // Rede de segurança: a cada 15 min (mínimo do Android), se houver internet, varre a caixa de saída.
      await Workmanager().registerPeriodicTask(
        _nomePeriodico,
        _tarefaEnvio,
        frequency: const Duration(minutes: 15),
        constraints: Constraints(networkType: NetworkType.connected),
        existingWorkPolicy: ExistingPeriodicWorkPolicy.keep,
      );
    } catch (e) {
      debugPrint('[Agendador] não iniciou: $e');
    }
  }

  /// Chamar logo depois de gravar uma coleta (ou uma exclusão): o Android roda o envio assim
  /// que houver internet, mesmo com o app fechado, e repete com espera crescente se falhar.
  static Future<void> agendarEnvio() async {
    if (!_suportado) return;
    try {
      await Workmanager().registerOneOffTask(
        _nomeUnico,
        _tarefaEnvio,
        constraints: Constraints(networkType: NetworkType.connected),
        existingWorkPolicy: ExistingWorkPolicy.replace,
        backoffPolicy: BackoffPolicy.exponential,
        backoffPolicyDelay: const Duration(seconds: 30),
      );
    } catch (e) {
      debugPrint('[Agendador] não agendou: $e');
    }
  }
}
