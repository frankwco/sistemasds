import 'package:flutter/services.dart';
import 'package:intl/intl.dart';

import 'conversao.dart';

const _comAcento = 'áàâãäåéèêëíìîïóòôõöúùûüçñýÿ';
const _semAcento = 'aaaaaaeeeeiiiiooooouuuucnyy';

/// Minúsculo e sem acento, para a busca ignorar maiúsculas e acentuação
/// (o Dart não tem `String.normalize`).
String normalizar(Object? entrada) {
  final t = texto(entrada).toLowerCase();
  final sb = StringBuffer();
  for (final unidade in t.codeUnits) {
    final c = String.fromCharCode(unidade);
    final i = _comAcento.indexOf(c);
    sb.write(i >= 0 ? _semAcento[i] : c);
  }
  return sb.toString();
}

/// Escapa `%`, `_` e `\` para usar o texto digitado num LIKE ... ESCAPE '\'.
String escaparLike(String s) =>
    s.replaceAll(r'\', r'\\').replaceAll('%', r'\%').replaceAll('_', r'\_');

final NumberFormat _fmtPeso = NumberFormat('#,##0.000', 'pt_BR');

/// Peso em milésimos (1234 = 1,234 kg) -> "1,234". Inteiro evita erro de ponto flutuante.
String formatarPeso(int milesimos) => _fmtPeso.format(milesimos / 1000);

/// Peso que veio do servidor (número ou texto) -> "1,234"; vazio se não der para ler.
String formatarPesoServidor(Object? valor) {
  final n = numero(valor);
  return n == null ? '' : _fmtPeso.format(n);
}

final DateFormat _fmtDataHora = DateFormat('dd/MM/yyyy HH:mm');
final DateFormat _fmtData = DateFormat('dd/MM/yyyy');

String formatarDataHora(DateTime d) => _fmtDataHora.format(d);
String formatarData(DateTime d) => _fmtData.format(d);

/// Só dígitos, pronto para `https://wa.me/55...`.
String apenasDigitos(String s) => s.replaceAll(RegExp(r'\D'), '');

/// Campo de peso estilo calculadora: cada dígito digitado entra pela direita
/// ("5" -> 0,005; "52" -> 0,052). Substitui a máscara brmasker do app Ionic.
class PesoInputFormatter extends TextInputFormatter {
  /// 9 dígitos = até 999.999,999 kg; mais que isso é erro de digitação.
  static const maxDigitos = 9;

  @override
  TextEditingValue formatEditUpdate(TextEditingValue antigo, TextEditingValue novo) {
    final digitos = apenasDigitos(novo.text);
    if (digitos.length > maxDigitos) {
      return antigo;
    }
    final formatado = formatarPeso(int.tryParse(digitos) ?? 0);
    return TextEditingValue(
      text: formatado,
      selection: TextSelection.collapsed(offset: formatado.length),
    );
  }
}

/// Texto do campo de peso -> milésimos.
int pesoParaMilesimos(String texto) => int.tryParse(apenasDigitos(texto)) ?? 0;
