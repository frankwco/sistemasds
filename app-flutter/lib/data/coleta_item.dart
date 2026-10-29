import 'dart:convert';
import 'dart:ui' show Color;

import '../core/conversao.dart';
import '../core/formato.dart';
import 'coleta_payload.dart';
import 'models.dart';

class GrupoValor {
  const GrupoValor(this.nome, this.valor, this.cor, this.texto);
  final String nome;
  final String valor;
  final Color cor;
  final Color texto;
}

/// Linha das listas de coletas, com tudo já calculado (grupos, textos, data), para a
/// tela não refazer isso a cada reconstrução.
class ColetaItem {
  const ColetaItem({
    required this.id,
    required this.titulo,
    required this.sincronizado,
    required this.grupos,
    required this.total,
    required this.data,
    required this.instante,
    required this.json,
    this.tentativas = 0,
    this.erro,
  });

  final String id;
  final String titulo;
  final bool sincronizado;
  final List<GrupoValor> grupos;
  final String total;
  final String data;
  final int instante;
  final Map<String, dynamic> json;

  /// Falhas de envio e o motivo da última (só para coleta ainda pendente).
  final int tentativas;
  final String? erro;

  String get nomePonto => texto(_ponto(json)['nomeRazaoSocial']);

  factory ColetaItem.deLinha(Map<String, Object?> linha) => ColetaItem.deJson(
        id: linha['id'] as String,
        json: Map<String, dynamic>.from(jsonDecode(linha['json'] as String) as Map),
        sincronizado: linha['sincronizado'] == 1,
        tentativas: (linha['tentativas'] as int?) ?? 0,
        erro: linha['ultimo_erro'] as String?,
      );

  factory ColetaItem.deJson({
    required String id,
    required Map<String, dynamic> json,
    required bool sincronizado,
    int tentativas = 0,
    String? erro,
  }) {
    final ponto = _ponto(json);
    final gruposDoPonto = texto(ponto['gruposColetados']).split(';').where((s) => s.isNotEmpty).toSet();
    final cidade = ponto['cidade'];
    final nomeCidade = cidade is Map ? texto(cidade['nome']) : '';
    final instante = instanteDaColeta(json);

    return ColetaItem(
      id: id,
      titulo: [
        texto(ponto['nomeRazaoSocial']),
        texto(ponto['nomeFantasia']),
        texto(ponto['apelido']),
        nomeCidade,
      ].where((s) => s.isNotEmpty).join(' - '),
      sincronizado: sincronizado,
      grupos: [
        for (final g in gruposColeta)
          if (gruposDoPonto.contains(g.nome)) GrupoValor(g.nome, formatarPesoServidor(json[g.campo]), g.cor, g.texto),
      ],
      total: formatarPesoServidor(json['totalColetado']),
      data: instante > 0 ? formatarDataHora(DateTime.fromMillisecondsSinceEpoch(instante)) : '',
      instante: instante,
      json: json,
      tentativas: tentativas,
      erro: erro,
    );
  }

  static Map<String, dynamic> _ponto(Map<String, dynamic> json) {
    final p = json['pontoColetaSelecionado'];
    return p is Map ? Map<String, dynamic>.from(p) : <String, dynamic>{};
  }
}
