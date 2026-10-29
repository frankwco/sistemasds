import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// Traços da assinatura. Desenho e exportação ficam aqui, sem pacote externo: o
/// `angular2-signaturepad` do app Ionic reconstruía a tela inteira a cada toque.
class SignatureController extends ChangeNotifier {
  final List<List<Offset>> _tracos = [];
  Size _tamanho = Size.zero;

  bool get vazio => _tracos.isEmpty;

  void iniciar(Offset p) {
    _tracos.add([p]);
    notifyListeners();
  }

  void adicionar(Offset p) {
    if (_tracos.isEmpty) return;
    final traco = _tracos.last;
    // Ignora micro-movimentos: menos pontos, traço mais liso e menos trabalho por quadro.
    if ((traco.last - p).distance < 1.5) return;
    traco.add(p);
    notifyListeners();
  }

  void desfazer() {
    if (_tracos.isEmpty) return;
    _tracos.removeLast();
    notifyListeners();
  }

  void limpar() {
    if (_tracos.isEmpty) return;
    _tracos.clear();
    notifyListeners();
  }

  static const _espessura = 2.6;

  void _desenhar(Canvas canvas, double escala) {
    final pincel = Paint()
      ..color = Colors.black
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..strokeWidth = _espessura * escala;

    for (final traco in _tracos) {
      if (traco.length == 1) {
        canvas.drawCircle(traco.first * escala, _espessura * escala / 2, pincel..style = PaintingStyle.fill);
        pincel.style = PaintingStyle.stroke;
        continue;
      }
      final caminho = Path()..moveTo(traco.first.dx * escala, traco.first.dy * escala);
      for (var i = 1; i < traco.length - 1; i++) {
        // Curva pelos pontos médios: o traço sai suave em vez de quebrado.
        final meio = (traco[i] + traco[i + 1]) / 2;
        caminho.quadraticBezierTo(traco[i].dx * escala, traco[i].dy * escala, meio.dx * escala, meio.dy * escala);
      }
      caminho.lineTo(traco.last.dx * escala, traco.last.dy * escala);
      canvas.drawPath(caminho, pincel);
    }
  }

  /// PNG com fundo transparente e traço preto, como o `toDataURL()` do app antigo.
  /// [larguraPx] fixa a resolução (e o tamanho do arquivo) independentemente da tela.
  Future<Uint8List> exportarPng({double larguraPx = 640}) async {
    final largura = _tamanho.width <= 0 ? larguraPx : _tamanho.width;
    final altura = _tamanho.height <= 0 ? larguraPx * 0.4 : _tamanho.height;
    final escala = larguraPx / largura;

    final gravador = ui.PictureRecorder();
    _desenhar(Canvas(gravador), escala);
    final imagem = await gravador.endRecording().toImage(larguraPx.round(), (altura * escala).round());
    final bytes = await imagem.toByteData(format: ui.ImageByteFormat.png);
    imagem.dispose();
    return bytes!.buffer.asUint8List();
  }
}

class _Pintor extends CustomPainter {
  _Pintor(this.controller) : super(repaint: controller);

  final SignatureController controller;

  @override
  void paint(Canvas canvas, Size size) {
    controller._tamanho = size;
    controller._desenhar(canvas, 1);
  }

  @override
  bool shouldRepaint(_Pintor antigo) => antigo.controller != controller;
}

/// Área de desenho. Só ela é repintada a cada toque; o resto da tela não é reconstruído.
class SignaturePad extends StatelessWidget {
  const SignaturePad({super.key, required this.controller, this.altura = 180, this.aoTocar});

  final SignatureController controller;
  final double altura;

  /// true ao encostar o dedo, false ao soltar: a tela usa para travar a rolagem durante o traço.
  final ValueChanged<bool>? aoTocar;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: altura,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.black26),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Listener(
          onPointerDown: (e) {
            aoTocar?.call(true);
            controller.iniciar(e.localPosition);
          },
          onPointerMove: (e) => controller.adicionar(e.localPosition),
          onPointerUp: (_) => aoTocar?.call(false),
          onPointerCancel: (_) => aoTocar?.call(false),
          child: RepaintBoundary(
            child: CustomPaint(painter: _Pintor(controller), size: Size.infinite),
          ),
        ),
      ),
    );
  }
}
