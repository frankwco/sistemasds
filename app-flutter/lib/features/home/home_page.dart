import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/formato.dart';
import '../../core/ui.dart';
import '../../data/models.dart';
import '../../data/sync_service.dart';
import '../../providers.dart';
import '../coleta/coleta_page.dart';
import '../alertas/alertas_page.dart';
import '../pontos/dados_ponto_page.dart';
import '../cobranca/cobranca_page.dart';
import '../relatorio/relatorio_coletas_page.dart';
import '../veiculo/abastecimento_page.dart';
import '../veiculo/captura_ia_page.dart';
import '../veiculo/despesa_page.dart';
import '../veiculo/meus_lancamentos_page.dart';
import '../pontos/pontos_com_avisos_page.dart';
import '../pontos/pontos_sem_coleta_page.dart';
import 'selecao_page.dart';

/// Início: confere o veículo e o motorista da jornada, mostra o estado do envio e abre a coleta.
class HomePage extends ConsumerStatefulWidget {
  const HomePage({super.key});

  @override
  ConsumerState<HomePage> createState() => _HomePageState();
}

class _HomePageState extends ConsumerState<HomePage> {
  @override
  void initState() {
    super.initState();
    // Ao abrir: envia pendentes, atualiza coletas e pontos (incremental, ou completo se o
    // aparelho ainda não tem nenhum) e os catálogos. Fora da primeira montagem da tela.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final sync = ref.read(syncProvider);
      unawaited(sync.sincronizar(catalogos: true).catchError((_) {}));
      unawaited(ref.read(coletasRepoProvider).limparAssinaturasOrfas().catchError((_) {}));
    });
  }

  Future<void> _escolherVeiculo() async {
    final lista = await ref.read(catalogoRepoProvider).listarVeiculos();
    if (!mounted) return;
    final v = await Navigator.push<Veiculo>(
      context,
      MaterialPageRoute(
        builder: (_) => SelecaoPage<Veiculo>(
          titulo: 'Veículo',
          itens: lista,
          rotulo: (v) => v.descricao,
          subtitulo: (v) => v.placa,
          buscavel: true,
          mensagemConfirmacao: (v) =>
              'SELECIONAR o veículo ${v.descricao}? Seleção errada pode ocasionar erros nos lançamentos!',
        ),
      ),
    );
    if (v != null) await ref.read(sessaoProvider.notifier).escolherVeiculo(v);
  }

  Future<void> _escolherMotorista() async {
    final lista = await ref.read(catalogoRepoProvider).listarMotoristas();
    if (!mounted) return;
    final m = await Navigator.push<Motorista>(
      context,
      MaterialPageRoute(
        builder: (_) => SelecaoPage<Motorista>(
          titulo: 'Motorista',
          itens: lista,
          rotulo: (m) => m.nome,
          buscavel: true,
          mensagemConfirmacao: (m) =>
              'SELECIONAR o motorista ${m.nome}? Seleção errada pode ocasionar erros nos lançamentos!',
        ),
      ),
    );
    if (m != null) await ref.read(sessaoProvider.notifier).escolherMotorista(m);
  }

  Future<void> _sair() async {
    final pendentes = ref.read(syncProvider).estado.value.pendentes;
    final ok = await confirmar(
      context,
      titulo: 'Sair',
      conteudo: Text(pendentes > 0
          ? 'Deseja SAIR do app? Há $pendentes coleta(s) ainda não enviada(s); elas continuam guardadas e sobem no próximo acesso.'
          : 'Deseja SAIR do app?'),
    );
    if (ok) await ref.read(sessaoProvider.notifier).sair();
  }

  void _abrirColeta() {
    final s = ref.read(sessaoProvider);
    // Como no app anterior: sem veículo e motorista a coleta não abre.
    if (s.veiculo == null || s.motorista == null) {
      mostrarAviso(context, 'Selecione o VEÍCULO e depois o MOTORISTA!');
      return;
    }
    unawaited(ref.read(syncProvider).sincronizar().catchError((_) {}));
    Navigator.push(context, MaterialPageRoute(builder: (_) => const ColetaPage()));
  }

  void _abrir(Widget pagina) => Navigator.push(context, MaterialPageRoute(builder: (_) => pagina));

  List<Widget> _grade(String titulo, List<_Atalho> atalhos) => [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
          child: Text(titulo.toUpperCase(),
              style: const TextStyle(color: Colors.black54, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 0.8)),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 6),
          child: GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            childAspectRatio: 1.7,
            children: [for (final a in atalhos) a],
          ),
        ),
      ];

  @override
  Widget build(BuildContext context) {
    final sessao = ref.watch(sessaoProvider);
    final sync = ref.watch(syncProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Gestão Fácil'),
        actions: [IconButton(tooltip: 'Sair', icon: const Icon(Icons.logout), onPressed: _sair)],
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text('Olá, ${sessao.usuario?.nome ?? ''}', style: Theme.of(context).textTheme.titleLarge),
                  Text(formatarData(DateTime.now()), style: const TextStyle(color: Colors.black54)),
                ]),
              ),
              // Canto oposto ao nome e à data: só aparece enquanto o app está atualizando.
              ValueListenableBuilder<SyncEstado>(
                valueListenable: sync.estado,
                builder: (context, e, _) => (e.sincronizando || e.enviando)
                    ? Padding(
                        padding: const EdgeInsets.only(top: 4, left: 8),
                        child: Row(mainAxisSize: MainAxisSize.min, children: [
                          const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)),
                          const SizedBox(width: 8),
                          Text(e.enviando ? 'Enviando...' : 'Atualizando dados...',
                              style: const TextStyle(color: Colors.black54, fontSize: 12)),
                        ]),
                      )
                    : const SizedBox.shrink(),
              ),
            ]),
          ),
          Cartao(
            onTap: _escolherVeiculo,
            child: Row(children: [
              const IconeSelo(Icons.local_shipping_outlined),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  const Text('Veículo', style: TextStyle(color: Colors.black54, fontSize: 12)),
                  Text(sessao.veiculo == null ? 'Selecione' : '${sessao.veiculo!.descricao} ${sessao.veiculo!.placa}',
                      style: const TextStyle(fontWeight: FontWeight.w600)),
                ]),
              ),
              const Icon(Icons.chevron_right),
            ]),
          ),
          Cartao(
            onTap: _escolherMotorista,
            child: Row(children: [
              const IconeSelo(Icons.person_outline, cor: Color(0xFFFF8A00)),
              const SizedBox(width: 12),
              Expanded(
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  const Text('Motorista', style: TextStyle(color: Colors.black54, fontSize: 12)),
                  Text(sessao.motorista?.nome ?? 'Selecione', style: const TextStyle(fontWeight: FontWeight.w600)),
                ]),
              ),
              const Icon(Icons.chevron_right),
            ]),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: FilledButton.icon(
              onPressed: _abrirColeta,
              icon: const Icon(Icons.shopping_bag_outlined),
              label: const Text('Coleta'),
            ),
          ),
          // Sem o env.json no build o backup no Firebase não funciona: avisa em vez de falhar em silêncio.
          if (!ref.watch(firebaseBackupProvider).ativo)
            Cartao(
              cor: const Color(0xFFFFE9E9),
              child: const Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Icon(Icons.cloud_off, color: corPerigo),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Backup no Firebase DESLIGADO neste aplicativo: as coletas não estão sendo copiadas para o Firebase. '
                    'Avise o suporte (versão gerada sem as credenciais).',
                    style: TextStyle(fontSize: 13),
                  ),
                ),
              ]),
            ),
          ..._grade('Veículos', [
            _Atalho(Icons.auto_awesome, 'Lançamento por IA', const Color(0xFFB45CFF), () => _abrir(const CapturaIaPage())),
            _Atalho(Icons.water_drop_outlined, 'Abastecimento', corPrimaria, () => _abrir(const AbastecimentoPage())),
            _Atalho(Icons.construction_outlined, 'Despesa/Manutenção', corAlerta, () => _abrir(const DespesaPage())),
            _Atalho(Icons.list_alt_outlined, 'Meus lançamentos', corPrimaria, () => _abrir(const MeusLancamentosPage())),
            _Atalho(Icons.notifications_outlined, 'Central de alertas', corPerigo, () => _abrir(const AlertasPage())),
          ]),
          if (sessao.usuario?.acessoSistema ?? false)
            ..._grade('Financeiro', [
              _Atalho(Icons.chat, 'Cobrança', corSucesso, () => _abrir(const CobrancaPage())),
            ]),
          ..._grade('Ações rápidas', [
            _Atalho(Icons.info_outline, 'Dados de pontos', corPrimaria, () => _abrir(const DadosPontoPage())),
            _Atalho(Icons.highlight_off, 'Pontos sem coletas', corAlerta, () => _abrir(const PontosSemColetaPage())),
            _Atalho(Icons.bar_chart, 'Relatório de coletas', corPrimaria, () => _abrir(const RelatorioColetasPage())),
            _Atalho(Icons.error_outline, 'Pontos com avisos', corPerigo, () => _abrir(const PontosComAvisosPage())),
          ]),
          const SizedBox(height: 12),
          ValueListenableBuilder<SyncEstado>(
            valueListenable: sync.estado,
            builder: (context, e, _) => _CartaoSync(estado: e, onSincronizar: () {
              unawaited(sync.sincronizar(pontosCompleto: true, catalogos: true).catchError((_) {}));
            }),
          ),
        ],
      ),
    );
  }
}

/// Cartão de atalho da grade da tela inicial.
class _Atalho extends StatelessWidget {
  const _Atalho(this.icone, this.rotulo, this.cor, this.aoTocar);

  final IconData icone;
  final String rotulo;
  final Color cor;
  final VoidCallback aoTocar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(6),
      child: Material(
        color: Colors.white,
        elevation: 1,
        borderRadius: BorderRadius.circular(16),
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: aoTocar,
          child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
            IconeSelo(icone, cor: cor),
            const SizedBox(height: 8),
            Text(rotulo, textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
          ]),
        ),
      ),
    );
  }
}

class _CartaoSync extends StatelessWidget {
  const _CartaoSync({required this.estado, required this.onSincronizar});

  final SyncEstado estado;
  final VoidCallback onSincronizar;

  @override
  Widget build(BuildContext context) {
    final temErro = estado.erro != null;
    final cor = temErro ? corPerigo : (estado.pendentes > 0 ? corAlerta : corSucesso);
    final titulo = estado.enviando
        ? 'Enviando ${estado.pendentes} coleta(s)...'
        : estado.pendentes > 0
            ? '${estado.pendentes} coleta(s) aguardando envio'
            : 'Tudo enviado';
    final detalhe = temErro
        ? estado.erro!
        : estado.ultimaSync == null
            ? 'Ainda não sincronizou nesta sessão.'
            : 'Última sincronização: ${formatarDataHora(estado.ultimaSync!)}';

    return Cartao(
      child: Row(children: [
        estado.enviando
            ? const SizedBox(width: 44, height: 44, child: Padding(padding: EdgeInsets.all(10), child: CircularProgressIndicator(strokeWidth: 3)))
            : IconeSelo(temErro ? Icons.cloud_off : Icons.cloud_done_outlined, cor: cor),
        const SizedBox(width: 12),
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(titulo, style: const TextStyle(fontWeight: FontWeight.w600)),
            Text(detalhe, style: const TextStyle(color: Colors.black54, fontSize: 12)),
          ]),
        ),
        IconButton(
          tooltip: 'Sincronizar tudo',
          icon: const Icon(Icons.sync),
          onPressed: estado.sincronizando ? null : onSincronizar,
        ),
      ]),
    );
  }
}
