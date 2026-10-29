import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';

/// Telefones do ponto, com ligar e WhatsApp (via url_launcher, sem plugin de compartilhamento).
class TelefonesPage extends ConsumerWidget {
  const TelefonesPage({super.key, required this.ponto});

  final Ponto ponto;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final telefones = ponto.telefones;
    final nomeUsuario = ref.read(sessaoProvider).usuario?.nome ?? '';

    return Scaffold(
      appBar: AppBar(title: const Text('Telefones')),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          Cartao(
            child: Row(children: [
              const IconeSelo(Icons.business_outlined),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(ponto.nomeRazaoSocial, style: const TextStyle(fontWeight: FontWeight.w600)),
                  if (ponto.nomeFantasia.isNotEmpty)
                    Text(ponto.apelido.isEmpty ? ponto.nomeFantasia : '${ponto.nomeFantasia} - ${ponto.apelido}'),
                  if (ponto.enderecoCompleto.isNotEmpty)
                    Text(ponto.enderecoCompleto, style: const TextStyle(color: Colors.black54, fontSize: 12)),
                ]),
              ),
            ]),
          ),
          if (telefones.isEmpty)
            const Padding(
              padding: EdgeInsets.all(32),
              child: Center(child: Text('Nenhum telefone cadastrado para este ponto.')),
            ),
          for (var i = 0; i < telefones.length; i++)
            Cartao(
              child: Row(children: [
                const IconeSelo(Icons.call_outlined, cor: corSucesso),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text('Telefone ${i + 1}', style: const TextStyle(color: Colors.black54, fontSize: 12)),
                    Text(telefones[i], style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600)),
                  ]),
                ),
                IconButton.filledTonal(
                  tooltip: 'Ligar',
                  icon: const Icon(Icons.call),
                  onPressed: () async {
                    if (!await ligarPara(telefones[i]) && context.mounted) {
                      mostrarAviso(context, 'Não foi possível abrir o discador.');
                    }
                  },
                ),
                const SizedBox(width: 4),
                IconButton.filled(
                  tooltip: 'WhatsApp',
                  style: IconButton.styleFrom(backgroundColor: corSucesso),
                  icon: const Icon(Icons.chat),
                  onPressed: () async {
                    final ok = await abrirWhatsapp(telefones[i], 'Olá, aqui é o $nomeUsuario da DS Resíduos. ');
                    if (!ok && context.mounted) mostrarAviso(context, 'Não foi possível abrir o WhatsApp.');
                  },
                ),
              ]),
            ),
        ],
      ),
    );
  }
}
