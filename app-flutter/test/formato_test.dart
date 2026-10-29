import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:gestaofacil/core/formato.dart';

TextEditingValue _digitar(String antes, String depois) =>
    PesoInputFormatter().formatEditUpdate(TextEditingValue(text: antes), TextEditingValue(text: depois));

void main() {
  group('normalizar', () {
    test('tira acento e maiúsculas', () {
      expect(normalizar('São José – Ação'), 'sao jose – acao');
      expect(normalizar('ÇÃO'), 'cao');
    });

    test('aceita nulo e números', () {
      expect(normalizar(null), '');
      expect(normalizar(12), '12');
    });
  });

  group('escaparLike', () {
    test('escapa curingas do LIKE', () {
      expect(escaparLike('50%_a'), r'50\%\_a');
      expect(escaparLike(r'a\b'), r'a\\b');
    });
  });

  group('peso', () {
    test('formata em pt-BR com 3 casas', () {
      expect(formatarPeso(0), '0,000');
      expect(formatarPeso(5), '0,005');
      expect(formatarPeso(1234567), '1.234,567');
    });

    test('formatarPesoServidor aceita número, texto com vírgula e nulo', () {
      expect(formatarPesoServidor(12.5), '12,500');
      expect(formatarPesoServidor('3,25'), '3,250');
      expect(formatarPesoServidor(null), '');
      expect(formatarPesoServidor('abc'), '');
    });

    test('pesoParaMilesimos lê o texto do campo', () {
      expect(pesoParaMilesimos('0,000'), 0);
      expect(pesoParaMilesimos('1.234,567'), 1234567);
    });

    test('digitação estilo calculadora: dígitos entram pela direita', () {
      expect(_digitar('0,000', '0,0005').text, '0,005');
      expect(_digitar('0,005', '0,0052').text, '0,052');
      expect(_digitar('0,052', '0,0521').text, '0,521');
      expect(_digitar('0,521', '0,5213').text, '5,213');
    });

    test('apagar remove o último dígito', () {
      expect(_digitar('5,213', '5,21').text, '0,521');
      expect(_digitar('0,005', '0,00').text, '0,000');
    });

    test('cursor sempre no fim', () {
      final v = _digitar('0,000', '0,0005');
      expect(v.selection.baseOffset, v.text.length);
    });

    test('recusa mais de 9 dígitos', () {
      final antigo = const TextEditingValue(text: '999.999,999');
      final novo = PesoInputFormatter().formatEditUpdate(antigo, const TextEditingValue(text: '999.999,9999'));
      expect(novo.text, '999.999,999');
    });
  });
}
