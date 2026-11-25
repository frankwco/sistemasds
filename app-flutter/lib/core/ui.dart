import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import 'formato.dart';

void mostrarAviso(BuildContext context, String mensagem) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(mensagem), duration: const Duration(seconds: 3)));
}

/// Pergunta Sim/Não; devolve true só se o operador confirmou.
Future<bool> confirmar(
  BuildContext context, {
  required String titulo,
  required Widget conteudo,
  String confirmar = 'Sim',
  String cancelar = 'Não',
  bool destrutivo = false,
}) async {
  final r = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(titulo),
      content: SingleChildScrollView(child: conteudo),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: Text(cancelar)),
        FilledButton(
          style: FilledButton.styleFrom(
            minimumSize: const Size(88, 40),
            backgroundColor: destrutivo ? Theme.of(ctx).colorScheme.error : null,
          ),
          onPressed: () => Navigator.pop(ctx, true),
          child: Text(confirmar),
        ),
      ],
    ),
  );
  return r ?? false;
}

Future<void> informar(BuildContext context, {required String titulo, required Widget conteudo, String botao = 'OK'}) {
  return showDialog<void>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: Text(titulo),
      content: SingleChildScrollView(child: conteudo),
      actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: Text(botao))],
    ),
  );
}

const _canalExterno = MethodChannel('gestaofacil/externo');

/// Abre o WhatsApp no número (com DDI 55 se faltar) e já com a mensagem.
Future<bool> abrirWhatsapp(String telefone, String mensagem) async {
  var numero = apenasDigitos(telefone);
  if (numero.length <= 11) numero = '55$numero';
  final uri = Uri.parse('https://wa.me/$numero?text=${Uri.encodeComponent(mensagem)}');
  try {
    // Nativo (MainActivity): abre em tarefa própria, para o voltar cair na mesma tela do app.
    return await _canalExterno.invokeMethod<bool>('abrir', {'url': uri.toString()}) ?? false;
  } on MissingPluginException {
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  } on PlatformException {
    return launchUrl(uri, mode: LaunchMode.externalApplication);
  }
}

Future<bool> ligarPara(String telefone) => launchUrl(Uri(scheme: 'tel', path: apenasDigitos(telefone)));

/// Tela cheia com spinner por cima de tudo, enquanto uma gravação está em andamento.
class SobreposicaoOcupado extends StatelessWidget {
  const SobreposicaoOcupado({super.key, required this.mensagem});
  final String mensagem;

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: Stack(
        children: [
          const ModalBarrier(color: Colors.black54, dismissible: false),
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const CircularProgressIndicator(color: Colors.white),
                const SizedBox(height: 16),
                Text(mensagem, style: const TextStyle(color: Colors.white, fontSize: 16)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Selo redondo com ícone, usado nos cartões.
class IconeSelo extends StatelessWidget {
  const IconeSelo(this.icone, {super.key, this.cor = const Color(0xFF3880FF)});
  final IconData icone;
  final Color cor;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 44,
      height: 44,
      decoration: BoxDecoration(color: cor.withValues(alpha: 0.14), borderRadius: BorderRadius.circular(12)),
      child: Icon(icone, color: cor),
    );
  }
}

/// Cartão padrão das telas (cantos arredondados, margem lateral de 12).
class Cartao extends StatelessWidget {
  const Cartao({super.key, required this.child, this.onTap, this.cor, this.borda, this.padding = const EdgeInsets.all(14)});

  final Widget child;
  final VoidCallback? onTap;
  final Color? cor;

  /// Contorno colorido para destacar o cartão (ex.: Coleta Única na lista do motorista).
  final Color? borda;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: cor ?? Colors.white,
      elevation: borda == null ? 1 : 3,
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: borda == null ? BorderSide.none : BorderSide(color: borda!, width: 2.5),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(onTap: onTap, child: Padding(padding: padding, child: child)),
    );
  }
}
