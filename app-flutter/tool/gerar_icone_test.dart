// Gera os PNGs do ícone do app (rodar: flutter test tool/gerar_icone_test.dart).
// Saída: assets/icone/icone.png (completo) e assets/icone/icone_frente.png (transparente, para o
// ícone adaptativo do Android). Desenhado em código para não depender de editor de imagem.
import 'dart:io';
import 'dart:math';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show FontLoader;
import 'package:flutter_test/flutter_test.dart';

const _tam = 1024.0;

void _glifo(Canvas c, double escala) {
  final centro = const Offset(_tam / 2, _tam / 2);
  final r = 292.0 * escala;
  final espessura = 68.0 * escala;

  final traco = Paint()
    ..color = Colors.white
    ..style = PaintingStyle.stroke
    ..strokeWidth = espessura
    ..strokeCap = StrokeCap.round;
  final cheio = Paint()..color = Colors.white;

  // Três setas circulares (símbolo de reciclagem).
  for (var i = 0; i < 3; i++) {
    final inicio = (-90 + 120 * i + 14) * pi / 180;
    final varredura = 84 * pi / 180;
    c.drawArc(Rect.fromCircle(center: centro, radius: r), inicio, varredura, false, traco);

    final fim = inicio + varredura;
    final ponta = centro + Offset(cos(fim), sin(fim)) * r;
    final tangente = Offset(-sin(fim), cos(fim)); // sentido horário
    final radial = Offset(cos(fim), sin(fim));
    final meiaBase = espessura / 2 + 30 * escala;
    final cabeca = Path()
      ..moveTo(ponta.dx + tangente.dx * 78 * escala, ponta.dy + tangente.dy * 78 * escala)
      ..lineTo(ponta.dx + radial.dx * meiaBase, ponta.dy + radial.dy * meiaBase)
      ..lineTo(ponta.dx - radial.dx * meiaBase, ponta.dy - radial.dy * meiaBase)
      ..close();
    c.drawPath(cabeca, cheio);
  }

  // Letras DS em laranja no centro (cor diferente do DS branco do app antigo).
  final letras = TextPainter(
    text: TextSpan(
      text: 'DS',
      style: TextStyle(fontFamily: 'ArialNegrito', fontSize: 330 * escala, color: const Color(0xFFFF9F1C), letterSpacing: -6 * escala, height: 1),
    ),
    textDirection: TextDirection.ltr,
  )..layout();
  letras.paint(c, centro - Offset(letras.width / 2, letras.height / 2));
}

Future<void> _salvar(String caminho, void Function(Canvas) desenhar) async {
  final gravador = ui.PictureRecorder();
  desenhar(Canvas(gravador));
  final imagem = await gravador.endRecording().toImage(_tam.toInt(), _tam.toInt());
  final bytes = await imagem.toByteData(format: ui.ImageByteFormat.png);
  File(caminho)
    ..createSync(recursive: true)
    ..writeAsBytesSync(bytes!.buffer.asUint8List());
}

void main() {
  test('gera o ícone', () async {
    // O teste não traz fontes: carrega a Arial Negrito do Windows só para desenhar as letras.
    final fonte = FontLoader('ArialNegrito')
      ..addFont(Future.value(ByteData.sublistView(File('C:/Windows/Fonts/arialbd.ttf').readAsBytesSync())));
    await fonte.load();

    await _salvar('assets/icone/icone.png', (c) {
      final fundo = Paint()
        ..shader = ui.Gradient.linear(
          Offset.zero,
          const Offset(_tam, _tam),
          const [Color(0xFF2F6FEB), Color(0xFF0A2A7A)],
        );
      c.drawRect(const Rect.fromLTWH(0, 0, _tam, _tam), fundo);
      _glifo(c, 1.0);
    });
    await _salvar('assets/icone/icone_frente.png', (c) => _glifo(c, 0.84));
  });
}
