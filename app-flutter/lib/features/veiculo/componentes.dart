import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../providers.dart';

/// Data como o backend espera: ao meio-dia local, para o fuso nunca empurrar para o dia vizinho.
String dataParaApi(DateTime d) => DateTime(d.year, d.month, d.day, 12).toUtc().toIso8601String();

/// Lê número digitado com vírgula ou ponto ("1.234,56" ou "12.5"); null se vazio ou inválido.
double? lerDecimal(String entrada) {
  final s = entrada.trim();
  if (s.isEmpty) return null;
  if (s.contains(',')) return double.tryParse(s.replaceAll('.', '').replaceAll(',', '.'));
  return double.tryParse(s);
}

/// Veículo e motorista escolhidos na tela inicial (só leitura).
class CartaoVeiculoMotorista extends ConsumerWidget {
  const CartaoVeiculoMotorista({super.key, this.motoristaObrigatorio = false});

  final bool motoristaObrigatorio;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final s = ref.watch(sessaoProvider);
    return Cartao(
      child: Column(children: [
        Row(children: [
          const IconeSelo(Icons.local_shipping_outlined),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Veículo', style: TextStyle(color: Colors.black54, fontSize: 12)),
              Text(s.veiculo == null ? 'Nenhum selecionado' : '${s.veiculo!.descricao} ${s.veiculo!.placa}',
                  style: const TextStyle(fontWeight: FontWeight.w600)),
            ]),
          ),
        ]),
        const SizedBox(height: 10),
        Row(children: [
          const IconeSelo(Icons.person_outline, cor: corSucesso),
          const SizedBox(width: 12),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Motorista', style: TextStyle(color: Colors.black54, fontSize: 12)),
              Text(
                s.motorista?.nome ?? (motoristaObrigatorio ? 'Nenhum selecionado (obrigatório)' : 'Nenhum selecionado'),
                style: TextStyle(
                    fontWeight: FontWeight.w600, color: s.motorista == null && motoristaObrigatorio ? corPerigo : null),
              ),
            ]),
          ),
        ]),
      ]),
    );
  }
}

/// Campo de data (abre o seletor de data do sistema).
class CampoData extends StatelessWidget {
  const CampoData({super.key, required this.rotulo, required this.valor, required this.aoMudar, this.limpavel = false});

  final String rotulo;
  final DateTime? valor;
  final ValueChanged<DateTime?> aoMudar;
  final bool limpavel;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () async {
        final agora = DateTime.now();
        final d = await showDatePicker(
          context: context,
          initialDate: valor ?? agora,
          firstDate: DateTime(agora.year - 5),
          lastDate: DateTime(agora.year + 5),
        );
        if (d != null) aoMudar(d);
      },
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: rotulo,
          prefixIcon: const Icon(Icons.calendar_today_outlined),
          suffixIcon:
              limpavel && valor != null ? IconButton(icon: const Icon(Icons.clear), onPressed: () => aoMudar(null)) : null,
        ),
        child: Text(valor == null ? 'Selecione' : formatarData(valor!)),
      ),
    );
  }
}

/// Lista de cadastros do servidor ({id, nome}) em menu suspenso, com "+" opcional para cadastrar.
class CampoEntidade extends StatelessWidget {
  const CampoEntidade({
    super.key,
    required this.rotulo,
    required this.icone,
    required this.itens,
    required this.selecionadoId,
    required this.aoMudar,
    this.carregando = false,
    this.aoCadastrar,
  });

  final String rotulo;
  final IconData icone;
  final List<Map<String, dynamic>> itens;
  final int? selecionadoId;
  final ValueChanged<int?> aoMudar;
  final bool carregando;
  final VoidCallback? aoCadastrar;

  @override
  Widget build(BuildContext context) {
    final valor = itens.any((i) => inteiro(i['id']) == selecionadoId) ? selecionadoId : null;
    return Row(children: [
      Expanded(
        child: DropdownButtonFormField<int>(
          key: ValueKey('$rotulo-$valor-${itens.length}'),
          initialValue: valor,
          isExpanded: true,
          decoration: InputDecoration(labelText: rotulo, prefixIcon: Icon(icone)),
          items: [
            for (final i in itens)
              DropdownMenuItem(value: inteiro(i['id']), child: Text(texto(i['nome']), overflow: TextOverflow.ellipsis)),
          ],
          onChanged: carregando ? null : aoMudar,
        ),
      ),
      if (carregando)
        const Padding(
            padding: EdgeInsets.all(12), child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)))
      else if (aoCadastrar != null)
        IconButton(tooltip: 'Cadastrar novo', icon: const Icon(Icons.add_circle_outline), onPressed: aoCadastrar),
    ]);
  }
}

/// Pede um nome e devolve o texto (ou null).
Future<String?> pedirNome(BuildContext context, String titulo, String dica) async {
  final campo = TextEditingController();
  final r = await showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(titulo),
      content: TextField(controller: campo, autofocus: true, decoration: InputDecoration(hintText: dica)),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(100, 40)),
          onPressed: () => Navigator.pop(ctx, campo.text.trim()),
          child: const Text('Cadastrar'),
        ),
      ],
    ),
  );
  campo.dispose();
  return r == null || r.isEmpty ? null : r;
}

/// Foto do comprovante (câmera). A imagem é reduzida e comprimida para subir rápido.
class FotoComprovante extends StatelessWidget {
  const FotoComprovante({super.key, required this.bytes, required this.aoMudar});

  final Uint8List? bytes;
  final ValueChanged<Uint8List?> aoMudar;

  Future<void> _tirar() async {
    final f = await ImagePicker().pickImage(source: ImageSource.camera, imageQuality: 70, maxWidth: 1600, maxHeight: 1600);
    if (f != null) aoMudar(await f.readAsBytes());
  }

  @override
  Widget build(BuildContext context) {
    return Cartao(
      child: bytes == null
          ? OutlinedButton.icon(
              style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(48)),
              onPressed: _tirar,
              icon: const Icon(Icons.photo_camera_outlined),
              label: const Text('Fotografar comprovante'),
            )
          : Row(children: [
              ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: Image.memory(bytes!, width: 110, height: 110, fit: BoxFit.cover)),
              const SizedBox(width: 12),
              const Expanded(child: Text('Comprovante anexado')),
              IconButton(tooltip: 'Trocar', icon: const Icon(Icons.photo_camera_outlined), onPressed: _tirar),
              IconButton(
                  color: corPerigo, tooltip: 'Remover', icon: const Icon(Icons.delete_outline), onPressed: () => aoMudar(null)),
            ]),
    );
  }
}
