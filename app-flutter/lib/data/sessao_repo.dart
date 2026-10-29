import 'db.dart';
import 'models.dart';

/// O que fica gravado entre uma abertura do app e outra.
class SessaoEstado {
  const SessaoEstado({this.usuario, this.veiculo, this.motorista, this.cidade});

  final Usuario? usuario;
  final Veiculo? veiculo;
  final Motorista? motorista;
  final Cidade? cidade;
}

/// Sessão, escolhas do operador e pequenas preferências (tabela `kv`).
/// Não guarda a senha: ela só trafega no login.
class SessaoRepo {
  SessaoRepo(this._db);
  final AppDatabase _db;

  static const _usuario = 'usuario';
  static const _veiculo = 'veiculoSelecionado';
  static const _motorista = 'motoristaSelecionado';
  static const _cidade = 'cidadeSelecionada';
  static const _manter = 'manterConectado';
  static const _ultimaAtualizacaoPontos = 'dataUltimaAtualizacao';

  /// Sem "manter conectado" o app volta ao login (e esquece veículo/motorista/cidade),
  /// como no app Ionic. As coletas pendentes NÃO são apagadas: continuam na caixa de
  /// saída e sobem no próximo login.
  Future<SessaoEstado> carregar() async {
    if (await _db.lerKv(_manter) != '1') {
      await limpar();
      return const SessaoEstado();
    }
    final usuario = await _db.lerKv(_usuario);
    final veiculo = await _db.lerKv(_veiculo);
    final motorista = await _db.lerKv(_motorista);
    final cidade = await _db.lerKv(_cidade);
    return SessaoEstado(
      usuario: usuario == null ? null : Usuario.deJson(usuario),
      veiculo: veiculo == null ? null : Veiculo.deJson(veiculo),
      motorista: motorista == null ? null : Motorista.deJson(motorista),
      cidade: cidade == null ? null : Cidade.deJson(cidade),
    );
  }

  /// Usuário gravado, mesmo sem 'manter conectado'. Para a tarefa em segundo plano, que não
  /// pode usar carregar() (ele apaga a sessão de quem não marcou 'manter conectado').
  Future<Usuario?> lerUsuarioSalvo() async {
    final u = await _db.lerKv(_usuario);
    return u == null ? null : Usuario.deJson(u);
  }

  Future<void> salvarUsuario(Usuario u, {required bool manter}) async {
    await _db.gravarKv(_usuario, u.toJson());
    await _db.gravarKv(_manter, manter ? '1' : '0');
  }

  Future<void> salvarVeiculo(Veiculo v) => _db.gravarKv(_veiculo, v.toJson());
  Future<void> salvarMotorista(Motorista m) => _db.gravarKv(_motorista, m.toJson());
  Future<void> salvarCidade(Cidade c) => _db.gravarKv(_cidade, c.toJson());

  Future<void> limpar() async {
    for (final chave in [_usuario, _veiculo, _motorista, _cidade, _manter]) {
      await _db.removerKv(chave);
    }
  }

  Future<String?> lerUltimaAtualizacaoPontos() => _db.lerKv(_ultimaAtualizacaoPontos);
  Future<void> gravarUltimaAtualizacaoPontos(String valor) => _db.gravarKv(_ultimaAtualizacaoPontos, valor);

  /// Quantas vezes o aviso geral de pontos da cidade já abriu hoje (máx. 2 por cidade/dia).
  /// Uma chave por cidade+dia, em vez do array que o app antigo só fazia crescer.
  Future<int> avisosHoje(int cidadeId, String dia) async =>
      int.tryParse(await _db.lerKv('avisos:$cidadeId:$dia') ?? '') ?? 0;

  Future<void> registrarAvisoHoje(int cidadeId, String dia, int vezes) async {
    await _db.gravarKv('avisos:$cidadeId:$dia', '$vezes');
    // Contadores de dias anteriores não servem mais.
    await _db.db.delete('kv', where: "chave LIKE 'avisos:%' AND chave NOT LIKE ?", whereArgs: ['avisos:%:$dia']);
  }
}
