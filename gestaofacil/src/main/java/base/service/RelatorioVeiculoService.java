package base.service;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;

import base.modelo.Fornecedor;
import base.modelo.LancamentoVeiculo;
import base.modelo.LeituraOdometro;
import base.modelo.RelatorioCategoriaDTO;
import base.modelo.RelatorioConsumoVeiculoDTO;
import base.modelo.RelatorioCustoVeiculoDTO;
import base.modelo.RelatorioManutencaoItemDTO;
import base.modelo.RelatorioOficinaDTO;
import base.modelo.RelatorioPostoDTO;
import base.modelo.RelatorioTendenciaMensalDTO;
import base.modelo.Veiculo;
import dao.GenericDAO;

/**
 * Indicadores de custo/consumo/manutenção da frota (RF19-27). As consultas
 * trazem só os lançamentos/leituras do período (volume esperado é baixo — é
 * uma frota, não milhões de coletas) e a agregação (SUM/GROUP BY/ranking) é
 * feita em Java: evita JPQL com subconsultas de MIN/MAX agrupado, que não dá
 * pra validar sem um banco à mão pra testar.
 */
public class RelatorioVeiculoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamento;

	@Inject
	private GenericDAO<LeituraOdometro> daoLeitura;

	// instância nova a cada chamada — SimpleDateFormat não é thread-safe pra
	// ser um campo static compartilhado entre requisições concorrentes
	private String formatarData(Date data) {
		return new SimpleDateFormat("yyyy-MM-dd").format(data);
	}

	// inicio/fim nulos = sem limite de data (painel de veículos usa isso pra "todo
	// o histórico"); os relatórios por período continuam passando os dois preenchidos
	private String condicaoPeriodo(Date inicio, Date fim) {
		if (inicio != null && fim != null) {
			return "data between '" + formatarData(inicio) + "' and '" + formatarData(fim) + "'";
		}
		if (inicio != null) {
			return "data >= '" + formatarData(inicio) + "'";
		}
		if (fim != null) {
			return "data <= '" + formatarData(fim) + "'";
		}
		return "1=1";
	}

	private List<LancamentoVeiculo> buscarLancamentos(Date inicio, Date fim, Long veiculoId) {
		String condicao = condicaoPeriodo(inicio, fim);
		if (veiculoId != null) {
			condicao += " and veiculo.id=" + veiculoId;
		}
		return daoLancamento.listar(LancamentoVeiculo.class, condicao);
	}

	private List<LeituraOdometro> buscarLeituras(Date inicio, Date fim, Long veiculoId) {
		String condicao = condicaoPeriodo(inicio, fim);
		if (veiculoId != null) {
			condicao += " and veiculo.id=" + veiculoId;
		}
		return daoLeitura.listar(LeituraOdometro.class, condicao);
	}

	private Map<Long, Long> kmRodadoPorVeiculo(List<LeituraOdometro> leituras) {
		Map<Long, Long> minKm = new HashMap<>();
		Map<Long, Long> maxKm = new HashMap<>();
		for (LeituraOdometro leitura : leituras) {
			if (leitura.getVeiculo() == null || leitura.getKm() == null) {
				continue;
			}
			Long id = leitura.getVeiculo().getId();
			minKm.merge(id, leitura.getKm(), Math::min);
			maxKm.merge(id, leitura.getKm(), Math::max);
		}
		Map<Long, Long> resultado = new HashMap<>();
		for (Long id : maxKm.keySet()) {
			resultado.put(id, maxKm.get(id) - minKm.get(id));
		}
		return resultado;
	}

	private Double media(List<Double> valores) {
		if (valores == null || valores.isEmpty()) {
			return null;
		}
		double soma = 0;
		for (Double valor : valores) {
			soma += valor;
		}
		return soma / valores.size();
	}

	/** RF19 (custo total) + RF20 (custo/km) — já ordenado desc, que é o ranking do RF27. */
	public List<RelatorioCustoVeiculoDTO> custoPorVeiculo(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);
		Map<Long, Long> kmRodado = kmRodadoPorVeiculo(buscarLeituras(inicio, fim, veiculoId));

		Map<Long, Veiculo> veiculos = new LinkedHashMap<>();
		Map<Long, Double> custoTotal = new HashMap<>();
		for (LancamentoVeiculo lancamento : lancamentos) {
			if (lancamento.getVeiculo() == null) {
				continue;
			}
			Long id = lancamento.getVeiculo().getId();
			veiculos.putIfAbsent(id, lancamento.getVeiculo());
			custoTotal.merge(id, lancamento.getValor() == null ? 0d : lancamento.getValor(), Double::sum);
		}

		List<RelatorioCustoVeiculoDTO> resultado = new ArrayList<>();
		for (Long id : veiculos.keySet()) {
			RelatorioCustoVeiculoDTO dto = new RelatorioCustoVeiculoDTO();
			dto.setVeiculo(veiculos.get(id));
			dto.setCustoTotal(custoTotal.getOrDefault(id, 0d));
			Long km = kmRodado.get(id);
			dto.setKmRodado(km);
			if (km != null && km > 0) {
				dto.setCustoPorKm(dto.getCustoTotal() / km);
			}
			resultado.add(dto);
		}

		resultado.sort((a, b) -> {
			if (a.getCustoPorKm() == null && b.getCustoPorKm() == null) {
				return 0;
			}
			if (a.getCustoPorKm() == null) {
				return 1;
			}
			if (b.getCustoPorKm() == null) {
				return -1;
			}
			return Double.compare(b.getCustoPorKm(), a.getCustoPorKm());
		});
		return resultado;
	}

	/** RF21 (km/l por veículo). */
	public List<RelatorioConsumoVeiculoDTO> consumoPorVeiculo(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);
		Map<Long, Long> kmRodado = kmRodadoPorVeiculo(buscarLeituras(inicio, fim, veiculoId));

		Map<Long, Veiculo> veiculos = new LinkedHashMap<>();
		Map<Long, Double> litrosTotal = new HashMap<>();
		for (LancamentoVeiculo lancamento : lancamentos) {
			if (!"Abastecimento".equals(lancamento.getTipo()) || lancamento.getVeiculo() == null) {
				continue;
			}
			Long id = lancamento.getVeiculo().getId();
			veiculos.putIfAbsent(id, lancamento.getVeiculo());
			litrosTotal.merge(id, lancamento.getLitros() == null ? 0d : lancamento.getLitros(), Double::sum);
		}

		List<RelatorioConsumoVeiculoDTO> resultado = new ArrayList<>();
		for (Long id : veiculos.keySet()) {
			RelatorioConsumoVeiculoDTO dto = new RelatorioConsumoVeiculoDTO();
			dto.setVeiculo(veiculos.get(id));
			Long km = kmRodado.get(id);
			Double litros = litrosTotal.get(id);
			dto.setKmRodado(km);
			dto.setLitrosTotal(litros);
			if (km != null && km > 0 && litros != null && litros > 0) {
				dto.setKmPorLitro(km / litros);
			}
			resultado.add(dto);
		}
		return resultado;
	}

	/**
	 * RF21 (por posto)/RF22 — km/l calculado pela diferença de km entre um
	 * abastecimento e o anterior do MESMO veículo (por isso olha todos os
	 * abastecimentos do veículo, não só os do período, pra ter a referência
	 * de km anterior), atribuída ao posto do abastecimento atual.
	 */
	public List<RelatorioPostoDTO> consumoPorPosto(Date inicio, Date fim, Long veiculoId) {
		String condicao = "tipo='Abastecimento' and km is not null";
		if (veiculoId != null) {
			condicao += " and veiculo.id=" + veiculoId;
		}
		condicao += " order by veiculo.id, km asc";
		List<LancamentoVeiculo> todosAbastecimentos = daoLancamento.listar(LancamentoVeiculo.class, condicao);

		Map<String, List<Double>> kmPorLitroPorNome = new LinkedHashMap<>();
		Map<String, List<Double>> precoPorNome = new LinkedHashMap<>();
		Map<String, String> bandeiraPorNome = new HashMap<>();
		Map<String, Integer> quantidadePorNome = new HashMap<>();

		Long veiculoAnteriorId = null;
		Long kmAnterior = null;

		for (LancamentoVeiculo atual : todosAbastecimentos) {
			if (atual.getVeiculo() == null) {
				continue;
			}
			Long veiculoAtualId = atual.getVeiculo().getId();

			if (!veiculoAtualId.equals(veiculoAnteriorId)) {
				kmAnterior = atual.getKm();
				veiculoAnteriorId = veiculoAtualId;
				continue;
			}

			long deltaKm = atual.getKm() - kmAnterior;
			kmAnterior = atual.getKm();

			boolean dentroDoPeriodo = atual.getData() != null && !atual.getData().before(inicio)
					&& !atual.getData().after(fim);

			if (dentroDoPeriodo && deltaKm > 0 && atual.getLitros() != null && atual.getLitros() > 0
					&& atual.getFornecedor() != null) {
				String nome = atual.getFornecedor().getNome();
				kmPorLitroPorNome.computeIfAbsent(nome, k -> new ArrayList<>()).add(deltaKm / atual.getLitros());
				quantidadePorNome.merge(nome, 1, Integer::sum);
				bandeiraPorNome.put(nome, atual.getFornecedor().getBandeira());
				if (atual.getValorLitro() != null) {
					precoPorNome.computeIfAbsent(nome, k -> new ArrayList<>()).add(atual.getValorLitro());
				}
			}
		}

		List<RelatorioPostoDTO> resultado = new ArrayList<>();
		for (String nome : kmPorLitroPorNome.keySet()) {
			RelatorioPostoDTO dto = new RelatorioPostoDTO();
			dto.setNome(nome);
			dto.setBandeira(bandeiraPorNome.get(nome));
			dto.setQuantidadeAbastecimentos(quantidadePorNome.get(nome));
			dto.setKmPorLitroMedio(media(kmPorLitroPorNome.get(nome)));
			dto.setPrecoMedioLitro(media(precoPorNome.get(nome)));
			resultado.add(dto);
		}

		resultado.sort((a, b) -> Double.compare(
				b.getKmPorLitroMedio() == null ? 0 : b.getKmPorLitroMedio(),
				a.getKmPorLitroMedio() == null ? 0 : a.getKmPorLitroMedio()));
		return resultado;
	}

	/** RF24 — despesas por categoria e % do total do período. */
	public List<RelatorioCategoriaDTO> custoPorCategoria(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);

		Map<String, Double> totalPorCategoria = new LinkedHashMap<>();
		double totalGeral = 0;
		for (LancamentoVeiculo lancamento : lancamentos) {
			String categoria = lancamento.getCategoriaDespesa() != null ? lancamento.getCategoriaDespesa().getNome()
					: "Sem categoria";
			double valor = lancamento.getValor() == null ? 0 : lancamento.getValor();
			totalPorCategoria.merge(categoria, valor, Double::sum);
			totalGeral += valor;
		}

		List<RelatorioCategoriaDTO> resultado = new ArrayList<>();
		for (String categoria : totalPorCategoria.keySet()) {
			RelatorioCategoriaDTO dto = new RelatorioCategoriaDTO();
			dto.setCategoria(categoria);
			dto.setValorTotal(totalPorCategoria.get(categoria));
			dto.setPercentual(totalGeral > 0 ? (dto.getValorTotal() / totalGeral) * 100 : 0);
			resultado.add(dto);
		}

		resultado.sort((a, b) -> Double.compare(b.getValorTotal(), a.getValorTotal()));
		return resultado;
	}

	/** RF23 — custo médio e avaliação por oficina, considerando só lançamentos de Serviço. */
	public List<RelatorioOficinaDTO> custoPorOficina(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);

		Map<Long, Fornecedor> oficinas = new LinkedHashMap<>();
		Map<Long, List<Double>> valoresPorOficina = new HashMap<>();
		for (LancamentoVeiculo lancamento : lancamentos) {
			if (!"Serviço".equals(lancamento.getTipo()) || lancamento.getFornecedor() == null) {
				continue;
			}
			Long id = lancamento.getFornecedor().getId();
			oficinas.putIfAbsent(id, lancamento.getFornecedor());
			valoresPorOficina.computeIfAbsent(id, k -> new ArrayList<>())
					.add(lancamento.getValor() == null ? 0d : lancamento.getValor());
		}

		List<RelatorioOficinaDTO> resultado = new ArrayList<>();
		for (Long id : oficinas.keySet()) {
			Fornecedor fornecedor = oficinas.get(id);
			RelatorioOficinaDTO dto = new RelatorioOficinaDTO();
			dto.setNome(fornecedor.getNome());
			dto.setAvaliacao(fornecedor.getAvaliacao());
			List<Double> valores = valoresPorOficina.get(id);
			dto.setQuantidadeServicos(valores.size());
			dto.setCustoMedio(media(valores));
			resultado.add(dto);
		}

		resultado.sort((a, b) -> Double.compare(
				a.getCustoMedio() == null ? 0 : a.getCustoMedio(),
				b.getCustoMedio() == null ? 0 : b.getCustoMedio()));
		return resultado;
	}

	/**
	 * Evolução mensal de custo e custo/km — usado pelo painel dinâmico de
	 * veículos pra mostrar histórico ao longo do tempo (não é RF19-27, esses já
	 * respondem "quanto no período"; aqui é "como isso variou mês a mês").
	 * Km rodado de cada mês é o mesmo max−min de leituras usado nos outros
	 * métodos, só que agrupado por mês em vez de por veículo — mês com uma
	 * leitura só (ou nenhuma) fica sem km/custo-por-km, igual já acontece hoje
	 * num período curto nos outros relatórios.
	 */
	public List<RelatorioTendenciaMensalDTO> tendenciaMensal(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);
		List<LeituraOdometro> leituras = buscarLeituras(inicio, fim, veiculoId);

		SimpleDateFormat chaveMes = new SimpleDateFormat("yyyy-MM");

		Map<String, Double> custoPorMes = new TreeMap<>();
		for (LancamentoVeiculo lancamento : lancamentos) {
			if (lancamento.getData() == null) {
				continue;
			}
			String chave = chaveMes.format(lancamento.getData());
			custoPorMes.merge(chave, lancamento.getValor() == null ? 0d : lancamento.getValor(), Double::sum);
		}

		Map<String, Long> minKmPorMes = new TreeMap<>();
		Map<String, Long> maxKmPorMes = new TreeMap<>();
		for (LeituraOdometro leitura : leituras) {
			if (leitura.getData() == null || leitura.getKm() == null) {
				continue;
			}
			String chave = chaveMes.format(leitura.getData());
			minKmPorMes.merge(chave, leitura.getKm(), Math::min);
			maxKmPorMes.merge(chave, leitura.getKm(), Math::max);
		}

		Set<String> meses = new TreeSet<>();
		meses.addAll(custoPorMes.keySet());
		meses.addAll(maxKmPorMes.keySet());

		List<RelatorioTendenciaMensalDTO> resultado = new ArrayList<>();
		for (String chave : meses) {
			RelatorioTendenciaMensalDTO dto = new RelatorioTendenciaMensalDTO();
			dto.setMesReferencia(chave);
			dto.setCustoTotal(custoPorMes.getOrDefault(chave, 0d));
			Long min = minKmPorMes.get(chave);
			Long max = maxKmPorMes.get(chave);
			Long km = (min != null && max != null) ? max - min : null;
			dto.setKmRodado(km);
			if (km != null && km > 0) {
				dto.setCustoPorKm(dto.getCustoTotal() / km);
			}
			resultado.add(dto);
		}
		return resultado;
	}

	/** RF25/RF26 — cada Serviço do período com o downtime (dataSaida - dataEntrada) em dias. */
	public List<RelatorioManutencaoItemDTO> manutencoes(Date inicio, Date fim, Long veiculoId) {
		List<LancamentoVeiculo> lancamentos = buscarLancamentos(inicio, fim, veiculoId);

		List<RelatorioManutencaoItemDTO> resultado = new ArrayList<>();
		for (LancamentoVeiculo lancamento : lancamentos) {
			if (!"Serviço".equals(lancamento.getTipo())) {
				continue;
			}
			RelatorioManutencaoItemDTO dto = new RelatorioManutencaoItemDTO();
			dto.setVeiculo(lancamento.getVeiculo());
			dto.setTipoManutencao(lancamento.getTipoManutencao());
			dto.setDataEntrada(lancamento.getDataEntrada());
			dto.setDataSaida(lancamento.getDataSaida());
			if (lancamento.getDataEntrada() != null && lancamento.getDataSaida() != null) {
				long diffMs = lancamento.getDataSaida().getTime() - lancamento.getDataEntrada().getTime();
				dto.setDowntimeDias(diffMs / (1000L * 60 * 60 * 24));
			}
			resultado.add(dto);
		}

		resultado.sort((a, b) -> {
			if (a.getDataEntrada() == null && b.getDataEntrada() == null) {
				return 0;
			}
			if (a.getDataEntrada() == null) {
				return 1;
			}
			if (b.getDataEntrada() == null) {
				return -1;
			}
			return b.getDataEntrada().compareTo(a.getDataEntrada());
		});
		return resultado;
	}

}
