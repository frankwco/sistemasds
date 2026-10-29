import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';
import '../../background/envio_em_segundo_plano.dart';
import 'signature_pad.dart';

/// Revisão, e-mail e assinatura. Devolve true (Navigator.pop) quando a coleta foi salva.
class AssinaturaPage extends ConsumerStatefulWidget {
  const AssinaturaPage({super.key, required this.ponto, required this.situacao, required this.valores});

  final Ponto ponto;
  final String situacao;

  /// Pesos em milésimos por campo (a, e, b, a2, a3, a4).
  final Map<String, int> valores;

  @override
  ConsumerState<AssinaturaPage> createState() => _AssinaturaPageState();
}

class _AssinaturaPageState extends ConsumerState<AssinaturaPage> {
  final _assinatura = SignatureController();
  final _email = TextEditingController();
  bool _desenhando = false;

  /// Trava o botão e cobre a tela enquanto salva (evita coleta duplicada por toque repetido).
  bool _salvando = false;

  @override
  void dispose() {
    _assinatura.dispose();
    _email.dispose();
    super.dispose();
  }

  Widget _resumoValores() {
    final grupos = widget.ponto.gruposDoPonto;
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final g in grupos)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(color: g.cor, borderRadius: BorderRadius.circular(10), border: Border.all(color: Colors.black12)),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Text(g.nome, style: TextStyle(color: g.texto, fontWeight: FontWeight.bold)),
              Text(formatarPeso(widget.valores[g.campo] ?? 0), style: TextStyle(color: g.texto)),
            ]),
          ),
      ],
    );
  }

  Future<void> _finalizar() async {
    if (_salvando) return;

    final emailNovo = _email.text.trim();
    if (emailNovo.isNotEmpty && !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(emailNovo)) {
      mostrarAviso(context, 'E-mail inválido.');
      return;
    }
    if (_assinatura.vazio) {
      final seguir = await confirmar(
        context,
        titulo: 'Sem assinatura',
        conteudo: const Text('A assinatura está em branco. Deseja salvar a coleta mesmo assim?'),
        confirmar: 'Salvar mesmo assim',
        cancelar: 'Voltar',
      );
      if (!seguir || !mounted) return;
    }
    final ok = await confirmar(
      context,
      titulo: 'Confira os dados',
      conteudo: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(widget.ponto.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text('Situação: ${widget.situacao}'),
          const SizedBox(height: 12),
          if (widget.situacao == 'Coletado') _resumoValores(),
          const SizedBox(height: 12),
          const Text('Deseja SALVAR a coleta?'),
        ],
      ),
      confirmar: 'Salvar',
      cancelar: 'Cancelar',
    );
    if (!ok || !mounted) return;

    setState(() => _salvando = true);
    // Deixa o overlay "Salvando..." ser pintado antes de gerar o PNG.
    await Future<void>.delayed(const Duration(milliseconds: 30));
    try {
      final sessao = ref.read(sessaoProvider);
      final png = await _assinatura.exportarPng();
      await ref.read(coletaServiceProvider).registrar(
            ponto: widget.ponto,
            situacao: widget.situacao,
            valores: widget.valores,
            assinaturaPng: png,
            usuario: sessao.usuario!,
            veiculo: sessao.veiculo,
            motorista: sessao.motorista,
            emailNovo: emailNovo,
          );
      // Coleta salva: manda ao servidor agora, sem esperar o ciclo de 2 min. Se falhar,
      // continua na caixa de saída e o próximo ciclo reenvia.
      final sync = ref.read(syncProvider);
      unawaited(sync.atualizarPendentes());
      unawaited(sync.enviarPendentes().catchError((_) {}));
      // Rede de segurança: se não houver internet agora (ou o app for fechado), o Android envia
      // assim que ela voltar, mesmo com o app fechado.
      unawaited(AgendadorEnvio.agendarEnvio());
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      debugPrint('[Coleta] falha ao salvar: $e');
      if (mounted) {
        setState(() => _salvando = false);
        mostrarAviso(context, 'Erro ao salvar a coleta, tente novamente.');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_salvando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Assinar e Finalizar')),
        body: Stack(
          children: [
            ListView(
              // Enquanto o dedo está na assinatura, a lista não rola junto.
              physics: _desenhando ? const NeverScrollableScrollPhysics() : null,
              padding: const EdgeInsets.only(bottom: 24),
              children: [
                Cartao(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Row(children: [
                      IconeSelo(Icons.assignment_outlined),
                      SizedBox(width: 12),
                      Text('Resumo da coleta', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    ]),
                    const SizedBox(height: 12),
                    Text(widget.ponto.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.w600)),
                    Text('Situação: ${widget.situacao}'),
                    const SizedBox(height: 8),
                    if (widget.situacao == 'Coletado') _resumoValores(),
                  ]),
                ),
                Cartao(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Row(children: [
                      IconeSelo(Icons.mail_outline, cor: Color(0xFFFF8A00)),
                      SizedBox(width: 12),
                      Text('E-mail do ponto', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                    ]),
                    const SizedBox(height: 4),
                    Text(widget.ponto.email.isEmpty ? 'Sem e-mail cadastrado' : widget.ponto.email,
                        style: const TextStyle(color: Colors.black54)),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: 'Novo e-mail (opcional)', prefixIcon: Icon(Icons.mail_outline)),
                    ),
                  ]),
                ),
                Cartao(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Row(children: [
                      const IconeSelo(Icons.draw_outlined, cor: corSucesso),
                      const SizedBox(width: 12),
                      const Expanded(child: Text('Assinatura', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
                      IconButton(tooltip: 'Desfazer', icon: const Icon(Icons.undo), onPressed: _assinatura.desfazer),
                      IconButton(tooltip: 'Limpar', icon: const Icon(Icons.delete_outline), onPressed: _assinatura.limpar),
                    ]),
                    const SizedBox(height: 8),
                    SignaturePad(
                      controller: _assinatura,
                      aoTocar: (tocando) {
                        if (tocando != _desenhando) setState(() => _desenhando = tocando);
                      },
                    ),
                    const SizedBox(height: 8),
                    Row(children: [
                      const Icon(Icons.person_outline, size: 18, color: Colors.black54),
                      const SizedBox(width: 6),
                      Expanded(child: Text(widget.ponto.nomeRazaoSocial, style: const TextStyle(color: Colors.black54))),
                    ]),
                  ]),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                  child: FilledButton.icon(
                    onPressed: _salvando ? null : _finalizar,
                    icon: const Icon(Icons.check_circle_outline),
                    label: const Text('Finalizar'),
                  ),
                ),
              ],
            ),
            if (_salvando) const SobreposicaoOcupado(mensagem: 'Salvando coleta...'),
          ],
        ),
      ),
    );
  }
}
