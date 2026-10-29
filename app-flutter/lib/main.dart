import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';
import 'background/envio_em_segundo_plano.dart';
import 'data/db.dart';
import 'data/firebase_backup.dart';
import 'data/sessao_repo.dart';
import 'providers.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('pt_BR');

  // Banco e sessão carregados antes da primeira tela: ela já abre no lugar certo
  // (login ou início), sem piscar um carregando.
  final banco = await AppDatabase.abrir();
  final sessao = await SessaoRepo(banco).carregar();
  final backup = await FirebaseBackup.iniciar();
  await AgendadorEnvio.iniciar();

  runApp(ProviderScope(
    overrides: [
      bancoProvider.overrideWithValue(banco),
      firebaseBackupProvider.overrideWithValue(backup),
      sessaoInicialProvider.overrideWithValue(sessao),
    ],
    child: const GestaoFacilApp(),
  ));
}
