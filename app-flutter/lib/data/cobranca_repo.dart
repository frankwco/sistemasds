import 'dart:convert';

import '../core/conversao.dart';
import '../core/formato.dart';
import 'db.dart';

/// A linha de histórico é de WhatsApp? (o servidor manda o `canal`; `whatsapp` nem sempre vem)
bool historicoDeWhatsapp(Object? linha) =>
    linha is Map && (texto(linha['canal']) == 'WHATSAPP' || verdadeiro(linha['whatsapp']));

/// Texto do último envio por WhatsApp na rodada atual, ou null se ainda não foi enviado.
/// Registro de WhatsApp sem nenhum número confirmado não vale: é envio que abriu o WhatsApp
/// num número que não tinha conta (convite) e depois teve a confirmação desfeita.
String? whatsappEnviadoEm(Map<String, dynamic> cliente) {
  if (!verdadeiro(cliente['telefone1Whatsapp']) && !verdadeiro(cliente['telefone2Whatsapp'])) return null;
  String? ultimo;
  for (final h in (cliente['historicoAtual'] as List? ?? const [])) {
    if (historicoDeWhatsapp(h)) ultimo = texto((h as Map)['texto']);
  }
  return ultimo;
}

enum FiltroCobranca { todos, naoEnviados, enviados }

/// Cobrança que abriu o WhatsApp e ainda espera o operador dizer se foi enviada. Fica gravada
/// porque o Android pode encerrar o app enquanto o WhatsApp está na frente.
class EnvioPendente {
  const EnvioPendente({
    required this.clienteId,
    required this.numero,
    required this.instante,
    this.rolagem = 0,
    this.filtro = FiltroCobranca.todos,
  });

  static EnvioPendente? deJson(String? s) {
    if (s == null) return null;
    final m = jsonDecode(s);
    if (m is! Map) return null;
    final rolagem = m['rolagem'];
    return EnvioPendente(
      clienteId: inteiro(m['clienteId']),
      numero: inteiro(m['numero'], 1),
      instante: inteiro(m['instante']),
      rolagem: rolagem is num ? rolagem.toDouble() : 0,
      filtro: FiltroCobranca.values.asNameMap()[texto(m['filtro'])] ?? FiltroCobranca.todos,
    );
  }

  final int clienteId;

  /// 1 ou 2 (telefone1/telefone2 do cliente).
  final int numero;
  final int instante;

  /// Onde a lista estava, para voltar ao mesmo lugar.
  final double rolagem;
  final FiltroCobranca filtro;

  String toJson() => jsonEncode({
        'clienteId': clienteId,
        'numero': numero,
        'instante': instante,
        'rolagem': rolagem,
        'filtro': filtro.name,
      });

  Duration get idade => DateTime.now().difference(DateTime.fromMillisecondsSinceEpoch(instante));
}

/// O que a Cobrança guarda no aparelho (tabela `kv`).
class CobrancaRepo {
  CobrancaRepo(this._db);
  final AppDatabase _db;

  static const _pendente = 'cobranca:envioPendente';
  static const _semWhatsapp = 'cobranca:semWhatsapp';

  /// Até quando a tela reabre sozinha e até quando ainda se pergunta se o envio foi feito.
  static const validadeReabrir = Duration(minutes: 30);
  static const validadePergunta = Duration(hours: 12);

  Future<void> gravarPendente(EnvioPendente p) => _db.gravarKv(_pendente, p.toJson());

  Future<EnvioPendente?> lerPendente() async {
    final p = EnvioPendente.deJson(await _db.lerKv(_pendente));
    if (p == null || p.idade <= validadePergunta) return p;
    await _db.removerKv(_pendente);
    return null;
  }

  /// Retira o pendente; false se ele já tinha sido resolvido (não registra o envio duas vezes).
  /// Compara o instante, que identifica o envio.
  Future<bool> resolverPendente(EnvioPendente p) async {
    final atual = EnvioPendente.deJson(await _db.lerKv(_pendente));
    if (atual?.instante != p.instante) return false;
    return await _db.db.delete('kv', where: 'chave = ?', whereArgs: [_pendente]) > 0;
  }

  Future<bool> deveReabrir() async {
    final p = await lerPendente();
    return p != null && p.idade <= validadeReabrir;
  }

  /// Pelo número (não só 1/2): corrigiu o telefone no cadastro, a marca deixa de valer.
  static String chaveTelefone(int clienteId, String telefone) => '$clienteId-${apenasDigitos(telefone)}';

  /// Números que o operador já viu que não são WhatsApp.
  Future<Set<String>> lerSemWhatsapp() async {
    final s = await _db.lerKv(_semWhatsapp);
    return s == null ? {} : {for (final v in jsonDecode(s) as List) '$v'};
  }

  Future<void> gravarSemWhatsapp(Set<String> chaves) => _db.gravarKv(_semWhatsapp, jsonEncode(chaves.toList()));
}
