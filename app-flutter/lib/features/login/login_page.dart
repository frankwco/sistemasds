import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/ui.dart';
import '../../data/api_client.dart';
import '../../data/models.dart';
import '../../providers.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _email = TextEditingController();
  final _senha = TextEditingController();
  bool _manter = false;
  bool _ocupado = false;
  bool _ocultarSenha = true;

  @override
  void dispose() {
    _email.dispose();
    _senha.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    final email = _email.text.trim();
    if (email.isEmpty || _senha.text.isEmpty) {
      mostrarAviso(context, 'Informe e-mail e senha.');
      return;
    }
    setState(() => _ocupado = true);
    try {
      final dados = await ref.read(apiProvider).login(email, _senha.text);
      if (!mounted) return;
      if (dados == null) {
        mostrarAviso(context, 'Usuário ou senha inválidos!');
        return;
      }
      await ref.read(sessaoProvider.notifier).entrar(Usuario(dados), manter: _manter);
      // A tela raiz troca sozinha para o início quando a sessão muda.
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível entrar: ${e.descricao}');
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  Future<void> _recuperarSenha() async {
    final campo = TextEditingController(text: _email.text.trim());
    final email = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Recuperar a senha'),
        content: TextField(
          controller: campo,
          keyboardType: TextInputType.emailAddress,
          decoration: const InputDecoration(labelText: 'Seu e-mail'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(100, 40)),
            onPressed: () => Navigator.pop(ctx, campo.text.trim()),
            child: const Text('Recuperar'),
          ),
        ],
      ),
    );
    campo.dispose();
    if (email == null || email.isEmpty || !mounted) return;

    setState(() => _ocupado = true);
    try {
      final r = await ref.read(apiProvider).recuperarSenha(email);
      if (!mounted) return;
      mostrarAviso(
        context,
        r['sucesso'] == true
            ? 'A senha foi encaminhada para o seu e-mail.'
            : 'Erro ao recuperar a senha. ${r['mensagem'] ?? ''}',
      );
    } on ApiException catch (e) {
      if (mounted) mostrarAviso(context, 'Não foi possível recuperar a senha: ${e.descricao}');
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(Icons.recycling, size: 72, color: corPrimaria),
                  const SizedBox(height: 8),
                  Text('Gestão Fácil',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  const Text('Disponível somente para usuários previamente cadastrados.',
                      textAlign: TextAlign.center, style: TextStyle(color: Colors.black54)),
                  const SizedBox(height: 28),
                  TextField(
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.email],
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(labelText: 'E-mail', prefixIcon: Icon(Icons.mail_outline)),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: _senha,
                    obscureText: _ocultarSenha,
                    autofillHints: const [AutofillHints.password],
                    onSubmitted: (_) => _ocupado ? null : _entrar(),
                    decoration: InputDecoration(
                      labelText: 'Senha',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        icon: Icon(_ocultarSenha ? Icons.visibility : Icons.visibility_off),
                        onPressed: () => setState(() => _ocultarSenha = !_ocultarSenha),
                      ),
                    ),
                  ),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Manter-me conectado'),
                    value: _manter,
                    onChanged: (v) => setState(() => _manter = v),
                  ),
                  const SizedBox(height: 8),
                  FilledButton.icon(
                    onPressed: _ocupado ? null : _entrar,
                    icon: _ocupado
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.login),
                    label: Text(_ocupado ? 'Aguarde...' : 'Entrar'),
                  ),
                  TextButton(onPressed: _ocupado ? null : _recuperarSenha, child: const Text('Esqueceu a senha?')),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
