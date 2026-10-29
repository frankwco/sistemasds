import '../core/conversao.dart';
import '../core/formato.dart';

const combustiveis = ['Diesel S10', 'Diesel S500', 'Gasolina', 'Etanol', 'GNV', 'Arla 32', 'Outro'];

/// Casa o texto da IA com uma das opções fixas de combustível; null se não achar nada parecido.
String? normalizarCombustivel(String? valor) {
  if (valor == null || valor.isEmpty) return null;
  final busca = valor.toLowerCase();
  for (final c in combustiveis) {
    final cm = c.toLowerCase();
    if (cm == busca || busca.contains(cm) || cm.contains(busca)) return c;
  }
  return busca.contains('diesel') ? 'Diesel S10' : null;
}

/// Só letras e números em minúsculo: "BCW", "bcw-3c49" e "BCW 3C49" casam igual.
String normalizarPlaca(String? valor) => (valor ?? '').toLowerCase().replaceAll(RegExp(r'[^a-z0-9]'), '');

/// Com dois dos três valores (litros, valor/litro, total) preenchidos, calcula o terceiro.
({double? valor, double? valorLitro}) recalcularAbastecimento({double? litros, double? valorLitro, double? valor}) {
  if (litros != null && litros > 0) {
    if (valorLitro != null && valor == null) {
      valor = (litros * valorLitro * 100).round() / 100;
    } else if (valor != null && valorLitro == null) {
      valorLitro = (valor / litros * 1000).round() / 1000;
    }
  }
  return (valor: valor, valorLitro: valorLitro);
}

/// Veículo da lista que corresponde ao texto da IA (placa parcial ou trecho da descrição).
Map<String, dynamic>? casarVeiculo(List<Map<String, dynamic>> veiculos, String busca) {
  final placaBusca = normalizarPlaca(busca);
  final textoBusca = busca.toLowerCase();
  for (final v in veiculos) {
    final placa = normalizarPlaca(texto(v['placa']));
    final descricao = texto(v['descricao']).toLowerCase();
    final casouPlaca = placa.isNotEmpty && placaBusca.isNotEmpty && placa.contains(placaBusca);
    final casouDescricao = descricao.isNotEmpty && descricao.contains(textoBusca);
    if (casouPlaca || casouDescricao) return v;
  }
  return null;
}

/// Item da lista cujo [campo] contém o texto da IA, ou é contido por ele (sem acento e sem caixa).
Map<String, dynamic>? casarPorNome(List<Map<String, dynamic>> itens, String busca, {String campo = 'nome'}) {
  final b = normalizar(busca);
  if (b.isEmpty) return null;
  for (final i in itens) {
    final n = normalizar(i[campo]);
    if (n.isNotEmpty && (n.contains(b) || b.contains(n))) return i;
  }
  return null;
}

/// Categoria pelo nome exato (sem caixa).
Map<String, dynamic>? casarCategoria(List<Map<String, dynamic>> categorias, String busca) {
  final b = busca.toLowerCase();
  for (final c in categorias) {
    if (texto(c['nome']).toLowerCase() == b) return c;
  }
  return null;
}
