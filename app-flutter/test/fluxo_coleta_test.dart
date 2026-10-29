import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/data/api_client.dart';
import 'package:gestaofacil/data/catalogo_repo.dart';
import 'package:gestaofacil/data/coleta_service.dart';
import 'package:gestaofacil/data/coletas_repo.dart';
import 'package:gestaofacil/data/db.dart';
import 'package:gestaofacil/data/espelho_firebase.dart';
import 'package:gestaofacil/data/firebase_backup.dart';
import 'package:gestaofacil/data/models.dart';
import 'package:gestaofacil/data/pontos_repo.dart';
import 'package:gestaofacil/data/sessao_repo.dart';
import 'package:gestaofacil/data/sync_service.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

/// Testes do módulo principal com SQLite real (em memória) e servidor HTTP simulado.
/// Exercitam as consultas SQL, a transação de gravação, o envio, a exclusão e as corridas
/// entre os dois (o que os testes de regra pura não alcançam).

Map<String, dynamic> pontoJson(int id,
        {int cidade = 1, String nome = 'Padaria', String grupos = 'A;E;', String periodicidade = 'Semanal', Map<String, dynamic>? extra}) =>
    {
      'id': id,
      'idCidade': cidade,
      'status': 1,
      'contratoAtivo': 1,
      'nomeRazaoSocial': '$nome $id',
      'nomeFantasia': 'Fantasia',
      'apelido': '',
      'enderecoCompleto': 'Rua das Flores, $id',
      'gruposColetados': grupos,
      'periodicidadeColeta': periodicidade,
      'quantidadeColetadaMes': 0,
      'quantidadeColetaMes': 4,
      'diaUltimaColeta': 0,
      'mesUltimaColeta': 0,
      'anoUltimaColeta': 0,
      'cidade': {'id': cidade, 'nome': 'Maringá'},
      ...?extra,
    };

/// Firebase falso: registra o que foi pedido e responde sucesso/falha conforme o teste manda.
class FakeBackup implements BackupRemoto {
  FakeBackup({this.ativoFlag = false});
  bool ativoFlag;
  bool resultado = true;
  Future<void> Function()? aoGravar;
  final gravadas = <Map<String, dynamic>>[];
  final pngs = <Uint8List>[];
  final exclusoes = <({int pontoId, int dia, int mes, int ano})>[];
  int tentativas = 0;

  @override
  bool get ativo => ativoFlag;

  @override
  Future<bool> gravarColeta(Map<String, dynamic> payload, Uint8List assinaturaPng) async {
    tentativas++;
    await aoGravar?.call();
    if (!resultado) return false;
    gravadas.add(payload);
    pngs.add(assinaturaPng);
    return true;
  }

  @override
  Future<bool> marcarColetaExcluida({required int pontoId, required int dia, required int mes, required int ano}) async {
    tentativas++;
    if (!resultado) return false;
    exclusoes.add((pontoId: pontoId, dia: dia, mes: mes, ano: ano));
    return true;
  }

  @override
  Future<void> descarregarPendentes() async {}
}

class Amb {
  late AppDatabase banco;
  late Directory pasta;
  late PontosRepo pontos;
  late ColetasRepo coletas;
  late SessaoRepo sessao;
  late CatalogoRepo catalogo;
  late ColetaService servico;
  late SyncService sync;
  late FakeBackup backup;
  late EspelhoFirebase espelho;
  final List<http.Request> requisicoes = [];
  Future<http.Response> Function(http.Request) servidor = (r) async => http.Response('[]', 200);

  final usuario = Usuario({'id': 9, 'nome': 'Ana', 'email': 'ana@ds.com', 'acessoTotal': true});
  final veiculo = Veiculo({'id': 4, 'descricao': 'Caminhão', 'placa': 'ABC1D23'});
  final motorista = Motorista({'id': 5, 'nome': 'Beto', 'email': 'beto@ds.com'});

  Future<void> montar({bool backupAtivo = false}) async {
    final db = await databaseFactoryFfi.openDatabase(
      inMemoryDatabasePath,
      options: OpenDatabaseOptions(
        version: AppDatabase.versaoEsquema,
        onCreate: AppDatabase.criarEsquema,
        singleInstance: false,
      ),
    );
    pasta = Directory.systemTemp.createTempSync('gf_teste_');
    banco = AppDatabase.comBanco(db, pasta);
    pontos = PontosRepo(banco);
    coletas = ColetasRepo(banco, pontos);
    sessao = SessaoRepo(banco);
    catalogo = CatalogoRepo(banco);
    backup = FakeBackup(ativoFlag: backupAtivo);
    espelho = EspelhoFirebase(coletas, backup);
    servico = ColetaService(banco, pontos, coletas, backup, espelho);
    final api = ApiClient(cliente: MockClient((req) {
      requisicoes.add(req);
      return servidor(req);
    }));
    sync = SyncService(
      api: api,
      coletas: coletas,
      pontos: pontos,
      catalogos: catalogo,
      sessao: sessao,
      espelho: espelho,
      sessaoAtual: () => SessaoEstado(usuario: usuario, veiculo: veiculo, motorista: motorista),
    );
  }

  Future<void> desmontar() async {
    await banco.db.close();
    if (pasta.existsSync()) pasta.deleteSync(recursive: true);
  }

  Future<void> carregarPontos(List<Map<String, dynamic>> lista) => pontos.mesclar(lista, completo: true);

  Future<Ponto> ponto(int id) async => (await pontos.pagina(cidadeId: 1, hoje: DateTime.now(), limite: 100, offset: 0))
      .firstWhere((p) => p.id == id);

  Future<String> registrar(int pontoId, {String situacao = 'Coletado', Map<String, int>? valores}) async => servico.registrar(
        ponto: await ponto(pontoId),
        situacao: situacao,
        valores: valores ?? {'a': 1500, 'e': 250},
        assinaturaPng: Uint8List.fromList([1, 2, 3, 4]),
        usuario: usuario,
        veiculo: veiculo,
        motorista: motorista,
      );

  Future<Map<String, Object?>> linhaColeta(String id) async =>
      (await banco.db.query('coletas', where: 'id = ?', whereArgs: [id])).single;

  Future<Map<String, Object?>> linhaPonto(int id) async =>
      (await banco.db.query('pontos', where: 'id = ?', whereArgs: [id])).single;

  List<http.Request> posts(String fim) =>
      requisicoes.where((r) => r.method == 'POST' && r.url.path.endsWith(fim)).toList();

  dynamic corpo(http.Request r) => jsonDecode(utf8.decode(r.bodyBytes));
}

void main() {
  sqfliteFfiInit();
  databaseFactory = databaseFactoryFfi;

  late Amb a;
  setUp(() async {
    a = Amb();
    await a.montar();
  });
  tearDown(() => a.desmontar());

  group('gravar a coleta', () {
    test('grava a coleta pendente, atualiza o ponto e guarda a assinatura em arquivo', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      final id = await a.registrar(1);

      final c = await a.linhaColeta(id);
      expect(c['sincronizado'], 0);
      expect(c['status'], 1);
      expect(c['ponto_id'], 1);
      expect(File(c['assinatura'] as String).existsSync(), isTrue);

      final json = jsonDecode(c['json'] as String) as Map<String, dynamic>;
      expect(json['identificadorApp'], id);
      expect(json['a'], 1.5);
      expect(json['totalColetado'], 1.75);
      expect(json.containsKey('assinatura'), isFalse, reason: 'a assinatura vai do arquivo para base64 só no envio');

      final hoje = DateTime.now();
      final p = await a.linhaPonto(1);
      expect([p['dia'], p['mes'], p['ano']], [hoje.day, hoje.month, hoje.year]);
      expect(Ponto.deJson(p['json'] as String).quantidadeColetadaMes, 1);
      expect(await a.coletas.contarPendentes(), 1);
    });

    test('depois de coletar, o ponto sai de "a coletar" e vai para "coletados"', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      final hoje = DateTime.now();
      await a.registrar(1);

      final coletar = await a.pontos.pagina(cidadeId: 1, hoje: hoje, coletados: false, limite: 50, offset: 0);
      final coletados = await a.pontos.pagina(cidadeId: 1, hoje: hoje, coletados: true, limite: 50, offset: 0);
      expect(coletar.map((p) => p.id), [2]);
      expect(coletados.map((p) => p.id), [1]);
    });

    test('duas coletas do mesmo ponto: contador do mês acumula', () async {
      await a.carregarPontos([pontoJson(1)]);
      await a.registrar(1);
      await a.registrar(1);
      expect(Ponto.deJson((await a.linhaPonto(1))['json'] as String).quantidadeColetadaMes, 2);
    });

    test('situação diferente de Coletado grava pesos zerados', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1, situacao: 'Fechado', valores: {'a': 9000});
      final json = jsonDecode((await a.linhaColeta(id))['json'] as String) as Map<String, dynamic>;
      expect(json['a'], 0.0);
      expect(json['totalColetado'], 0.0);
      expect(json['situacao'], 'Fechado');
    });
  });

  group('envio ao servidor', () {
    test('envia com chave, cabeçalho, assinatura em base64 e marca como enviada', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      final arquivo = (await a.linhaColeta(id))['assinatura'] as String;

      await a.sync.enviarPendentes();

      final envios = a.posts('cadastrarColeta');
      expect(envios, hasLength(1));
      expect(envios.single.headers['X-App-Key'], isNotEmpty);
      expect(envios.single.headers['Content-Type'], contains('application/json'));
      final corpo = a.corpo(envios.single) as List;
      expect(corpo, hasLength(1));
      final c = corpo.single as Map<String, dynamic>;
      expect(c['identificadorApp'], id);
      expect(c['status'], true);
      expect(c['acessoTotal'], true);
      expect(c['assinatura'], startsWith('data:image/png;base64,'));
      expect(base64Decode((c['assinatura'] as String).split(',').last), [1, 2, 3, 4]);
      expect((c['pontoColetaSelecionado'] as Map)['id'], 1);
      expect((c['veiculo'] as Map)['placa'], 'ABC1D23');

      final linha = await a.linhaColeta(id);
      expect(linha['sincronizado'], 1);
      expect(linha['assinatura'], isNull);
      expect(File(arquivo).existsSync(), isFalse, reason: 'assinatura sai do aparelho depois de confirmada');
      expect(a.sync.estado.value.pendentes, 0);
    });

    test('erro 500: continua pendente, guarda o motivo, e o reenvio usa o MESMO identificador', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);

      a.servidor = (_) async => http.Response('erro', 500);
      await a.sync.enviarPendentes();
      var linha = await a.linhaColeta(id);
      expect(linha['sincronizado'], 0);
      expect(linha['tentativas'], 1);
      expect(linha['ultimo_erro'], contains('500'));
      expect(linha['enviando_ate'], isNull, reason: 'a reserva é liberada na falha');

      a.servidor = (_) async => http.Response('[]', 200);
      await a.sync.enviarPendentes();
      linha = await a.linhaColeta(id);
      expect(linha['sincronizado'], 1);

      final ids = a.posts('cadastrarColeta').map((r) => ((a.corpo(r) as List).single as Map)['identificadorApp']).toList();
      expect(ids, [id, id], reason: 'mesmo id nas duas tentativas: o servidor consegue deduplicar');
    });

    test('sem conexão: para na primeira falha, sem tentar as outras coletas', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      await a.registrar(1);
      await a.registrar(2);
      a.servidor = (_) async => throw http.ClientException('sem rede');

      await a.sync.enviarPendentes();

      expect(a.posts('cadastrarColeta'), hasLength(1));
      expect(await a.coletas.contarPendentes(), 2);
      expect(a.sync.estado.value.erro, contains('sem resposta'));
    });

    test('uma coleta com erro de servidor não impede o envio das outras', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      final id1 = await a.registrar(1);
      final id2 = await a.registrar(2);
      a.servidor = (r) async {
        final c = (a.corpo(r) as List).single as Map;
        return c['identificadorApp'] == id1 ? http.Response('x', 500) : http.Response('[]', 200);
      };

      await a.sync.enviarPendentes();

      expect((await a.linhaColeta(id1))['sincronizado'], 0);
      expect((await a.linhaColeta(id2))['sincronizado'], 1);
    });

    test('reserva: dois envios não pegam a mesma coleta ao mesmo tempo', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      expect(await a.coletas.reservarEnvio(id), isTrue);
      expect(await a.coletas.reservarEnvio(id), isFalse);
      // reserva vencida volta a ficar disponível (processo morto no meio do envio)
      await a.banco.db.update('coletas', {'enviando_ate': DateTime.now().millisecondsSinceEpoch - 1});
      expect(await a.coletas.reservarEnvio(id), isTrue);
    });

    test('envios simultâneos (app + tarefa em segundo plano) não duplicam a coleta', () async {
      await a.carregarPontos([pontoJson(1)]);
      await a.registrar(1);
      a.servidor = (_) async {
        await Future<void>.delayed(const Duration(milliseconds: 50));
        return http.Response('[]', 200);
      };
      // segundo SyncService sobre o mesmo banco, como a tarefa do WorkManager
      final api2 = ApiClient(cliente: MockClient((req) {
        a.requisicoes.add(req);
        return a.servidor(req);
      }));
      final outro = SyncService(
        api: api2,
        coletas: ColetasRepo(a.banco, a.pontos),
        pontos: a.pontos,
        catalogos: a.catalogo,
        sessao: a.sessao,
        sessaoAtual: () => SessaoEstado(usuario: a.usuario),
      );

      await Future.wait([a.sync.enviarPendentes(), outro.enviarPendentes()]);

      expect(a.posts('cadastrarColeta'), hasLength(1));
      expect(await a.coletas.contarPendentes(), 0);
    });
  });

  group('exclusão', () {
    test('excluir: some da lista, ponto volta a "a coletar", e a exclusão é enviada com status false', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.sync.enviarPendentes();
      a.requisicoes.clear();

      final r = await a.coletas.excluir(id);
      final hoje = DateTime.now();
      expect(r, isNotNull);
      expect([r!.pontoId, r.dia, r.mes, r.ano], [1, hoje.day, hoje.month, hoje.year]);

      expect(await a.coletas.contar(''), 0, reason: 'excluída não aparece na lista');
      final linha = await a.linhaColeta(id);
      expect([linha['status'], linha['sincronizado']], [0, 0]);

      final p = await a.linhaPonto(1);
      expect([p['dia'], p['mes'], p['ano']], [0, 0, 0]);
      expect(Ponto.deJson(p['json'] as String).quantidadeColetadaMes, 0);
      expect(
        await a.pontos.pagina(cidadeId: 1, hoje: hoje, coletados: false, limite: 10, offset: 0),
        hasLength(1),
      );

      await a.sync.enviarPendentes();
      final c = (a.corpo(a.posts('cadastrarColeta').single) as List).single as Map;
      expect(c['status'], false);
      expect(c['identificadorApp'], id);
      expect((await a.linhaColeta(id))['sincronizado'], 1);
    });

    test('excluir uma de duas coletas do dia mantém o ponto como coletado', () async {
      await a.carregarPontos([pontoJson(1)]);
      await a.registrar(1);
      await Future<void>.delayed(const Duration(milliseconds: 5));
      final id2 = await a.registrar(1);

      await a.coletas.excluir(id2);

      final hoje = DateTime.now();
      final p = await a.linhaPonto(1);
      expect([p['dia'], p['mes'], p['ano']], [hoje.day, hoje.month, hoje.year]);
      expect(Ponto.deJson(p['json'] as String).quantidadeColetadaMes, 1);
    });

    test('excluir uma coleta que ainda não tinha sido enviada: envia a exclusão e fecha pendência', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.coletas.excluir(id);
      await a.sync.enviarPendentes();

      final corpo = (a.corpo(a.posts('cadastrarColeta').single) as List).single as Map;
      expect(corpo['status'], false);
      expect(await a.coletas.contarPendentes(), 0);
    });

    test('CORRIDA: excluir enquanto o envio está no ar NÃO perde a exclusão', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);

      var primeira = true;
      a.servidor = (r) async {
        if (primeira) {
          primeira = false;
          await a.coletas.excluir(id); // o operador exclui no meio do envio
        }
        return http.Response('[]', 200);
      };

      await a.sync.enviarPendentes();

      final envios = a.posts('cadastrarColeta').map((r) => ((a.corpo(r) as List).single as Map)['status']).toList();
      expect(envios, [true, false], reason: 'a versão ativa foi enviada, e logo depois a exclusão');
      final linha = await a.linhaColeta(id);
      expect([linha['status'], linha['sincronizado']], [0, 1]);
      expect(await a.coletas.contarPendentes(), 0);
    });
  });

  group('retorno do servidor ("lista")', () {
    test('não sobrescreve pendente nem excluída; insere novas; mantém as locais ausentes', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      final pendente = await a.registrar(1);
      final jsonAntes = (await a.linhaColeta(pendente))['json'];

      final hoje = DateTime.now();
      Map<String, dynamic> doServidor(String id, int ponto, {bool status = true, String assinatura = 'GRANDE'}) => {
            'identificadorApp': id,
            'status': status,
            'assinatura': assinatura,
            'a': 9.0,
            'totalColetado': 9.0,
            'anoUltimaColeta': hoje.year,
            'mesUltimaColeta': hoje.month,
            'diaUltimaColeta': hoje.day,
            'horaUltimaColeta': 8,
            'pontoColetaSelecionado': pontoJson(ponto),
          };

      await a.coletas.mesclarDoServidor([
        doServidor(pendente, 1), // mesmo id da pendente local: o servidor ainda não a processou
        doServidor('novaA', 2),
        doServidor('novaB', 2, status: false),
      ]);

      expect((await a.linhaColeta(pendente))['json'], jsonAntes);
      expect((await a.linhaColeta(pendente))['sincronizado'], 0);

      final nova = await a.linhaColeta('novaA');
      expect(nova['sincronizado'], 1);
      expect(nova['json'] as String, isNot(contains('GRANDE')), reason: 'assinatura do servidor não é guardada');
      expect((await a.linhaColeta('novaB'))['status'], 0);

      final lista = await a.coletas.pagina(limite: 10, offset: 0);
      expect(lista.map((i) => i.id).toSet(), {pendente, 'novaA'});

      // uma segunda "lista" sem a local não a apaga
      await a.coletas.mesclarDoServidor([]);
      expect(await a.linhaColeta(pendente), isNotNull);
    });

    test('exclusão local ainda não confirmada prevalece sobre o servidor que devolve a coleta ativa', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.sync.enviarPendentes();
      await a.coletas.excluir(id);

      await a.coletas.mesclarDoServidor([
        {
          'identificadorApp': id,
          'status': true,
          'pontoColetaSelecionado': pontoJson(1),
          'anoUltimaColeta': DateTime.now().year,
        }
      ]);

      expect((await a.linhaColeta(id))['status'], 0, reason: 'a coleta excluída não "volta a aparecer"');
    });

    test('coleta já enviada e antiga (mais de 90 dias) sai do aparelho', () async {
      final velha = DateTime.now().subtract(const Duration(days: 120));
      await a.coletas.mesclarDoServidor([
        {
          'identificadorApp': 'velha',
          'status': true,
          'pontoColetaSelecionado': pontoJson(1),
          'anoUltimaColeta': velha.year,
          'mesUltimaColeta': velha.month,
          'diaUltimaColeta': velha.day,
        }
      ]);
      expect(await a.banco.db.query('coletas', where: "id = 'velha'"), isEmpty);
    });

    test('busca por nome ignora acento e maiúsculas, e a lista vem da mais recente para a mais antiga', () async {
      Map<String, dynamic> c(String id, String nome, int dia) => {
            'identificadorApp': id,
            'status': true,
            'pontoColetaSelecionado': pontoJson(1, nome: nome),
            'anoUltimaColeta': DateTime.now().year,
            'mesUltimaColeta': DateTime.now().month,
            'diaUltimaColeta': dia,
          };
      final hoje = DateTime.now().day;
      await a.coletas.mesclarDoServidor([c('x', 'Padaria São João', 1 > hoje ? hoje : 1), c('y', 'Farmácia', hoje)]);

      expect((await a.coletas.pagina(busca: 'sao joao', limite: 10, offset: 0)).map((i) => i.id), ['x']);
      expect(await a.coletas.contar('FARMACIA'), 1);
      final todas = await a.coletas.pagina(limite: 10, offset: 0);
      expect(todas.first.instante >= todas.last.instante, isTrue);
    });
  });

  group('pontos: sincronização e consulta', () {
    test('carga completa: ordem do servidor, remove os que sumiram, preserva os protegidos', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2), pontoJson(3)]);
      await a.registrar(3); // coleta pendente protege o ponto 3

      await a.pontos.mesclar([pontoJson(2), pontoJson(1)], completo: true);

      final ids = (await a.banco.db.query('pontos', orderBy: 'seq')).map((r) => r['id']).toList();
      expect(ids.contains(3), isTrue, reason: 'ponto com coleta pendente não é apagado');
      expect(ids.indexOf(2) < ids.indexOf(1), isTrue, reason: 'a ordem passa a ser a da resposta');
    });

    test('carga completa com resposta VAZIA não apaga a base', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2)]);
      await a.pontos.mesclar([], completo: true);
      expect(await a.pontos.total(), 2);
    });

    test('incremental: atualiza existentes e só insere novos que vêm completos', () async {
      await a.carregarPontos([pontoJson(1)]);
      await a.pontos.mesclar([
        pontoJson(1, nome: 'Renomeado'),
        pontoJson(7), // novo e completo
        {'id': 8, 'nomeRazaoSocial': 'Pela metade'}, // novo sem cidade nem grupos
      ], completo: false);

      expect(await a.pontos.total(), 2);
      expect(Ponto.deJson((await a.linhaPonto(1))['json'] as String).nomeRazaoSocial, 'Renomeado 1');
    });

    test('incremental não desfaz a data da coleta de um ponto com coleta pendente', () async {
      await a.carregarPontos([pontoJson(1)]);
      await a.registrar(1);
      final hoje = DateTime.now();

      await a.pontos.mesclar([pontoJson(1)], completo: false); // snapshot antigo do servidor (sem a coleta)

      final p = await a.linhaPonto(1);
      expect([p['dia'], p['mes'], p['ano']], [hoje.day, hoje.month, hoje.year]);
    });

    test('ponto inativo ou sem contrato não aparece; busca ignora acento e filtra por endereço', () async {
      await a.carregarPontos([
        pontoJson(1, nome: 'Padaria São João'),
        pontoJson(2, extra: {'status': 0}),
        pontoJson(3, extra: {'contratoAtivo': 0}),
        pontoJson(4, nome: 'Farmácia', extra: {'enderecoCompleto': 'Av. Brasil, 100'}),
      ]);
      final hoje = DateTime.now();
      Future<List<int>> ids({String nome = '', String endereco = ''}) async =>
          (await a.pontos.pagina(cidadeId: 1, hoje: hoje, nome: nome, endereco: endereco, limite: 50, offset: 0))
              .map((p) => p.id)
              .toList();

      expect(await ids(), [1, 4]);
      expect(await ids(nome: 'sao joao'), [1]);
      expect(await ids(endereco: 'brasil'), [4]);
      expect(await ids(nome: '100%'), isEmpty, reason: 'curinga do LIKE é escapado');
    });

    test('Coleta Única fica coletada em qualquer dia, até a data ser zerada', () async {
      await a.carregarPontos([
        pontoJson(1, periodicidade: 'Coleta Única', extra: {'diaUltimaColeta': 3, 'mesUltimaColeta': 2, 'anoUltimaColeta': 2020}),
        pontoJson(2, extra: {'diaUltimaColeta': 3, 'mesUltimaColeta': 2, 'anoUltimaColeta': 2020}),
      ]);
      final hoje = DateTime.now();
      final coletados = await a.pontos.pagina(cidadeId: 1, hoje: hoje, coletados: true, limite: 10, offset: 0);
      final coletar = await a.pontos.pagina(cidadeId: 1, hoje: hoje, coletados: false, limite: 10, offset: 0);
      expect(coletados.map((p) => p.id), [1]);
      expect(coletar.map((p) => p.id), [2], reason: 'ponto comum só conta como coletado no próprio dia');
    });

    test('paginação: offset e limite respeitam a ordem do servidor', () async {
      await a.carregarPontos([for (var i = 1; i <= 7; i++) pontoJson(i)]);
      final hoje = DateTime.now();
      final p1 = await a.pontos.pagina(cidadeId: 1, hoje: hoje, limite: 3, offset: 0);
      final p2 = await a.pontos.pagina(cidadeId: 1, hoje: hoje, limite: 3, offset: 3);
      final p3 = await a.pontos.pagina(cidadeId: 1, hoje: hoje, limite: 3, offset: 6);
      expect([...p1, ...p2, ...p3].map((p) => p.id), [1, 2, 3, 4, 5, 6, 7]);
    });
  });

  group('backup no Firebase (espelho)', () {
    // Cada teste usa um ambiente com o backup LIGADO.
    setUp(() async {
      await a.desmontar();
      a = Amb();
      await a.montar(backupAtivo: true);
    });

    Future<int> fb(String id) async => (await a.linhaColeta(id))['fb'] as int;

    test('inclusão: grava o documento (com assinatura) e zera a pendência quando o Firebase confirma', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.espelho.espelharPendentes();

      expect(a.backup.gravadas, hasLength(1));
      expect(a.backup.gravadas.single['identificadorApp'], id);
      expect(a.backup.gravadas.single['status'], true);
      expect(a.backup.pngs.single, [1, 2, 3, 4]);
      expect(await fb(id), 0);
    });

    test('falha do Firebase: fica pendente NO BACKUP e a assinatura NÃO é apagada ao sincronizar com o servidor', () async {
      await a.carregarPontos([pontoJson(1)]);
      a.backup.resultado = false;
      final id = await a.registrar(1);
      final arquivo = (await a.linhaColeta(id))['assinatura'] as String;
      await a.espelho.espelharPendentes();
      expect(await fb(id), 1);

      await a.sync.enviarPendentes(); // servidor confirma; Firebase ainda não
      final linha = await a.linhaColeta(id);
      expect(linha['sincronizado'], 1);
      expect(linha['assinatura'], arquivo, reason: 'a assinatura ainda é necessária para o backup');
      expect(File(arquivo).existsSync(), isTrue);

      a.backup.resultado = true; // a internet/login voltou
      await a.espelho.espelharPendentes();
      expect(a.backup.pngs.last, [1, 2, 3, 4]);
      expect(await fb(id), 0);
      expect((await a.linhaColeta(id))['assinatura'], isNull);
      expect(File(arquivo).existsSync(), isFalse, reason: 'servidor e Firebase em dia: assinatura sai do aparelho');
    });

    test('Firebase confirma antes do servidor: a assinatura sai quando o servidor confirmar', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      final arquivo = (await a.linhaColeta(id))['assinatura'] as String;
      await a.espelho.espelharPendentes();
      expect(File(arquivo).existsSync(), isTrue, reason: 'o servidor ainda não recebeu');

      await a.sync.enviarPendentes();
      expect(File(arquivo).existsSync(), isFalse);
    });

    test('exclusão: marca no Firebase com o dia e o ponto certos', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.espelho.espelharPendentes();

      await a.coletas.excluir(id, espelhar: true);
      expect(await fb(id), 2);
      await a.espelho.espelharPendentes();

      final hoje = DateTime.now();
      expect(a.backup.exclusoes.single, (pontoId: 1, dia: hoje.day, mes: hoje.month, ano: hoje.year));
      expect(await fb(id), 0);
    });

    test('exclusão com o Firebase fora do ar fica pendente e é repetida depois', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      await a.espelho.espelharPendentes();
      a.backup.resultado = false;
      await a.coletas.excluir(id, espelhar: true);
      await a.espelho.espelharPendentes();
      expect(await fb(id), 2);

      a.backup.resultado = true;
      await a.espelho.espelharPendentes();
      expect(a.backup.exclusoes, hasLength(1));
      expect(await fb(id), 0);
    });

    test('CORRIDA: excluir enquanto o Firebase grava a inclusão NÃO perde a exclusão', () async {
      await a.carregarPontos([pontoJson(1)]);
      late String id;
      var primeira = true;
      a.backup.aoGravar = () async {
        if (primeira) {
          primeira = false;
          await a.coletas.excluir(id, espelhar: true); // o operador exclui no meio da gravação
        }
      };
      id = await a.registrar(1);
      await a.espelho.espelharPendentes();
      await a.espelho.espelharPendentes();

      expect(a.backup.exclusoes, hasLength(1), reason: 'a exclusão também chegou ao backup');
      expect(await fb(id), 0);
    });

    test('dois fracassos seguidos interrompem a rodada (sem internet não espera cada coleta)', () async {
      await a.carregarPontos([pontoJson(1), pontoJson(2), pontoJson(3)]);
      a.backup.resultado = false;
      await a.registrar(1);
      await a.registrar(2);
      await a.registrar(3);
      await a.espelho.espelharPendentes(); // espera as execuções disparadas pelo registro
      a.backup.tentativas = 0;
      await a.espelho.espelharPendentes();
      expect(a.backup.tentativas, 2);
      expect(await a.coletas.contarEspelhoPendente(), 3);
    });

    test('a rodada de sincronização também refaz o backup pendente', () async {
      await a.carregarPontos([pontoJson(1)]);
      a.backup.resultado = false;
      final id = await a.registrar(1);
      await a.espelho.espelharPendentes();
      a.backup.resultado = true;

      await a.sync.sincronizar();
      await a.espelho.espelharPendentes(); // espera a execução disparada pela rodada

      expect(await fb(id), 0);
    });
  });

  test('migração: banco antigo (v1) ganha as colunas novas sem perder dados', () async {
    final db = await databaseFactoryFfi.openDatabase(inMemoryDatabasePath, options: OpenDatabaseOptions(singleInstance: false));
    await db.execute('CREATE TABLE coletas (id TEXT PRIMARY KEY, ponto_id INTEGER NOT NULL, busca TEXT NOT NULL, status INTEGER NOT NULL, sincronizado INTEGER NOT NULL, instante INTEGER NOT NULL, assinatura TEXT, json TEXT NOT NULL)');
    await db.insert('coletas', {'id': 'velha', 'ponto_id': 1, 'busca': 'x', 'status': 1, 'sincronizado': 0, 'instante': 1, 'json': '{}'});

    await AppDatabase.atualizarEsquema(db, 1, AppDatabase.versaoEsquema);

    final colunas = (await db.rawQuery('PRAGMA table_info(coletas)')).map((c) => c['name']).toSet();
    expect(colunas, containsAll(['tentativas', 'ultimo_erro', 'enviando_ate', 'fb']));
    final linha = (await db.query('coletas')).single;
    expect(linha['id'], 'velha');
    expect([linha['tentativas'], linha['fb']], [0, 0]);
    await db.close();
  });

  group('rodada completa de sincronização', () {
    test('envia as pendentes ANTES de baixar, e depois traz coletas, pontos e catálogos', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      a.servidor = (r) async {
        final path = r.url.path;
        if (path.endsWith('cadastrarColeta')) return http.Response('[]', 200);
        if (path.endsWith('todosPontosColetaReduzidos')) return http.Response.bytes(utf8.encode(jsonEncode([pontoJson(1), pontoJson(5)])), 200);
        if (path.endsWith('cidades')) return http.Response.bytes(utf8.encode(jsonEncode([{'id': 1, 'nome': 'Maringá'}])), 200);
        if (path.endsWith('veiculos')) return http.Response(jsonEncode([{'id': 4, 'descricao': 'Caminhão', 'placa': 'ABC1D23'}]), 200);
        if (path.endsWith('motoristas')) return http.Response(jsonEncode([{'id': 5, 'nome': 'Beto', 'email': 'b@x.com'}]), 200);
        return http.Response('', 404);
      };

      await a.sync.sincronizar(catalogos: true);

      final ordem = a.requisicoes.map((r) => r.url.path.split('/').last).toList();
      expect(ordem.first, 'cadastrarColeta');
      expect(((a.corpo(a.requisicoes.first) as List).single as Map)['identificadorApp'], id, reason: 'o primeiro POST é a coleta pendente');
      final segundo = (a.corpo(a.requisicoes[1]) as List).single as Map;
      expect(segundo['tipo'], 'lista');
      expect(segundo['veiculo']['placa'], 'ABC1D23');

      expect((await a.linhaColeta(id))['sincronizado'], 1);
      expect(await a.pontos.total(), 2, reason: 'ponto 5 veio na carga');
      expect(await a.catalogo.listarCidades(), hasLength(1));
      expect((await a.catalogo.listarVeiculos()).single.placa, 'ABC1D23');
      expect(a.sync.estado.value.erro, isNull);
      expect(a.sync.estado.value.pendentes, 0);
      expect(a.sync.estado.value.sincronizando, isFalse);
    });

    test('a segunda rodada pede pontos de forma incremental (com data), não a lista inteira', () async {
      await a.carregarPontos([pontoJson(1)]);
      a.servidor = (r) async => http.Response('[]', 200);

      await a.sync.sincronizar();
      await a.sync.sincronizar();

      final consultas = a.requisicoes.where((r) => r.url.path.endsWith('todosPontosColetaReduzidos')).toList();
      expect(consultas, hasLength(2));
      expect(consultas.first.url.queryParameters['dataUltimaAtualizacao'], matches(RegExp(r'^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d$')));
      expect(consultas.first.url.toString(), contains('%20'), reason: 'espaço codificado como %20, não +');
    });

    test('servidor fora do ar: a rodada termina com erro visível e nada é perdido', () async {
      await a.carregarPontos([pontoJson(1)]);
      final id = await a.registrar(1);
      a.servidor = (_) async => throw http.ClientException('sem rede');

      await a.sync.sincronizar();

      expect(a.sync.estado.value.erro, isNotNull);
      expect(a.sync.estado.value.sincronizando, isFalse);
      expect((await a.linhaColeta(id))['sincronizado'], 0);
      expect(await a.pontos.total(), 1);
    });
  });
}
