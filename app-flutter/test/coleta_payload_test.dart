import 'dart:math';

import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/data/coleta_item.dart';
import 'package:gestaofacil/data/coleta_payload.dart';
import 'package:gestaofacil/data/localizacao.dart';
import 'package:gestaofacil/data/models.dart';

Ponto _ponto() => Ponto({
      'id': 77,
      'nomeRazaoSocial': 'Padaria São João',
      'nomeFantasia': 'Pão Quente',
      'apelido': 'Centro',
      'gruposColetados': 'A;E;A3;',
      'quantidadeColetadaMes': 2,
      'telefone1': '(44) 99999-0000',
      'telefone2': '  ',
      'cidade': {'id': 3, 'nome': 'Maringá'},
    });

Usuario _usuario() => Usuario({'id': 9, 'nome': 'Ana', 'email': 'ana@ds.com', 'acessoTotal': true});

void main() {
  final agora = DateTime(2026, 10, 27, 14, 5, 9);

  group('payload da coleta', () {
    test('inclui latitude/longitude como texto só quando há posição', () {
      Map<String, dynamic> monta(Posicao? pos) => montarPayloadColeta(
            identificador: 'abc',
            ponto: _ponto(),
            situacao: 'Coletado',
            valores: {'a': 1000},
            agora: agora,
            usuario: _usuario(),
            posicao: pos,
          );

      final com = monta(const Posicao(-23.5505, -46.6333));
      expect(com['latitude'], '-23.5505');
      expect(com['longitude'], '-46.6333');

      final sem = monta(null);
      expect(sem.containsKey('latitude'), isFalse);
      expect(sem.containsKey('longitude'), isFalse);
    });

    test('monta os campos que o backend espera', () {
      final p = montarPayloadColeta(
        identificador: 'abc',
        ponto: _ponto(),
        situacao: 'Coletado',
        valores: {'a': 1500, 'e': 250, 'b': 0, 'a2': 0, 'a3': 20000, 'a4': 0},
        agora: agora,
        usuario: _usuario(),
        veiculo: Veiculo({'id': 4, 'descricao': 'Caminhão', 'placa': 'ABC1D23'}),
        motorista: Motorista({'id': 5, 'nome': 'Beto', 'email': 'beto@ds.com'}),
        emailNovo: ' novo@x.com ',
      );

      expect(p['a'], 1.5);
      expect(p['e'], 0.25);
      expect(p['a3'], 20.0);
      expect(p['totalColetado'], 21.75);
      expect(p['status'], true);
      expect(p['sincronizado'], false);
      expect(p['identificadorApp'], 'abc');
      expect(p['diaUltimaColeta'], 27);
      expect(p['mesUltimaColeta'], 10);
      expect(p['anoUltimaColeta'], 2026);
      expect(p['horaUltimaColeta'], 14);
      expect(p['minutoUltimaColeta'], 5);
      expect(p['segundoUltimaColeta'], 9);
      expect(p['dataUltimaColetaString'], '2026-10-27 14:05:09');
      expect(p['emailNovo'], 'novo@x.com');
      expect(p['nomeMotorista'], 'Beto');
      expect(p['emailMotorista'], 'beto@ds.com');
      expect(p['emailUsuario'], 'ana@ds.com');
      expect((p['veiculo'] as Map)['placa'], 'ABC1D23');
      expect((p['dataUltimaColeta'] as String).endsWith('Z'), isTrue);
    });

    test('o ponto vai com a última coleta de hoje e o contador do mês +1', () {
      final p = montarPayloadColeta(
        identificador: 'x',
        ponto: _ponto(),
        situacao: 'Coletado',
        valores: const {},
        agora: agora,
        usuario: _usuario(),
      );
      final ponto = p['pontoColetaSelecionado'] as Map;
      expect(ponto['diaUltimaColeta'], 27);
      expect(ponto['mesUltimaColeta'], 10);
      expect(ponto['anoUltimaColeta'], 2026);
      expect(ponto['quantidadeColetadaMes'], 3);
    });

    test('fora de "Coletado" os pesos digitados são zerados', () {
      final p = montarPayloadColeta(
        identificador: 'x',
        ponto: _ponto(),
        situacao: 'Fechado',
        valores: {'a': 9000},
        agora: agora,
        usuario: _usuario(),
      );
      expect(p['a'], 0.0);
      expect(p['totalColetado'], 0.0);
    });

    test('sem veículo e motorista manda valores vazios, como o app antigo', () {
      final p = montarPayloadColeta(
        identificador: 'x',
        ponto: _ponto(),
        situacao: 'Coletado',
        valores: const {},
        agora: agora,
        usuario: _usuario(),
      );
      expect((p['veiculo'] as Map)['id'], isNull);
      expect(p['nomeMotorista'], '');
    });
  });

  group('regras de peso', () {
    test('peso suspeito: acima de 20 kg em qualquer grupo', () {
      expect(temPesoSuspeito({'a': 20000, 'e': 0}), isFalse);
      expect(temPesoSuspeito({'a': 20001}), isTrue);
      expect(temPesoSuspeito({'a': 100, 'e': 25000}), isTrue);
    });

    test('somarMilesimos', () {
      expect(somarMilesimos({'a': 1, 'e': 2, 'b': 3}), 6);
    });
  });

  group('identificador', () {
    test('termina com o id do ponto e não repete', () {
      final r = Random(1);
      final a = gerarIdentificador(77, agora, r);
      final b = gerarIdentificador(77, agora, r);
      expect(a.endsWith('77'), isTrue);
      expect(a, isNot(b));
      expect(a.startsWith('${agora.millisecondsSinceEpoch}'), isTrue);
    });
  });

  group('instanteDaColeta', () {
    test('usa os campos dia/mês/ano/hora do app', () {
      final i = instanteDaColeta({
        'anoUltimaColeta': 2026,
        'mesUltimaColeta': 10,
        'diaUltimaColeta': 27,
        'horaUltimaColeta': 14,
        'minutoUltimaColeta': 5,
        'segundoUltimaColeta': 9,
      });
      expect(i, agora.millisecondsSinceEpoch);
    });

    test('cai para dataUltimaColeta (texto ISO ou epoch) e para 0', () {
      expect(instanteDaColeta({'dataUltimaColeta': '2026-10-27T10:00:00Z'}),
          DateTime.utc(2026, 10, 27, 10).millisecondsSinceEpoch);
      expect(instanteDaColeta({'dataUltimaColeta': 1000}), 1000);
      expect(instanteDaColeta({}), 0);
    });
  });

  group('Ponto', () {
    test('grupos e telefones', () {
      final p = _ponto();
      expect(p.gruposDoPonto.map((g) => g.nome), ['A', 'E', 'A3']);
      expect(p.coletaGrupo('B'), isFalse);
      expect(p.telefones, ['(44) 99999-0000']);
    });

    test('"A;" não casa com "A2;"', () {
      final p = Ponto({'gruposColetados': 'A2;'});
      expect(p.coletaGrupo('A'), isFalse);
      expect(p.coletaGrupo('A2'), isTrue);
    });

    test('contador do mês não fica negativo', () {
      final p = Ponto({'quantidadeColetadaMes': 0}).comUltimaColeta(agora, delta: -1);
      expect(p.quantidadeColetadaMes, 0);
    });
  });

  group('ColetaItem', () {
    test('monta título, grupos do ponto, total e data', () {
      final item = ColetaItem.deJson(
        id: 'abc',
        sincronizado: true,
        json: {
          'a': 1.5,
          'e': '2,5',
          'a3': 0,
          'a4': 9,
          'totalColetado': 4,
          'anoUltimaColeta': 2026,
          'mesUltimaColeta': 10,
          'diaUltimaColeta': 27,
          'horaUltimaColeta': 14,
          'minutoUltimaColeta': 5,
          'pontoColetaSelecionado': _ponto().raw,
        },
      );
      expect(item.titulo, 'Padaria São João - Pão Quente - Centro - Maringá');
      // a4 existe na coleta mas o ponto não coleta A4: não aparece.
      expect(item.grupos.map((g) => '${g.nome}=${g.valor}'), ['A=1,500', 'E=2,500', 'A3=0,000']);
      expect(item.total, '4,000');
      expect(item.data, '27/10/2026 14:05');
      expect(item.sincronizado, isTrue);
    });

    test('coleta sem ponto não quebra', () {
      final item = ColetaItem.deJson(id: 'x', sincronizado: false, json: {});
      expect(item.titulo, '');
      expect(item.grupos, isEmpty);
      expect(item.data, '');
    });
  });
}
