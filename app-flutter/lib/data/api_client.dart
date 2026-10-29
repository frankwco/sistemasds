import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../core/config.dart';

/// Falha de chamada ao backend. `status` 0 = sem resposta (sem internet, DNS, timeout).
class ApiException implements Exception {
  ApiException(this.status, [this.mensagem = '']);

  final int status;
  final String mensagem;

  bool get semConexao => status == 0;

  /// Texto curto e legível para mostrar na tela.
  String get descricao {
    if (status == 0) return mensagem.isEmpty ? 'sem conexão com a internet' : 'sem resposta do servidor ($mensagem)';
    if (status == 404) return 'serviço não encontrado (404)';
    if (status == 401 || status == 403) return 'acesso negado ($status)';
    return 'erro $status${mensagem.isEmpty ? '' : ': $mensagem'}';
  }

  @override
  String toString() => 'ApiException($status, $mensagem)';
}

dynamic _decodificar(String corpo) => jsonDecode(corpo);

/// UTF-8 (o normal do backend). Se o servidor mandar Latin-1 sem declarar o charset, em vez de
/// falhar a sincronização inteira lê como Latin-1 e os acentos continuam certos.
String _textoDoCorpo(List<int> bytes) {
  try {
    return utf8.decode(bytes);
  } on FormatException {
    return latin1.decode(bytes);
  }
}

/// Cliente HTTP do backend. Sempre decodifica o corpo como UTF-8 (o `res.body` do pacote
/// http assume Latin-1 sem charset e estragaria os acentos) e decodifica respostas
/// grandes fora da thread da interface.
class ApiClient {
  ApiClient({http.Client? cliente}) : _http = cliente ?? http.Client();

  final http.Client _http;

  Map<String, String> get _cabecalhos => {
        'X-App-Key': Config.apiKey,
        'Content-Type': 'application/json; charset=utf-8',
        'Accept': 'application/json',
      };

  Uri _uri(String caminho) => Uri.parse('${Config.apiBase}$caminho');

  Future<dynamic> _executar(Future<http.Response> Function() chamada,
      {bool decodificar = true, Duration? limite}) async {
    final http.Response res;
    try {
      res = await chamada().timeout(limite ?? Config.timeoutHttp);
    } on TimeoutException {
      throw ApiException(0, 'tempo esgotado');
    } on SocketException {
      throw ApiException(0, 'sem conexão');
    } on http.ClientException catch (e) {
      throw ApiException(0, e.message);
    } on HandshakeException {
      throw ApiException(0, 'falha de conexão segura');
    }

    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException(res.statusCode, res.reasonPhrase ?? '');
    }
    if (!decodificar || res.bodyBytes.isEmpty) return null;
    final corpo = _textoDoCorpo(res.bodyBytes);
    // Lista de pontos pode ter milhares de itens: decodifica numa isolate.
    return corpo.length > 50000 ? compute(_decodificar, corpo) : _decodificar(corpo);
  }

  Future<dynamic> _get(String caminho) => _executar(() => _http.get(_uri(caminho), headers: _cabecalhos));

  Future<dynamic> _post(String caminho, Object corpo, {bool decodificar = true, Duration? limite}) => _executar(
        () => _http.post(_uri(caminho), headers: _cabecalhos, body: utf8.encode(jsonEncode(corpo))),
        decodificar: decodificar,
        limite: limite,
      );

  // ─── Sessão ────────────────────────────────────────────

  /// Devolve o usuário; sem `id` na resposta = e-mail ou senha inválidos.
  Future<Map<String, dynamic>?> login(String email, String senha) async {
    final r = await _post('usuarioAu', {'email': email, 'senha': senha});
    if (r is Map && r['id'] != null) return Map<String, dynamic>.from(r);
    return null;
  }

  /// `{sucesso: bool, mensagem: String}`.
  Future<Map<String, dynamic>> recuperarSenha(String email) async {
    final r = await _post('recuperarSenha', {'email': email});
    return r is Map ? Map<String, dynamic>.from(r) : <String, dynamic>{};
  }

  // ─── Catálogos ─────────────────────────────────────────

  Future<List<dynamic>> cidades() async => (await _get('cidades')) as List<dynamic>? ?? [];
  Future<List<dynamic>> veiculos() async => (await _get('veiculos')) as List<dynamic>? ?? [];
  Future<List<dynamic>> motoristas() async => (await _get('motoristas')) as List<dynamic>? ?? [];

  /// [dataUltimaAtualizacao] no formato `yyyy-MM-dd HH:mm:ss`, ou `nao` para a lista inteira.
  /// Monta a URL na mão para o espaço virar %20 (o `queryParameters` do Dart usa `+`).
  Future<List<dynamic>> pontosReduzidos(String dataUltimaAtualizacao) async {
    final r = await _get(
        'todosPontosColetaReduzidos?dataUltimaAtualizacao=${Uri.encodeComponent(dataUltimaAtualizacao)}');
    if (r is! List) throw ApiException(200, 'resposta de pontos inválida');
    return r;
  }

  // ─── Coletas ───────────────────────────────────────────

  /// Envia coletas (uma por chamada, para uma falha não derrubar as outras) ou o pedido
  /// "lista", e devolve o que o servidor responder.
  Future<List<dynamic>> cadastrarColeta(List<Map<String, dynamic>> corpo,
      {bool decodificar = true, Duration? limite}) async {
    final r = await _post('cadastrarColeta', corpo, decodificar: decodificar, limite: limite);
    return r is List ? r : <dynamic>[];
  }

  Future<List<dynamic>> todasColetas(int pontoId, String dataConsulta) async {
    final r = await _get('todasColetas?idPontoColeta=$pontoId&dataConsulta=${Uri.encodeComponent(dataConsulta)}');
    return r is List ? r : <dynamic>[];
  }

  // ─── Consultas ao vivo (sem cache)

  /// `{dataProximaColeta, mensagem, pontoColeta:{dataUltimaColeta, periodicidadeColeta...}}`.
  Future<Map<String, dynamic>> proximaColeta(int pontoId) async {
    final r = await _get('proximaColeta?idPontoColeta=$pontoId');
    return r is Map ? Map<String, dynamic>.from(r) : <String, dynamic>{};
  }

  /// Pontos com mais de 10 dias sem coleta (`diasSemColeta`).
  Future<List<dynamic>> diasSemColetaPontos() async => (await _get('diasSemColetaPontos')) as List<dynamic>? ?? [];

  Future<List<dynamic>> alertas() async => (await _get('alertas')) as List<dynamic>? ?? [];

  // ─── Lançamentos de veículo (abastecimento, despesa, serviço): sempre direto no servidor

  Future<List<dynamic>> fornecedores() async => (await _get('fornecedores')) as List<dynamic>? ?? [];
  Future<List<dynamic>> categoriasDespesa() async => (await _get('categoriasDespesa')) as List<dynamic>? ?? [];

  Future<Map<String, dynamic>> criarFornecedor(String nome, {String? tipo}) async {
    final r = await _post('fornecedores', {'nome': nome, 'tipo': tipo});
    return r is Map ? Map<String, dynamic>.from(r) : <String, dynamic>{};
  }

  /// Salva e devolve o lançamento com `id`. Uma única nova tentativa quando não há resposta nenhuma
  /// do servidor (visto na prática logo depois de uma chamada longa de IA, com a rede ainda reconectando).
  Future<Map<String, dynamic>> salvarLancamento(Map<String, dynamic> lancamento) async {
    Object? r;
    try {
      r = await _post('lancamentos', [lancamento]);
    } on ApiException catch (e) {
      if (!e.semConexao) rethrow;
      await Future<void>.delayed(const Duration(seconds: 2));
      r = await _post('lancamentos', [lancamento]);
    }
    if (r is List && r.isNotEmpty && r.first is Map) return Map<String, dynamic>.from(r.first as Map);
    throw ApiException(200, 'resposta inválida do servidor');
  }

  Future<List<dynamic>> lancamentosMotorista(int usuarioId) async =>
      (await _get('lancamentosMotorista?motoristaId=$usuarioId')) as List<dynamic>? ?? [];

  Future<void> excluirLancamento(int id, int usuarioId) => _executar(
        () => _http.delete(_uri('lancamentos/$id?motoristaId=$usuarioId'), headers: _cabecalhos),
        decodificar: false,
      );

  /// Registra no backend os metadados do anexo que já foi para o Firebase Storage.
  Future<void> registrarAnexo({
    required int lancamentoId,
    required String nomeArquivo,
    required String caminho,
    required int tamanho,
  }) =>
      _post(
        'anexos',
        {
          'lancamento': {'id': lancamentoId},
          'nomeArquivo': nomeArquivo,
          'caminho': caminho,
          'contentType': 'image/jpeg',
          'tamanho': tamanho,
        },
        decodificar: false,
      );

  // ─── Cobrança (WhatsApp) — só para usuário com acessoSistema; o backend também confere (403)

  /// `{resumo:{...}, clientes:[...]}`.
  Future<Map<String, dynamic>> cobrancas(int usuarioId) async {
    final r = await _get('cobrancas?usuarioId=$usuarioId');
    return r is Map ? Map<String, dynamic>.from(r) : <String, dynamic>{};
  }

  /// acao: 'confirmar' | 'desmarcar' | 'registrarEnvio'. Devolve a cobrança do cliente já atualizada.
  Future<Map<String, dynamic>?> acaoWhatsapp(String acao, int usuarioId, int clienteId, int numeroTelefone) async {
    final r = await _post('cobrancas/whatsapp/$acao',
        {'usuarioId': usuarioId, 'clienteId': clienteId, 'numeroTelefone': numeroTelefone});
    return r is Map ? Map<String, dynamic>.from(r) : null;
  }

  // ─── Captura assistida por IA (Gemini no backend). Nada é salvo aqui: só interpreta.
  // Limite de quase 3 min: maior que o do backend com o Gemini (150 s).

  static const _limiteIa = Duration(seconds: 170);

  Future<Map<String, dynamic>> _ia(String caminho, Map<String, dynamic> corpo) async {
    final r = await _post(caminho, corpo, limite: _limiteIa);
    return r is Map ? Map<String, dynamic>.from(r) : <String, dynamic>{};
  }

  /// Transcreve o áudio: o texto vem em `textoBruto`.
  Future<Map<String, dynamic>> interpretarAudio(String base64, String mimeType) =>
      _ia('ia/audio', {'base64': base64, 'mimeType': mimeType});

  Future<Map<String, dynamic>> interpretarTexto(String texto) => _ia('ia/texto', {'texto': texto});

  /// Foto, opcionalmente complementada por um texto, numa chamada só.
  Future<Map<String, dynamic>> interpretarFotoTexto(String base64, String mimeType, String? texto) =>
      _ia('ia/fotoTexto', {'base64': base64, 'mimeType': mimeType, 'texto': texto});

  /// Aviso de possível duplicidade (mesmo veículo+km ou mesmo nº de documento), ou null. Só reforço
  /// visual: qualquer falha (offline, etc.) vira "sem aviso".
  Future<String?> verificarDuplicidade({int? veiculoId, double? km, String? numeroDocumento}) async {
    final p = <String>[
      if (veiculoId != null && veiculoId > 0) 'veiculoId=$veiculoId',
      if (km != null && km > 0) 'km=${km == km.roundToDouble() ? km.toInt() : km}',
      if (numeroDocumento != null && numeroDocumento.isNotEmpty) 'numeroDocumento=${Uri.encodeComponent(numeroDocumento)}',
    ];
    if (p.isEmpty) return null;
    try {
      final r = await _get('verificarDuplicidade?${p.join('&')}');
      final aviso = r is Map ? r['aviso'] : null;
      return aviso is String && aviso.isNotEmpty ? aviso : null;
    } on ApiException {
      return null;
    }
  }

  void close() => _http.close();
}
