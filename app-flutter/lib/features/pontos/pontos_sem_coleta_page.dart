import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/estado_consulta.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../providers.dart';
import '../coleta/coletas_do_ponto_page.dart';

/// Pontos com mais de 10 dias sem coleta (consulta ao vivo no servidor).
class PontosSemColetaPage extends ConsumerStatefulWidget {
  const PontosSemColetaPage({super.key});

  @override
  ConsumerState<PontosSemColetaPage> createState() => _PontosSemColetaPageState();
}

class _PontosSemColetaPageState extends ConsumerState<PontosSemColetaPage> {
  late Future<List<dynamic>> _futuro = ref.read(apiProvider).diasSemColetaPontos();

  void _recarregar() => setState(() => _futuro = ref.read(apiProvider).diasSemColetaPontos());

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pontos sem coleta'),
        actions: [IconButton(tooltip: 'Atualizar', icon: const Icon(Icons.refresh), onPressed: _recarregar)],
      ),
      body: CorpoConsulta<List<dynamic>>(
        futuro: _futuro,
        aoTentarDeNovo: _recarregar,
        vazio: (l) => l.isEmpty,
        textoVazio: 'Nenhum ponto pendente! Todos os pontos estão em dia.',
        construir: (context, itens) => ListView.builder(
          padding: const EdgeInsets.only(bottom: 24),
          itemCount: itens.length + 1,
          itemBuilder: (_, i) {
            if (i == 0) {
              return Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                child: Text(
                    'Mais de 10 dias sem coleta - ${itens.length} ponto${itens.length > 1 ? 's' : ''} encontrado${itens.length > 1 ? 's' : ''}',
                    style: const TextStyle(color: Colors.black54)),
              );
            }
            final item = Map<String, dynamic>.from(itens[i - 1] as Map);
            final dias = inteiro(item['diasSemColeta']);
            final cor = dias >= 30 ? corPerigo : corAlerta;
            return Cartao(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Expanded(
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Text(texto(item['nomeRazaoSocial']), style: const TextStyle(fontWeight: FontWeight.w600)),
                      if (texto(item['nomeFantasia']).isNotEmpty) Text(texto(item['nomeFantasia'])),
                    ]),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(color: cor.withOpacity(0.18), borderRadius: BorderRadius.circular(12)),
                    child: Column(children: [
                      Text('$dias', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20, color: cor)),
                      Text('dias', style: TextStyle(fontSize: 11, color: cor)),
                    ]),
                  ),
                ]),
                const SizedBox(height: 6),
                Text(
                  '${texto(item['enderecoCompleto'])} ${texto(item['numero'])} - ${texto(item['nomeCidade'])}',
                  style: const TextStyle(color: Colors.black54, fontSize: 12),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  icon: const Icon(Icons.shopping_bag_outlined),
                  label: const Text('Ver coletas'),
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => ColetasDoPontoPage(ponto: Ponto(item))),
                  ),
                ),
              ]),
            );
          },
        ),
      ),
    );
  }
}
