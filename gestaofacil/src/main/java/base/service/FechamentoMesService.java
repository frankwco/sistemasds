package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import base.modelo.CategoriaIndicador;
import base.modelo.ExcedenteAbertoDTO;
import base.modelo.FechamentoMes;
import base.modelo.Tipo;
import dao.GenericDAO;
import util.FormataDataMB;
import util.Formatacoes;
import util.FormatarMB;
import util.Transacional;

public class FechamentoMesService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<FechamentoMes> dao;

	@Inject
	private EntityManager manager;

	@Transacional
	public void inserirAlterar(FechamentoMes tipo) {
		if (tipo.getId() == null) {
			dao.inserir(tipo);
		} else {
			dao.alterar(tipo);
		}
	}

	public String buscarFechamentoAberto(String idUnico) {
		String retorno = "";
		Double total = 0.;
		try {
			Query q = manager.createNativeQuery(
					"SELECT mes, ano, valorFaturar FROM fechamento_mes WHERE idInformacaoFinanceiraUnico = " + idUnico
							+ " and (faturado is false or faturado is null) and valorFaturar > 0 and status is true order by ano, mes");
			List<Object[]> authors = q.getResultList();
			retorno = "<table style=\"width:100%; border: 1px solid gray;\">";

			for (Object[] a : authors) {
				retorno += "<tr><td >" + a[0] + "/" + a[1] + "</td><td style=\"text-align:right\">"
						+ Formatacoes.formatarNumeroDuasCasasDecimais(Double.parseDouble(a[2].toString())) + "</td>";
				total += Double.parseDouble(a[2].toString());
			}

			if (total > 0) {
				retorno += "<tr><td><b>Total:</b></td><td style=\"text-align:right\">"
						+ Formatacoes.formatarNumeroDuasCasasDecimais(total) + "</td>";
			}
			retorno += "</table>";

		} catch (Exception e) {
			e.printStackTrace();
		}
		return retorno;
	}

	/**
	 * Soma o valorFaturar de todos os fechamentos em aberto (não faturados) cujo contrato
	 * (agrupado por idInformacaoFinanceiraUnico) está atualmente com situacaoContrato Ativo ou
	 * Renovado. O join usa uma subquery com DISTINCT porque idInformacaoFinanceiraUnico se
	 * repete em várias linhas de informacoes_financeiras (histórico de renovação) — sem o
	 * DISTINCT o join duplicaria os fechamentos e infla a soma.
	 *
	 * Exclui contratos com formaCobranca='Mensal': pra esses, o valorFaturar do fechamento é
	 * a cobrança mensal recorrente normal, não um excedente/excesso de verdade (que só existe
	 * pra contratos Anual, onde o valor cobrado à parte é além da mensalidade fixa do
	 * contrato). Incluir "Mensal" aqui infla o número — chegava a ~86% do total.
	 */
	public Double buscarTotalExcedentesAbertos() {
		try {
			Query q = manager.createNativeQuery(
					"SELECT COALESCE(SUM(fm.valorFaturar), 0) FROM fechamento_mes fm "
							+ "JOIN informacoes_financeiras infRow ON infRow.id = fm.informacaoFinanceira_id "
							+ "JOIN (SELECT DISTINCT idInformacaoFinanceiraUnico FROM informacoes_financeiras "
							+ "      WHERE situacaoContrato IN ('Ativo', 'Renovado')) inf "
							+ "  ON inf.idInformacaoFinanceiraUnico = fm.idInformacaoFinanceiraUnico "
							+ "WHERE (fm.faturado is false or fm.faturado is null) AND fm.valorFaturar > 0 AND fm.status is true "
							+ "AND infRow.formaCobranca <> 'Mensal'");
			Object resultado = q.getSingleResult();
			return resultado != null ? ((Number) resultado).doubleValue() : 0.;
		} catch (Exception e) {
			e.printStackTrace();
			return 0.;
		}
	}

	/**
	 * Soma o valorFaturar de TODOS os fechamentos em aberto, sem filtrar pela situação do
	 * contrato — inclui Ativo, Renovado, Inativo/Cancelado e até contratos sem situação
	 * definida. Usado só como número de referência geral (contraste com o total de
	 * buscarTotalExcedentesAbertos(), que é só contratos ativos).
	 *
	 * Também exclui formaCobranca='Mensal' — mesmo motivo de buscarTotalExcedentesAbertos().
	 */
	public Double buscarTotalExcedentesAbertosTodos() {
		try {
			Query q = manager.createNativeQuery(
					"SELECT COALESCE(SUM(fm.valorFaturar), 0) FROM fechamento_mes fm "
							+ "JOIN informacoes_financeiras inf ON inf.id = fm.informacaoFinanceira_id "
							+ "WHERE (fm.faturado is false or fm.faturado is null) AND fm.valorFaturar > 0 AND fm.status is true "
							+ "AND inf.formaCobranca <> 'Mensal'");
			Object resultado = q.getSingleResult();
			return resultado != null ? ((Number) resultado).doubleValue() : 0.;
		} catch (Exception e) {
			e.printStackTrace();
			return 0.;
		}
	}

	/**
	 * Soma TUDO que está em aberto no sistema, sem filtro nenhum além dos critérios básicos de
	 * "em aberto" (não faturado, valor > 0, ativo) — inclui excedente (Anual) e faturamento
	 * mensal (Mensal) juntos, de qualquer situação de contrato. É o número mais bruto possível,
	 * usado só como visão geral de referência (ex.: card fixo em fechamentoMes.jsf), sem
	 * depender de nenhuma busca/filtro que o usuário tenha feito na tela.
	 */
	public Double buscarTotalGeralAbertoSistema() {
		try {
			Query q = manager.createNativeQuery(
					"SELECT COALESCE(SUM(valorFaturar), 0) FROM fechamento_mes "
							+ "WHERE (faturado is false or faturado is null) AND valorFaturar > 0 AND status is true");
			Object resultado = q.getSingleResult();
			return resultado != null ? ((Number) resultado).doubleValue() : 0.;
		} catch (Exception e) {
			e.printStackTrace();
			return 0.;
		}
	}

	/**
	 * Soma TUDO que está em aberto (excedente + faturamento mensal juntos, sem excluir Mensal)
	 * dos contratos cujo GRUPO (idInformacaoFinanceiraUnico) tem, em algum ponto do histórico,
	 * a situação informada — mesmo critério "por grupo" de buscarTotalExcedentesAbertos(), só
	 * que sem o filtro de formaCobranca. Usado nos cards fixos "Contratos Ativos"/"Contratos
	 * Renovados" de fechamentoMes.jsf, que ficam ao lado da Visão Geral.
	 */
	public Double buscarTotalGeralAbertoPorSituacao(String situacao) {
		try {
			Query q = manager.createNativeQuery(
					"SELECT COALESCE(SUM(fm.valorFaturar), 0) FROM fechamento_mes fm "
							+ "JOIN (SELECT DISTINCT idInformacaoFinanceiraUnico FROM informacoes_financeiras "
							+ "      WHERE situacaoContrato = '" + situacao + "') inf "
							+ "  ON inf.idInformacaoFinanceiraUnico = fm.idInformacaoFinanceiraUnico "
							+ "WHERE (fm.faturado is false or fm.faturado is null) AND fm.valorFaturar > 0 AND fm.status is true");
			Object resultado = q.getSingleResult();
			return resultado != null ? ((Number) resultado).doubleValue() : 0.;
		} catch (Exception e) {
			e.printStackTrace();
			return 0.;
		}
	}

	/**
	 * Lista detalhada (um registro por mês/contrato pendente) de TODOS os excedentes em
	 * aberto do sistema, sem filtrar pela situação do contrato — inclui Ativo, Renovado,
	 * Inativo/Cancelado e contratos sem situação definida. A classificação de cada grupo
	 * (Ativo/Renovado/Outros) é feita depois, em Java, usando listarIdsContratosComVersaoAtiva()
	 * e listarIdsContratosComVersaoRenovada() — não dá pra confiar no campo situacaoContrato
	 * de cada linha isoladamente, porque ele reflete só a versão específica em que aquele
	 * fechamento pendente foi gerado (via fm.informacaoFinanceira_id), não o grupo inteiro.
	 *
	 * Query nativa em vez de JPQL por dois motivos:
	 * 1) Evita hidratar entidades JPA completas: InformacaoFinanceira e Pessoa têm várias
	 *    associações @ManyToOne/@OneToMany EAGER por padrão (pontoColetaGerarContrato,
	 *    contrato, listaItensInformacoesFinanceirasPontoColeta, cidade, cidadeResponsavel)
	 *    que disparavam uma query extra POR contrato distinto (N+1).
	 * 2) O otimizador do MySQL escolhia "pessoa" como tabela condutora do JOIN (full scan de
	 *    ~3.800 linhas) em vez de partir de fechamento_mes já filtrado (~1.300 linhas), o que
	 *    sozinho levava a query de ~6s para ~0.2s. Só dá pra forçar a ordem de junção certa
	 *    com o hint STRAIGHT_JOIN, que não existe em JPQL — daí a necessidade de SQL nativo.
	 *
	 * Exclui contratos com formaCobranca='Mensal' — mesmo motivo de buscarTotalExcedentesAbertos():
	 * pra esses, valorFaturar é a cobrança mensal recorrente normal, não um excedente de verdade.
	 */
	public List<ExcedenteAbertoDTO> listarExcedentesAbertosTodos() {
		List<ExcedenteAbertoDTO> resultado = new ArrayList<>();
		try {
			Query q = manager.createNativeQuery(
					"SELECT STRAIGHT_JOIN fm.idInformacaoFinanceiraUnico, p.nomeRazaoSocial, p.telefone1, "
							+ "fm.mes, fm.ano, fm.valorFaturar, inf.situacaoContrato, inf.dataInicial, inf.dataFinal "
							+ "FROM fechamento_mes fm "
							+ "JOIN informacoes_financeiras inf ON inf.id = fm.informacaoFinanceira_id "
							+ "JOIN pessoa p ON p.id = inf.cliente_id "
							+ "WHERE fm.status = true AND fm.valorFaturar > 0 "
							+ "AND (fm.faturado = false OR fm.faturado IS NULL) "
							+ "AND inf.formaCobranca <> 'Mensal' "
							+ "ORDER BY fm.idInformacaoFinanceiraUnico, fm.ano, fm.mes");

			@SuppressWarnings("unchecked")
			List<Object[]> linhas = q.getResultList();
			for (Object[] r : linhas) {
				resultado.add(new ExcedenteAbertoDTO(
						r[0] != null ? ((Number) r[0]).longValue() : null,
						(String) r[1],
						(String) r[2],
						r[3] != null ? ((Number) r[3]).intValue() : null,
						r[4] != null ? ((Number) r[4]).intValue() : null,
						r[5] != null ? ((Number) r[5]).doubleValue() : null,
						(String) r[6],
						(Date) r[7],
						(Date) r[8]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}

	/**
	 * IDs de grupo (idInformacaoFinanceiraUnico) que têm, em algum ponto do histórico, uma
	 * versão com situacaoContrato='Ativo' — mesmo critério de buscarTotalExcedentesAbertos(),
	 * usado pra classificar cada linha agrupada de listarExcedentesAbertosTodos() como "Ativo".
	 */
	public List<Long> listarIdsContratosComVersaoAtiva() {
		try {
			return manager.createQuery(
					"SELECT DISTINCT i2.idInformacaoFinanceiraUnico FROM InformacaoFinanceira i2 "
							+ "WHERE i2.situacaoContrato = 'Ativo'",
					Long.class).getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * IDs de grupo que têm, em algum ponto do histórico, uma versão com
	 * situacaoContrato='Renovado' — usado junto com listarIdsContratosComVersaoAtiva() pra
	 * classificar cada linha agrupada: Ativo (se tiver versão Ativo) > Renovado (senão, se
	 * tiver versão Renovado) > Outros (nenhuma das duas — Inativo/Cancelado ou sem situação).
	 */
	public List<Long> listarIdsContratosComVersaoRenovada() {
		try {
			return manager.createQuery(
					"SELECT DISTINCT i2.idInformacaoFinanceiraUnico FROM InformacaoFinanceira i2 "
							+ "WHERE i2.situacaoContrato = 'Renovado'",
					Long.class).getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public List<FechamentoMes> buscarFechamentoMes(Long idCliente) {
		List<FechamentoMes> listaFechamentoMes = listarFechamentoMesPorClienteComJoinFetch(idCliente);
		preencherFechamentosAbertosEmLote(listaFechamentoMes);
		return listaFechamentoMes;
	}

	/**
	 * Carrega fechamentos com JOIN FETCH para evitar N+1 em informacaoFinanceira e cliente.
	 * Os parâmetros mes e ano com valor 0 são ignorados no filtro.
	 */
	public List<FechamentoMes> listarFechamentoMesComJoinFetch(Integer mes, Integer ano, String extraCondicao) {
		StringBuilder jpql = new StringBuilder();
		jpql.append("SELECT fm FROM FechamentoMes fm ");
		jpql.append("JOIN FETCH fm.informacaoFinanceira inf ");
		jpql.append("JOIN FETCH inf.cliente ");
		jpql.append("LEFT JOIN FETCH inf.pontoColetaGerarContrato ");
		jpql.append("LEFT JOIN FETCH inf.contrato ");
		jpql.append("WHERE fm.status = true ");

		if (mes != null && mes != 0) {
			jpql.append("AND fm.mes = ").append(mes).append(" ");
		}
		if (ano != null && ano != 0) {
			jpql.append("AND fm.ano = ").append(ano).append(" ");
		}
		if (extraCondicao != null && !extraCondicao.isEmpty()) {
			jpql.append(extraCondicao);
		}

		try {
			return manager.createQuery(jpql.toString(), FechamentoMes.class).getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Carrega fechamentos de um cliente específico com JOIN FETCH.
	 */
	public List<FechamentoMes> listarFechamentoMesPorClienteComJoinFetch(Long clienteId) {
		try {
			return manager.createQuery(
				"SELECT fm FROM FechamentoMes fm " +
				"JOIN FETCH fm.informacaoFinanceira inf " +
				"JOIN FETCH inf.cliente c " +
				"LEFT JOIN FETCH inf.pontoColetaGerarContrato " +
				"LEFT JOIN FETCH inf.contrato " +
				"WHERE fm.status = true AND c.id = :clienteId " +
				"ORDER BY fm.dataFechamento DESC",
				FechamentoMes.class)
				.setParameter("clienteId", clienteId)
				.getResultList();
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Busca os fechamentos em aberto (não faturados) para uma lista de IDs em UMA ÚNICA query,
	 * em vez de fazer uma query por fechamento (problema N+1).
	 */
	public Map<Long, String> buscarFechamentosAbertosEmLote(List<Long> idsUnicos) {
		Map<Long, String> resultado = new HashMap<>();
		if (idsUnicos == null || idsUnicos.isEmpty()) {
			return resultado;
		}
		try {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Long id : idsUnicos) {
				if (id == null) continue;
				if (!first) sb.append(",");
				sb.append(id);
				first = false;
			}
			if (sb.length() == 0) return resultado;

			Query q = manager.createNativeQuery(
				"SELECT idInformacaoFinanceiraUnico, mes, ano, valorFaturar " +
				"FROM fechamento_mes " +
				"WHERE idInformacaoFinanceiraUnico IN (" + sb.toString() + ") " +
				"AND (faturado is false or faturado is null) " +
				"AND valorFaturar > 0 AND status is true " +
				"ORDER BY idInformacaoFinanceiraUnico, ano, mes");

			List<Object[]> rows = q.getResultList();

			// Agrupar resultados por idInformacaoFinanceiraUnico
			Map<Long, List<Object[]>> agrupado = new HashMap<>();
			for (Object[] row : rows) {
				Long idUnico = ((Number) row[0]).longValue();
				if (!agrupado.containsKey(idUnico)) {
					agrupado.put(idUnico, new ArrayList<>());
				}
				agrupado.get(idUnico).add(row);
			}

			// Montar HTML para cada ID (mesmo formato do método original)
			for (Long idUnico : idsUnicos) {
				if (idUnico == null || resultado.containsKey(idUnico)) continue;

				List<Object[]> items = agrupado.get(idUnico);
				String retorno = "<table style=\"width:100%; border: 1px solid gray;\">";
				Double total = 0.;
				if (items != null) {
					for (Object[] a : items) {
						retorno += "<tr><td >" + a[1] + "/" + a[2] + "</td><td style=\"text-align:right\">" 
								+ Formatacoes.formatarNumeroDuasCasasDecimais(Double.parseDouble(a[3].toString())) + "</td>";
						total += Double.parseDouble(a[3].toString());
					}
				}
				if (total > 0) {
					retorno += "<tr><td><b>Total:</b></td><td style=\"text-align:right\">" 
							+ Formatacoes.formatarNumeroDuasCasasDecimais(total) + "</td>";
				}
				retorno += "</table>";
				resultado.put(idUnico, retorno);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}

	/**
	 * Soma total (não o HTML, o número) do valorFaturar em aberto de cada contrato em lote —
	 * mesmo critério de buscarFechamentosAbertosEmLote(), usado quando só o total numérico
	 * importa (ex.: card de resumo), sem precisar montar a tabela HTML por mês.
	 */
	/**
	 * @param formaCobranca 'Anual', 'Mensal' ou null (sem filtrar por forma de cobrança).
	 *                      Cada fechamento é filtrado pela formaCobranca da versão específica
	 *                      de informacaoFinanceira à qual ele está ligado (fm.informacaoFinanceira_id).
	 */
	public Map<Long, Double> buscarTotaisAbertosEmLote(List<Long> idsUnicos, String formaCobranca) {
		Map<Long, Double> resultado = new HashMap<>();
		if (idsUnicos == null || idsUnicos.isEmpty()) {
			return resultado;
		}
		try {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Long id : idsUnicos) {
				if (id == null) continue;
				if (!first) sb.append(",");
				sb.append(id);
				first = false;
			}
			if (sb.length() == 0) return resultado;

			String filtroForma = formaCobranca != null ? "AND inf.formaCobranca = '" + formaCobranca + "' " : "";

			Query q = manager.createNativeQuery(
					"SELECT fm.idInformacaoFinanceiraUnico, SUM(fm.valorFaturar) " +
					"FROM fechamento_mes fm " +
					"JOIN informacoes_financeiras inf ON inf.id = fm.informacaoFinanceira_id " +
					"WHERE fm.idInformacaoFinanceiraUnico IN (" + sb.toString() + ") " +
					"AND (fm.faturado is false or fm.faturado is null) " +
					"AND fm.valorFaturar > 0 AND fm.status is true " +
					filtroForma +
					"GROUP BY fm.idInformacaoFinanceiraUnico");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = q.getResultList();
			for (Object[] row : rows) {
				resultado.put(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultado;
	}

	/**
	 * Preenche o campo transient fechamentosAbertos em lote para toda a lista.
	 */
	public void preencherFechamentosAbertosEmLote(List<FechamentoMes> lista) {
		if (lista == null || lista.isEmpty()) return;
		List<Long> ids = new ArrayList<>();
		for (FechamentoMes ff : lista) {
			if (ff.getIdInformacaoFinanceiraUnico() != null) {
				ids.add(ff.getIdInformacaoFinanceiraUnico());
			}
		}
		Map<Long, String> mapa = buscarFechamentosAbertosEmLote(ids);
		for (FechamentoMes ff : lista) {
			String html = mapa.get(ff.getIdInformacaoFinanceiraUnico());
			ff.setFechamentosAbertos(html != null ? html : "");
		}
	}

}
