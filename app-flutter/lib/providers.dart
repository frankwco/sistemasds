import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'data/api_client.dart';
import 'data/catalogo_repo.dart';
import 'data/coleta_service.dart';
import 'data/coletas_repo.dart';
import 'data/db.dart';
import 'data/espelho_firebase.dart';
import 'data/firebase_backup.dart';
import 'data/lancamento_service.dart';
import 'data/models.dart';
import 'data/pontos_repo.dart';
import 'data/sessao_repo.dart';
import 'data/sync_service.dart';

/// Sobrescritos em main() com o que foi carregado antes de abrir a primeira tela.
final bancoProvider = Provider<AppDatabase>((ref) => throw UnimplementedError('bancoProvider'));
final sessaoInicialProvider = Provider<SessaoEstado>((ref) => throw UnimplementedError('sessaoInicialProvider'));

final firebaseBackupProvider =
    Provider<FirebaseBackup>((ref) => throw UnimplementedError('firebaseBackupProvider'));

final apiProvider = Provider<ApiClient>((ref) {
  final api = ApiClient();
  ref.onDispose(api.close);
  return api;
});

final sessaoRepoProvider = Provider((ref) => SessaoRepo(ref.watch(bancoProvider)));
final catalogoRepoProvider = Provider((ref) => CatalogoRepo(ref.watch(bancoProvider)));
final pontosRepoProvider = Provider((ref) => PontosRepo(ref.watch(bancoProvider)));
final coletasRepoProvider = Provider((ref) => ColetasRepo(ref.watch(bancoProvider), ref.watch(pontosRepoProvider)));
final espelhoProvider =
    Provider((ref) => EspelhoFirebase(ref.watch(coletasRepoProvider), ref.watch(firebaseBackupProvider)));

final coletaServiceProvider = Provider((ref) => ColetaService(
      ref.watch(bancoProvider),
      ref.watch(pontosRepoProvider),
      ref.watch(coletasRepoProvider),
      ref.watch(firebaseBackupProvider),
      ref.watch(espelhoProvider),
    ));

final lancamentoServiceProvider =
    Provider((ref) => LancamentoService(ref.watch(apiProvider), ref.watch(firebaseBackupProvider)));

final syncProvider = Provider<SyncService>((ref) {
  final sync = SyncService(
    api: ref.watch(apiProvider),
    coletas: ref.watch(coletasRepoProvider),
    pontos: ref.watch(pontosRepoProvider),
    catalogos: ref.watch(catalogoRepoProvider),
    sessao: ref.watch(sessaoRepoProvider),
    espelho: ref.watch(espelhoProvider),
    sessaoAtual: () => ref.read(sessaoProvider),
  );
  ref.onDispose(sync.dispose);
  return sync;
});

/// Quem está logado e o que escolheu (veículo, motorista, cidade). Cada mudança já é gravada.
class SessaoNotifier extends Notifier<SessaoEstado> {
  @override
  SessaoEstado build() => ref.read(sessaoInicialProvider);

  SessaoRepo get _repo => ref.read(sessaoRepoProvider);

  Future<void> entrar(Usuario usuario, {required bool manter}) async {
    await _repo.salvarUsuario(usuario, manter: manter);
    state = SessaoEstado(usuario: usuario);
  }

  Future<void> escolherVeiculo(Veiculo v) async {
    await _repo.salvarVeiculo(v);
    state = SessaoEstado(usuario: state.usuario, veiculo: v, motorista: state.motorista, cidade: state.cidade);
  }

  Future<void> escolherMotorista(Motorista m) async {
    await _repo.salvarMotorista(m);
    state = SessaoEstado(usuario: state.usuario, veiculo: state.veiculo, motorista: m, cidade: state.cidade);
  }

  Future<void> escolherCidade(Cidade c) async {
    await _repo.salvarCidade(c);
    state = SessaoEstado(usuario: state.usuario, veiculo: state.veiculo, motorista: state.motorista, cidade: c);
  }

  Future<void> sair() async {
    await _repo.limpar();
    state = const SessaoEstado();
  }
}

final sessaoProvider = NotifierProvider<SessaoNotifier, SessaoEstado>(SessaoNotifier.new);
