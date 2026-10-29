// Conversões tolerantes para o JSON do backend, que mistura número, texto e nulo.

int inteiro(Object? v, [int padrao = 0]) {
  if (v is num) return v.toInt();
  if (v is String) return int.tryParse(v.trim()) ?? padrao;
  return padrao;
}

double? numero(Object? v) {
  if (v is num) return v.toDouble();
  if (v is String) return double.tryParse(v.trim().replaceAll(',', '.'));
  return null;
}

bool verdadeiro(Object? v) {
  if (v is bool) return v;
  if (v is num) return v != 0;
  if (v is String) return v == 'true' || v == '1';
  return false;
}

/// Falso só quando o backend mandou explicitamente 0/false (ausente conta como ativo).
bool zeradoOuFalso(Object? v) => v == 0 || v == false;

String texto(Object? v) => v == null ? '' : v.toString();
