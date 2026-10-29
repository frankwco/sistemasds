import 'dart:math';

import 'package:intl/intl.dart';

import '../core/config.dart';
import '../core/conversao.dart';
import 'models.dart';

/// Identificador único da coleta no aparelho (`identificadorApp`). É a chave de
/// idempotência: reenviar a mesma coleta não pode duplicá-la no servidor.
/// Mantém o formato antigo (milissegundos + aleatório + id do ponto), só com o aleatório
/// sendo seguro de verdade.
String gerarIdentificador(int pontoId, DateTime agora, [Random? aleatorio]) {
  final r = aleatorio ?? Random.secure();
  final sorteio = List.generate(8, (_) => r.nextInt(10)).join();
  return '${agora.millisecondsSinceEpoch}$sorteio$pontoId';
}

/// Soma dos pesos em milésimos.
int somarMilesimos(Map<String, int> valores) => valores.values.fold(0, (a, b) => a + b);

/// Algum grupo passa do limite que merece conferência (20 kg)?
bool temPesoSuspeito(Map<String, int> valores) =>
    valores.values.any((v) => v > Config.pesoSuspeitoKg * 1000);

/// Valores que realmente vão na coleta: fora de "Coletado" não há peso, mesmo que o
/// operador tenha digitado antes de trocar a situação.
Map<String, int> valoresEfetivos(String situacao, Map<String, int> digitados) => {
      for (final g in gruposColeta) g.campo: situacao == 'Coletado' ? (digitados[g.campo] ?? 0) : 0,
    };

final DateFormat _fmtTextoApi = DateFormat('yyyy-MM-dd HH:mm:ss');

/// Monta o JSON que o backend recebe em `cadastrarColeta`. A assinatura NÃO entra aqui:
/// ela vai do arquivo para base64 só no envio (ver SyncService).
Map<String, dynamic> montarPayloadColeta({
  required String identificador,
  required Ponto ponto,
  required String situacao,
  required Map<String, int> valores,
  required DateTime agora,
  required Usuario usuario,
  Veiculo? veiculo,
  Motorista? motorista,
  String emailNovo = '',
}) {
  final efetivos = valoresEfetivos(situacao, valores);
  // Instante em UTC com precisão de milissegundos (o Dart às vezes emite microssegundos).
  final instanteUtc = DateTime.fromMillisecondsSinceEpoch(agora.millisecondsSinceEpoch, isUtc: true);

  return {
    'situacao': situacao,
    for (final g in gruposColeta) g.campo: (efetivos[g.campo]! / 1000),
    'totalColetado': somarMilesimos(efetivos) / 1000,
    'status': true,
    'sincronizado': false,
    'identificadorApp': identificador,
    'versaoApp': Config.versaoApp,
    'dataUltimaColeta': instanteUtc.toIso8601String(),
    'dataUltimaColetaString': _fmtTextoApi.format(agora),
    'diaUltimaColeta': agora.day,
    'mesUltimaColeta': agora.month,
    'anoUltimaColeta': agora.year,
    'horaUltimaColeta': agora.hour,
    'minutoUltimaColeta': agora.minute,
    'segundoUltimaColeta': agora.second,
    'emailNovo': emailNovo.trim(),
    'veiculo': veiculo?.raw ?? {'id': null, 'descricao': '', 'placa': ''},
    'nomeMotorista': motorista?.nome ?? '',
    'emailMotorista': motorista?.email ?? '',
    'emailUsuario': usuario.email,
    'pontoColetaSelecionado': ponto.comUltimaColeta(agora).raw,
  };
}

/// Instante (ms) em que a coleta foi finalizada: campos dia/mês/ano/hora do app, ou
/// `dataUltimaColeta` (texto ISO ou epoch) quando vier só isso do servidor.
int instanteDaColeta(Map<String, dynamic> coleta) {
  final ano = inteiro(coleta['anoUltimaColeta']);
  if (ano > 0) {
    return DateTime(
      ano,
      inteiro(coleta['mesUltimaColeta'], 1),
      inteiro(coleta['diaUltimaColeta'], 1),
      inteiro(coleta['horaUltimaColeta']),
      inteiro(coleta['minutoUltimaColeta']),
      inteiro(coleta['segundoUltimaColeta']),
    ).millisecondsSinceEpoch;
  }
  final v = coleta['dataUltimaColeta'];
  if (v is num) return v.toInt();
  if (v is String) return DateTime.tryParse(v)?.millisecondsSinceEpoch ?? 0;
  return 0;
}
