package base.service;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import base.modelo.InformacaoFinanceira;
import base.modelo.PainelExecutivoDTO;

/**
 * Monta os indicadores do Painel Executivo. Todas as consultas aqui são
 * agregadas (COUNT/SUM) direto no banco — nunca trazemos ItensColeta,
 * FechamentoMes ou InformacaoFinanceira linha a linha pra somar em Java (foi
 * isso que deixou o relatório de Coletas por Grupo lento antes de corrigir).
 */
public class RelatorioPainelExecutivoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	public PainelExecutivoDTO gerarRelatorio(Integer mes, Integer ano) {

		int anoAnterior = ano - 1;

		PainelExecutivoDTO dto = new PainelExecutivoDTO();

		dto.setContratosAtivos(contarContratosAtivos());
		dto.setPontosColetaAtivos(contarPontosColetaAtivos());
		dto.setContratosNovosAno(contarContratosNovosNoAno(ano));
		dto.setContratosEncerradosAno(contarContratosEncerradosNoAno(ano));

		dto.setFaturamentoMes(somarFaturamento(mes, ano));
		dto.setFaturamentoMesAnoAnterior(somarFaturamento(mes, anoAnterior));
		dto.setFaturamentoAno(somarFaturamentoAno(ano));
		dto.setFaturamentoAnoAnterior(somarFaturamentoAno(anoAnterior));

		Calendar calInicioMes = new GregorianCalendar(ano, mes - 1, 1);
		Date inicioMes = calInicioMes.getTime();
		Date fimMes = new GregorianCalendar(ano, mes - 1, calInicioMes.getActualMaximum(Calendar.DAY_OF_MONTH))
				.getTime();

		Calendar calInicioMesAnoAnterior = new GregorianCalendar(anoAnterior, mes - 1, 1);
		Date inicioMesAnoAnterior = calInicioMesAnoAnterior.getTime();
		Date fimMesAnoAnterior = new GregorianCalendar(anoAnterior, mes - 1,
				calInicioMesAnoAnterior.getActualMaximum(Calendar.DAY_OF_MONTH)).getTime();

		Date inicioAno = new GregorianCalendar(ano, Calendar.JANUARY, 1).getTime();
		Date fimAno = new GregorianCalendar(ano, Calendar.DECEMBER, 31).getTime();

		Date inicioAnoAnterior = new GregorianCalendar(anoAnterior, Calendar.JANUARY, 1).getTime();
		Date fimAnoAnterior = new GregorianCalendar(anoAnterior, Calendar.DECEMBER, 31).getTime();

		dto.setPesoColetadoMes(somarPesoColetado(inicioMes, fimMes));
		dto.setPesoColetadoMesAnoAnterior(somarPesoColetado(inicioMesAnoAnterior, fimMesAnoAnterior));
		dto.setPesoColetadoAno(somarPesoColetado(inicioAno, fimAno));
		dto.setPesoColetadoAnoAnterior(somarPesoColetado(inicioAnoAnterior, fimAnoAnterior));

		List<InformacaoFinanceira> semFechamento = listarContratosAtivosSemFechamento(mes, ano, fimMes);
		dto.setContratosSemFechamentoLista(semFechamento);
		dto.setContratosSemFechamentoMes((long) semFechamento.size());

		return dto;
	}

	private Long contarContratosAtivos() {
		return manager.createQuery(
				"select count(i) from InformacaoFinanceira i where i.status = true and i.situacaoContrato = 'Ativo'",
				Long.class).getSingleResult();
	}

	private Long contarPontosColetaAtivos() {
		return manager.createQuery("select count(p) from PontoColeta p where p.status = true", Long.class)
				.getSingleResult();
	}

	private Long contarContratosNovosNoAno(int ano) {
		Date inicio = new GregorianCalendar(ano, Calendar.JANUARY, 1).getTime();
		Date fim = new GregorianCalendar(ano, Calendar.DECEMBER, 31).getTime();

		// "Novo" = primeiro contrato (InformacaoFinanceira) do cliente. Uma
		// renovação gera um novo registro pro mesmo cliente com dataInicial
		// nova, então contar todo registro com dataInicial no ano inflava
		// muito o número (contava renovação como cliente novo).
		TypedQuery<Long> query = manager.createQuery(
				"select count(distinct i.cliente) from InformacaoFinanceira i "
				+ "where i.status = true and i.dataInicial between :inicio and :fim "
				+ "and i.dataInicial = (select min(i2.dataInicial) from InformacaoFinanceira i2 "
				+ "where i2.cliente = i.cliente and i2.status = true)",
				Long.class);
		query.setParameter("inicio", inicio, TemporalType.DATE);
		query.setParameter("fim", fim, TemporalType.DATE);
		return query.getSingleResult();
	}

	private Long contarContratosEncerradosNoAno(int ano) {
		Date inicio = new GregorianCalendar(ano, Calendar.JANUARY, 1).getTime();
		Date fim = new GregorianCalendar(ano, Calendar.DECEMBER, 31).getTime();

		TypedQuery<Long> query = manager.createQuery("select count(i) from InformacaoFinanceira i "
				+ "where i.status = true and i.situacaoContrato = 'Inativo' and i.dataFinal between :inicio and :fim",
				Long.class);
		query.setParameter("inicio", inicio, TemporalType.DATE);
		query.setParameter("fim", fim, TemporalType.DATE);
		return query.getSingleResult();
	}

	private Double somarFaturamento(int mes, int ano) {
		TypedQuery<Double> query = manager.createQuery(
				"select sum(f.valorFinal) from FechamentoMes f where f.status = true and f.ano = :ano and f.mes = :mes",
				Double.class);
		query.setParameter("ano", ano);
		query.setParameter("mes", mes);
		Double resultado = query.getSingleResult();
		return resultado == null ? 0. : resultado;
	}

	private Double somarFaturamentoAno(int ano) {
		TypedQuery<Double> query = manager.createQuery(
				"select sum(f.valorFinal) from FechamentoMes f where f.status = true and f.ano = :ano", Double.class);
		query.setParameter("ano", ano);
		Double resultado = query.getSingleResult();
		return resultado == null ? 0. : resultado;
	}

	/**
	 * Contratos ativos que ainda não tiveram o fechamento do mês feito no
	 * período selecionado. O peso coletado desses clientes já entra na conta
	 * (vem direto da coleta física), mas o faturamento deles só entra depois
	 * que o fechamento é feito — enquanto isso, o R$/kg do período fica
	 * artificialmente baixo.
	 *
	 * A comparação é feita por idInformacaoFinanceiraUnico (não pelo
	 * relacionamento informacaoFinanceira direto): quando um contrato é
	 * renovado, o FechamentoMes antigo continua apontando pro registro
	 * anterior (agora "Renovado"), e é o idInformacaoFinanceiraUnico que se
	 * mantém estável entre as renovações — é assim que o próprio
	 * FechamentoMesMB.fazerFechamentoMes reconhece "esse contrato já tem
	 * fechamento nesse mês".
	 *
	 * Só entram contratos com dataInicial até o último dia do mês
	 * selecionado, igual à regra que o fechamento em lote ("todos") usa pra
	 * decidir quem é elegível — senão um contrato que só começou depois do
	 * período apareceria aqui como "sem fechamento" por engano.
	 *
	 * Traz a lista (não só a contagem) com o cliente já carregado via join
	 * fetch, pra tela poder mostrar o nome de quem está faltando fechar. A
	 * lista tende a ser pequena (poucos contratos por mês), então trazer as
	 * entidades aqui não tem o problema de volume do relatório de Coletas por
	 * Grupo.
	 */
	private List<InformacaoFinanceira> listarContratosAtivosSemFechamento(int mes, int ano, Date fimMes) {
		TypedQuery<InformacaoFinanceira> query = manager.createQuery("select i from InformacaoFinanceira i "
				+ "join fetch i.cliente c "
				+ "where i.status = true and i.situacaoContrato = 'Ativo' and i.dataInicial <= :fimMes "
				+ "and not exists (select 1 from FechamentoMes f "
				+ "where f.idInformacaoFinanceiraUnico = i.idInformacaoFinanceiraUnico "
				+ "and f.mes = :mes and f.ano = :ano and f.status = true) "
				+ "order by c.nomeRazaoSocial asc",
				InformacaoFinanceira.class);
		query.setParameter("fimMes", fimMes, TemporalType.DATE);
		query.setParameter("mes", mes);
		query.setParameter("ano", ano);
		return query.getResultList();
	}

	/**
	 * Soma o peso coletado no período, excluindo clientes marcados com
	 * ocultarRelatorio = true — mesmo critério usado pelo relatório oficial
	 * "Quantidade Coletada no Período" (quantidade_coletada_periodo.jrxml),
	 * pra esse número bater exatamente com o que o cliente pode conferir
	 * naquele relatório.
	 *
	 * A exclusão é feita com "pontoColeta.id not in (subconsulta)" em vez de
	 * dar join direto até Cliente: testado no banco, o join direto faz o
	 * MySQL escolher um plano que varre a tabela cliente inteira antes de
	 * filtrar por data (17s pro ano inteiro); com a subconsulta (poucos
	 * clientes ocultos) o mesmo resultado sai em menos de 1s.
	 */
	private Double somarPesoColetado(Date dataInicio, Date dataFim) {
		TypedQuery<Double> query = manager.createQuery("select sum(i.quantidadeColetada) from ItensColeta i "
				+ "join i.coleta c "
				+ "where i.status = true and c.status = true "
				+ "and c.dataColeta between :dataInicio and :dataFim "
				+ "and c.pontoColeta.id not in ("
				+ "  select pc.id from PontoColeta pc "
				+ "  where pc.cliente.id in (select cli.id from Cliente cli where cli.ocultarRelatorio = true)"
				+ ")",
				Double.class);
		query.setParameter("dataInicio", dataInicio, TemporalType.DATE);
		query.setParameter("dataFim", dataFim, TemporalType.DATE);
		Double resultado = query.getSingleResult();
		return resultado == null ? 0. : resultado;
	}

}
