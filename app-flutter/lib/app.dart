import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'features/home/home_page.dart';
import 'features/login/login_page.dart';
import 'providers.dart';

const corPrimaria = Color(0xFF3880FF);
const corSucesso = Color(0xFF2DD36F);
const corAlerta = Color(0xFFFFC409);
const corPerigo = Color(0xFFEB445A);

ThemeData _tema() {
  final esquema = ColorScheme.fromSeed(seedColor: corPrimaria);
  return ThemeData(
    useMaterial3: true,
    colorScheme: esquema,
    scaffoldBackgroundColor: const Color(0xFFF4F5F8),
    appBarTheme: AppBarTheme(
      backgroundColor: corPrimaria,
      foregroundColor: Colors.white,
      elevation: 0,
    ),
    inputDecorationTheme: InputDecorationTheme(
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
      isDense: true,
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(52),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
      ),
    ),
  );
}

class GestaoFacilApp extends ConsumerStatefulWidget {
  const GestaoFacilApp({super.key});

  @override
  ConsumerState<GestaoFacilApp> createState() => _GestaoFacilAppState();
}

class _GestaoFacilAppState extends ConsumerState<GestaoFacilApp> with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  /// Voltou do segundo plano: envia o que ficou pendente e atualiza os pontos.
  @override
  void didChangeAppLifecycleState(AppLifecycleState estado) {
    if (estado == AppLifecycleState.resumed && ref.read(sessaoProvider).usuario != null) {
      unawaited(ref.read(syncProvider).sincronizar().catchError((_) {}));
    }
  }

  @override
  Widget build(BuildContext context) {
    // A sincronização automática vive aqui, na raiz: sobrevive à navegação entre telas
    // (no app Ionic ela morria junto com a aba inicial no meio da coleta em campo).
    ref.listen(sessaoProvider.select((s) => s.usuario?.id), (anterior, atual) {
      final sync = ref.read(syncProvider);
      if (atual == null) {
        sync.parar();
      } else {
        sync.iniciar();
      }
    });
    final logado = ref.watch(sessaoProvider.select((s) => s.usuario != null));
    if (logado) {
      ref.read(syncProvider).iniciar();
    }

    return MaterialApp(
      title: 'Gestão Fácil',
      debugShowCheckedModeBanner: false,
      theme: _tema(),
      locale: const Locale('pt', 'BR'),
      supportedLocales: const [Locale('pt', 'BR')],
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      // Android recente desenha o app por baixo da barra de navegação (voltar/início/recentes):
      // reserva essa faixa em todas as telas. Com o teclado aberto o recuo some sozinho.
      builder: (context, child) => ColoredBox(
        color: const Color(0xFFF4F5F8),
        child: SafeArea(top: false, left: false, right: false, child: child ?? const SizedBox.shrink()),
      ),
      home: logado ? const HomePage() : const LoginPage(),
    );
  }
}
