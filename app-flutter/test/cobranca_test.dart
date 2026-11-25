import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/data/api_client.dart';
import 'package:gestaofacil/data/cobranca_repo.dart';
import 'package:gestaofacil/data/db.dart';
import 'package:gestaofacil/data/models.dart';
import 'package:gestaofacil/data/sessao_repo.dart';
import 'package:gestaofacil/features/cobranca/cobranca_page.dart';
import 'package:gestaofacil/providers.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

const _canal = MethodChannel('gestaofacil/externo');

/// Servidor simulado com as mesmas regras do ServicoApp.
class Servidor {
  final chamadas = <String>[];
  bool falhar = false;
  final clientes = <Map<String, dynamic>>[
    {
      'clienteId': 1,
      'nome': 'Padaria',
      'telefone1': '(44) 99999-0001',
      'telefone1Whatsapp': false,
      'mensagemWhatsapp': 'Olá',
      'historicoAtual': <Map<String, dynamic>>[],
    },
    {
      'clienteId': 2,
      'nome': 'Clínica',
      'telefone1': '(44) 99999-0002',
      'telefone1Whatsapp': true,
      'mensagemWhatsapp': 'Olá',
      'historicoAtual': <Map<String, dynamic>>[],
    },
  ];

  http.Client get cliente => MockClient((req) async {
        Object corpo;
        if (req.method == 'GET') {
          corpo = {
            'resumo': {'totalClientesEmAberto': 2, 'valorTotalEmAbertoFormatado': r'R$ 10,00'},
            'clientes': clientes,
          };
        } else {
          if (falhar) return http.Response('erro', 500);
          final m = jsonDecode(req.body) as Map;
          final acao = req.url.pathSegments.last;
          chamadas.add('$acao ${m['clienteId']}${m['manterHistorico'] == true ? ' manter' : ''}');
          final c = clientes.firstWhere((c) => c['clienteId'] == m['clienteId']);
          final campo = 'telefone${m['numeroTelefone']}Whatsapp';
          final historico = c['historicoAtual'] as List;
          if (acao == 'confirmar') c[campo] = true;
          if (acao == 'desmarcar') c[campo] = false;
          if (acao != 'desmarcar' && c[campo] == true) historico.add({'canal': 'WHATSAPP', 'texto': '25/11/2026 10:00'});
          corpo = c;
        }
        return http.Response.bytes(utf8.encode(jsonEncode(corpo)), 200);
      });
}

void main() {
  late AppDatabase banco;
  late Servidor servidor;
  late List<String> abertos;

  Future<void> esperar(WidgetTester t) async {
    for (var i = 0; i < 12; i++) {
      await t.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 15)));
      await t.pump(const Duration(milliseconds: 100));
    }
  }

  Future<void> abrir(WidgetTester t) async {
    await t.pumpWidget(ProviderScope(
      overrides: [
        bancoProvider.overrideWithValue(banco),
        sessaoInicialProvider.overrideWithValue(SessaoEstado(usuario: Usuario({'id': 7, 'acessoSistema': true}))),
        apiProvider.overrideWithValue(ApiClient(cliente: servidor.cliente)),
      ],
      child: const MaterialApp(home: CobrancaPage()),
    ));
    await esperar(t);
  }

  Future<void> tocar(WidgetTester t, Finder f) async {
    await t.tap(f);
    await esperar(t);
  }

  void cenario(String nome, Future<void> Function(WidgetTester) corpo) {
    testWidgets(nome, (t) async {
      sqfliteFfiInit();
      servidor = Servidor();
      abertos = [];
      t.binding.defaultBinaryMessenger.setMockMethodCallHandler(_canal, (c) async {
        abertos.add((c.arguments as Map)['url'] as String);
        return true;
      });
      await t.runAsync(() async {
        final db = await databaseFactoryFfi.openDatabase(inMemoryDatabasePath,
            options: OpenDatabaseOptions(
                version: AppDatabase.versaoEsquema, onCreate: AppDatabase.criarEsquema, singleInstance: false));
        banco = AppDatabase.comBanco(db, Directory.systemTemp);
      });
      await corpo(t);
      await t.pumpWidget(const SizedBox());
      await t.runAsync(() => banco.db.close());
    });
  }

  final enviarPrimeiro = find.byTooltip('Enviar WhatsApp').first;
  final enviarSegundo = find.byTooltip('Enviar WhatsApp').last;

  cenario('abrir o WhatsApp não marca nada; continua na Cobrança e pergunta', (t) async {
    await abrir(t);
    expect(find.text('WhatsApp não enviado'), findsNWidgets(2));
    await tocar(t, enviarPrimeiro);
    expect(abertos.single, startsWith('https://wa.me/5544999990001?text='));
    expect(servidor.chamadas, isEmpty);
    expect(find.text('A cobrança foi enviada?'), findsOneWidget);

    await tocar(t, find.text('Sim, enviei'));
    expect(servidor.chamadas, ['confirmar 1']); // confirmar já registra: só uma chamada
    expect(find.text('WhatsApp enviado em 25/11/2026 10:00'), findsOneWidget);
    expect(find.text('Cobrança'), findsOneWidget);
    expect(find.text('Não enviados (1)'), findsOneWidget);
  });

  cenario('número já confirmado: "sim" só registra o envio', (t) async {
    await abrir(t);
    await tocar(t, enviarSegundo);
    await tocar(t, find.text('Sim, enviei'));
    expect(servidor.chamadas, ['registrarEnvio 2']);
  });

  cenario('número que não é WhatsApp fica como não enviado e perde o verde', (t) async {
    (servidor.clientes[1]['historicoAtual'] as List).add({'canal': 'WHATSAPP', 'texto': 'antigo'});
    await abrir(t);
    expect(find.text('Enviados (1)'), findsOneWidget);
    await tocar(t, enviarSegundo);
    await tocar(t, find.text('Número não é WhatsApp'));
    expect(servidor.chamadas, ['desmarcar 2 manter']);
    expect(find.text('Não é WhatsApp'), findsOneWidget);
    expect(find.byTooltip('WhatsApp confirmado'), findsNothing);
    expect(find.text('Não enviados (2)'), findsOneWidget);

    // A marca do número fica guardada no aparelho.
    await t.pumpWidget(const SizedBox());
    await abrir(t);
    expect(find.text('Não é WhatsApp'), findsOneWidget);
  });

  cenario('"não enviei" e o voltar não gravam nada', (t) async {
    await abrir(t);
    await tocar(t, enviarSegundo);
    await tocar(t, find.text('Não enviei'));
    await tocar(t, enviarSegundo);
    await t.binding.handlePopRoute();
    await esperar(t);
    expect(servidor.chamadas, isEmpty);
    expect(find.text('WhatsApp não enviado'), findsNWidgets(2));
    expect(find.text('A cobrança foi enviada?'), findsNothing);
  });

  cenario('app encerrado com o WhatsApp na frente: reabre e pergunta, uma vez só', (t) async {
    await abrir(t);
    await tocar(t, find.text('Não enviados (2)'));
    await tocar(t, enviarSegundo);
    await t.pumpWidget(const SizedBox()); // Android encerrou o app
    expect(await t.runAsync(() => CobrancaRepo(banco).deveReabrir()), isTrue);

    await abrir(t);
    expect(find.text('A cobrança foi enviada?'), findsOneWidget);
    await tocar(t, find.text('Sim, enviei'));
    expect(servidor.chamadas, ['registrarEnvio 2']);
    expect(find.text('Clínica'), findsNothing); // voltou com o filtro "Não enviados"
    expect(await t.runAsync(() => CobrancaRepo(banco).deveReabrir()), isFalse);
  });

  cenario('falha ao salvar oferece tentar de novo', (t) async {
    await abrir(t);
    servidor.falhar = true;
    await tocar(t, enviarPrimeiro);
    await tocar(t, find.text('Sim, enviei'));
    expect(find.text('Não foi possível salvar'), findsOneWidget);
    servidor.falhar = false;
    await tocar(t, find.text('Tentar de novo'));
    expect(servidor.chamadas, ['confirmar 1']);
  });

  cenario('pendente antigo não reabre a tela nem pergunta', (t) async {
    final repo = CobrancaRepo(banco);
    Future<void> gravar(Duration idade) => repo.gravarPendente(EnvioPendente(
        clienteId: 1, numero: 1, instante: DateTime.now().subtract(idade).millisecondsSinceEpoch));
    await t.runAsync(() async {
      await gravar(const Duration(hours: 1));
      expect(await repo.deveReabrir(), isFalse);
      expect(await repo.lerPendente(), isNotNull);
      await gravar(const Duration(hours: 13));
      expect(await repo.lerPendente(), isNull);
    });
  });
}
