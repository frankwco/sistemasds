import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app.dart';
import '../../core/conversao.dart';
import '../../core/estado_consulta.dart';
import '../../core/formato.dart';
import '../../providers.dart';

/// Central de alertas (vencimentos e manutenções por km). Sempre ao vivo, sem cache.
class AlertasPage extends ConsumerStatefulWidget {
  const AlertasPage({super.key});

  @override
  ConsumerState<AlertasPage> createState() => _AlertasPageState();
}

class _AlertasPageState extends ConsumerState<AlertasPage> {
  late Future<List<dynamic>> _futuro = ref.read(apiProvider).alertas();

  void _recarregar() => setState(() => _futuro = ref.read(apiProvider).alertas());

  String _data(Object? v) {
    DateTime? d;
    if (v is num) d = DateTime.fromMillisecondsSinceEpoch(v.toInt());
    if (v is String && v.isNotEmpty) d = DateTime.tryParse(v);
    return d == null ? '' : formatarData(d);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Central de alertas'),
        actions: [IconButton(tooltip: 'Atualizar', icon: const Icon(Icons.refresh), onPressed: _recarregar)],
      ),
      body: CorpoConsulta<List<dynamic>>(
        futuro: _futuro,
        aoTentarDeNovo: _recarregar,
        vazio: (l) => l.isEmpty,
        textoVazio: 'Nenhum alerta no momento',
        construir: (context, alertas) => ListView.separated(
          itemCount: alertas.length,
          separatorBuilder: (_, __) => const Divider(height: 1),
          itemBuilder: (_, i) {
            final a = Map<String, dynamic>.from(alertas[i] as Map);
            final vencido = a['urgencia'] == 'Vencido';
            final venc = _data(a['dataVencimento']);
            final detalhe = [
              texto(a['tipo']),
              if (texto(a['veiculo']).isNotEmpty) texto(a['veiculo']),
              if (texto(a['motorista']).isNotEmpty) texto(a['motorista']),
            ].join(' · ');
            return ListTile(
              leading: Icon(vencido ? Icons.error : Icons.schedule,
                  color: vencido ? corPerigo : (a['urgencia'] == 'Próximo' ? corAlerta : Colors.black45)),
              title: Text(texto(a['descricao'])),
              subtitle: Text([
                detalhe,
                if (venc.isNotEmpty) 'Vencimento: $venc' else if (a['kmAlvo'] != null) 'Km: ${texto(a['kmAtual'])} / ${texto(a['kmAlvo'])}',
              ].join('\n')),
              isThreeLine: true,
            );
          },
        ),
      ),
    );
  }
}
