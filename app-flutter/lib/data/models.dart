import 'dart:convert';

import 'package:flutter/painting.dart' show Color;

import '../core/conversao.dart';

/// Grupo de resíduo coletado. `campo` é o nome do campo no JSON da coleta e `nome` o
/// código que aparece em `gruposColetados` do ponto ("A;E;A2;").
class GrupoColeta {
  const GrupoColeta(this.campo, this.nome, this.cor, this.texto);

  final String campo;
  final String nome;
  final Color cor;

  /// Cor da letra sobre o fundo [cor].
  final Color texto;
}

const gruposColeta = <GrupoColeta>[
  GrupoColeta('a', 'A', Color(0xFFF3F3F3), Color(0xFF000000)),
  GrupoColeta('e', 'E', Color(0xFFFFF766), Color(0xFF000000)),
  GrupoColeta('b', 'B', Color(0xFF868083), Color(0xFFFFFFFF)),
  GrupoColeta('a2', 'A2', Color(0xFFFC877E), Color(0xFF000000)),
  GrupoColeta('a3', 'A3', Color(0xFFF75045), Color(0xFFFFFFFF)),
  GrupoColeta('a4', 'A4', Color(0xFF970E04), Color(0xFFFFFFFF)),
];

/// Situações possíveis de uma visita; só "Coletado" leva pesos.
const situacoesColeta = <String>[
  'Coletado',
  'Fechado',
  'Não Atendeu',
  'Feriado',
  'Cliente Contactado',
  'Sem Lixo',
];

class Usuario {
  Usuario(this.raw);
  factory Usuario.deJson(String s) => Usuario(Map<String, dynamic>.from(jsonDecode(s) as Map));

  final Map<String, dynamic> raw;

  int get id => inteiro(raw['id']);
  String get nome => texto(raw['nome']);
  String get email => texto(raw['email']);
  Object? get acessoTotal => raw['acessoTotal'];

  /// Libera o bloco Financeiro (cobrança) na tela inicial.
  bool get acessoSistema => verdadeiro(raw['acessoSistema']);

  String toJson() => jsonEncode(raw);
}

/// Itens de catálogo (cidade, veículo, motorista). Guardam o JSON do servidor inteiro
/// porque o backend recebe o objeto completo (ex.: `veiculo` na coleta).
class Cidade {
  Cidade(this.raw);
  factory Cidade.deJson(String s) => Cidade(Map<String, dynamic>.from(jsonDecode(s) as Map));

  final Map<String, dynamic> raw;
  int get id => inteiro(raw['id']);
  String get nome => texto(raw['nome']);
  String toJson() => jsonEncode(raw);
}

class Veiculo {
  Veiculo(this.raw);
  factory Veiculo.deJson(String s) => Veiculo(Map<String, dynamic>.from(jsonDecode(s) as Map));

  final Map<String, dynamic> raw;
  int get id => inteiro(raw['id']);
  String get descricao => texto(raw['descricao']);
  String get placa => texto(raw['placa']);
  String toJson() => jsonEncode(raw);
}

class Motorista {
  Motorista(this.raw);
  factory Motorista.deJson(String s) => Motorista(Map<String, dynamic>.from(jsonDecode(s) as Map));

  final Map<String, dynamic> raw;
  int get id => inteiro(raw['id']);
  String get nome => texto(raw['nome']);
  String get email => texto(raw['email']);
  String toJson() => jsonEncode(raw);
}

/// Ponto de coleta. Guarda o JSON do servidor como veio (a coleta o leva inteiro no
/// payload) e expõe só os campos que o app usa.
class Ponto {
  Ponto(this.raw);
  factory Ponto.deJson(String s) => Ponto(Map<String, dynamic>.from(jsonDecode(s) as Map));

  final Map<String, dynamic> raw;

  int get id => inteiro(raw['id']);
  String get nomeRazaoSocial => texto(raw['nomeRazaoSocial']);
  String get nomeFantasia => texto(raw['nomeFantasia']);
  String get apelido => texto(raw['apelido']);
  String get endereco => texto(raw['endereco']);
  String get enderecoCompleto => texto(raw['enderecoCompleto']);
  String get periodicidadeColeta => texto(raw['periodicidadeColeta']);
  String get aviso => texto(raw['aviso']);
  bool get situacaoAviso => verdadeiro(raw['situacaoAviso']);
  bool get coletaUnica => periodicidadeColeta == 'Coleta Única';
  int get quantidadeColetadaMes => inteiro(raw['quantidadeColetadaMes']);
  int get quantidadeColetaMes => inteiro(raw['quantidadeColetaMes']);
  String get email => texto(raw['email']);

  /// Data da última coleta, se o servidor mandou (texto ISO ou epoch em ms).
  DateTime? get dataUltimaColeta {
    final v = raw['dataUltimaColeta'];
    if (v is num) return DateTime.fromMillisecondsSinceEpoch(v.toInt());
    if (v is String && v.isNotEmpty) return DateTime.tryParse(v);
    return null;
  }

  late final Set<String> grupos =
      texto(raw['gruposColetados']).split(';').where((s) => s.isNotEmpty).toSet();

  bool coletaGrupo(String nome) => grupos.contains(nome);

  /// Grupos deste ponto, na ordem fixa A, E, B, A2, A3, A4.
  List<GrupoColeta> get gruposDoPonto => gruposColeta.where((g) => grupos.contains(g.nome)).toList();

  List<String> get telefones => [
        for (final chave in const ['telefone1', 'telefone2', 'telefone1Cliente', 'telefone2Cliente'])
          if (texto(raw[chave]).trim().isNotEmpty) texto(raw[chave]).trim(),
      ];

  /// Cópia com a última coleta em [data] e o contador do mês ajustado por [delta].
  Ponto comUltimaColeta(DateTime data, {int delta = 1}) {
    final m = Map<String, dynamic>.from(raw);
    m['diaUltimaColeta'] = data.day;
    m['mesUltimaColeta'] = data.month;
    m['anoUltimaColeta'] = data.year;
    m['quantidadeColetadaMes'] = (inteiro(m['quantidadeColetadaMes']) + delta).clamp(0, 1 << 30);
    return Ponto(m);
  }
}
