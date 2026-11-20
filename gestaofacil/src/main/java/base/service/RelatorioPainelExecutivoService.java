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
 *
 * "Acumulado" = de janeiro até o mês selecionado, no ano selecionado e no
 * ano anterior (mesma quantidade de meses dos dois lados).
 */
public class RelatorioPainelExecutivoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	public PainelExecutivoDTO gerarRelatorio(Integer mes, Integer ano) {

		int anoAnterior = ano - 1;

		Date inicioMes = new GregorianCalendar(ano, mes - 1, 1).getTime();
		Date fimMes = ultimoDiaDoMes(mes, ano);
		Date inicioMesAnoAnterior = new GregorianCalendar(anoAnterior, mes - 1, 1).getTime();
		Date fimMesAnoAnterior = ultimoDiaDoMes(mes, anoAnterior);
		Date inicioAno = new GregorianCalendar(ano, Calendar.JANUARY, 1).getTime();
		Date inicioAnoAnterior = new GregorianCalendar(anoAnterior, Calendar.JANUARY, 1).getTime();

		PainelExecutivoDTO dto = new PainelExecutivoDTO();

		dto.setContratosAtivos(contarContratosAtivos());
		dto.setPontosColetaAtivos(contarPontosColetaAtivos());
		dto.setClientesNovos(contarClientesNovos(inicioAno, fimMes));
		dto.setClientesSaida(contarClientesQueSairam(inicioAno, fimMes));

		preencherFaturamento(dto, mes, ano);

		dto.setPesoColetadoMes(somarPesoColetado(inicioMes, fimMes));
		dto.setPesoColetadoMesAnoAnterior(somarPesoColetado(inicioMesAnoAnterior, fimMesAnoAnterior));
		dto.setPesoColetadoAno(somarPesoColetado(inicioAno, fimMes));
		dto.setPesoColetadoAnoAnterior(somarPesoColetado(inicioAnoAnterior, fimMesAnoAnterior));

		List<InformacaoFinanceira> semFechamento = listarContratosAtivosSemFechamento(mes, ano, fimMes);
		dto.setContratosSemFechamentoLista(semFechamento);
		dto.setContratosSemFechamentoMes((long) semFechamento.size());

		return dto;
	}

	private static Date ultimoDiaDoMes(int mes, int ano) {
		Calendar cal = new GregorianCalendar(ano, mes - 1, 1);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal.getTime();
	}

	/**
	 * Contratos ativos hoje, contando cada contrato uma vez só (todas as
	 * versões de renovação compartilham o idInformacaoFinanceiraUnico).
	 * Registros antigos sem idInformacaoFinanceiraUnico contam pelo próprio id.
	 */
	private Long contarContratosAtivos() {
		Number total = (Number) manager.createNativeQuery(
				"SELECT COUNT(DISTINCT COALESCE(idInformacaoFinanceiraUnico, id)) FROM informacoes_financeiras "
						+ "WHERE status = 1 AND situacaoContrato = 'Ativo'")
				.getSingleResult();
		return total == null ? 0L : total.longValue();
	}

	/**
	 * Pontos atendidos hoje: ponto ativo vinculado a pelo menos um contrato
	 * ativo (mesmo critério da lista de pontos do app). Contar só
	 * PontoColeta.status incluía centenas de pontos cujo contrato foi
	 * inativado/renovado sem continuação, mas cujo cadastro nunca foi inativado.
	 */
	private Long contarPontosColetaAtivos() {
		return manager.createQuery(
				"select count(distinct pc.id) from ItensInformacaoFinanceiraPontoColeta iif "
				+ "join iif.pontoColeta pc join iif.informacoesFinanceiras iff "
				+ "where iff.status = true and pc.status = true and iff.situacaoContrato = 'Ativo'",
				Long.class).getSingleResult();
	}

	/**
	 * "Novo" = cliente cujo PRIMEIRO contrato começou no período. Uma renovação
	 * gera outro registro pro mesmo cliente com dataInicial nova, então contar
	 * todo registro com dataInicial no período contaria renovação como cliente
	 * novo. Entram também os contratos já inativados (status = false): um
	 * cliente que entrou e saiu no mesmo ano continua sendo uma entrada.
	 */
	private Long contarClientesNovos(Date inicio, Date fim) {
		TypedQuery<Long> query = manager.createQuery(
				"select count(distinct i.cliente) from InformacaoFinanceira i "
				+ "where (i.status = true or i.situacaoContrato = 'Inativo') "
				+ "and i.dataInicial between :inicio and :fim "
				+ "and i.dataInicial = (select min(i2.dataInicial) from InformacaoFinanceira i2 "
				+ "where i2.cliente = i.cliente and (i2.status = true or i2.situacaoContrato = 'Inativo'))",
				Long.class);
		query.setParameter("inicio", inicio, TemporalType.DATE);
		query.setParameter("fim", fim, TemporalType.DATE);
		return query.getSingleResult();
	}

	/**
	 * Clientes que saíram no período: o último contrato inativado do cliente
	 * caiu no período e ele não tem nenhum contrato ativo hoje.
	 *
	 * Inativar grava status = false (InformacoesFinanceirasMB.inativar), então
	 * aqui NÃO se filtra status = true (era por isso que essa contagem dava
	 * sempre zero). A data usada é dataInativacao; contratos inativados antes
	 * desse campo existir usam a dataFinal do contrato como aproximação.
	 */
	private Long contarClientesQueSairam(Date inicio, Date fim) {
		TypedQuery<Long> query = manager.createQuery(
				"select count(distinct i.cliente) from InformacaoFinanceira i "
				+ "where i.situacaoContrato = 'Inativo' "
				+ "and coalesce(i.dataInativacao, i.dataFinal) between :inicio and :fim "
				+ "and not exists (select a.id from InformacaoFinanceira a "
				+ "  where a.cliente = i.cliente and a.status = true and a.situacaoContrato = 'Ativo') "
				+ "and not exists (select b.id from InformacaoFinanceira b "
				+ "  where b.cliente = i.cliente and b.situacaoContrato = 'Inativo' "
				+ "  and coalesce(b.dataInativacao, b.dataFinal) > coalesce(i.dataInativacao, i.dataFinal))",
				Long.class);
		query.setParameter("inicio", inicio, TemporalType.DATE);
		query.setParameter("fim", fim, TemporalType.DATE);
		return query.getSingleResult();
	}

	/**
	 * Soma o valor apurado (valorFinal) dos fechamentos de janeiro até o mês
	 * selecionado, no ano selecionado e no anterior, numa única consulta
	 * agrupada por ano/mês. Para cada mês sai também a soma sem os clientes
	 * com ocultarRelatorio = true, que é a base usada no R$/kg (o peso
	 * coletado já exclui esses clientes).
	 */
	private void preencherFaturamento(PainelExecutivoDTO dto, int mes, int ano) {
		int anoAnterior = ano - 1;

		@SuppressWarnings("unchecked")
		List<Object[]> linhas = manager.createNativeQuery(
				"SELECT fm.ano, fm.mes, SUM(fm.valorFinal), "
						+ "SUM(CASE WHEN c.ocultarRelatorio = 1 THEN 0 ELSE fm.valorFinal END) "
						+ "FROM fechamento_mes fm "
						+ "LEFT JOIN informacoes_financeiras inf ON inf.id = fm.informacaoFinanceira_id "
						+ "LEFT JOIN cliente c ON c.id = inf.cliente_id "
						+ "WHERE fm.status = 1 AND fm.ano IN (?1, ?2) AND fm.mes <= ?3 "
						+ "GROUP BY fm.ano, fm.mes")
				.setParameter(1, ano).setParameter(2, anoAnterior).setParameter(3, mes).getResultList();

		double mesAtual = 0, mesAnterior = 0, acumAtual = 0, acumAnterior = 0;
		double mesAtualBase = 0, mesAnteriorBase = 0, acumAtualBase = 0, acumAnteriorBase = 0;

		for (Object[] l : linhas) {
			int anoLinha = ((Number) l[0]).intValue();
			int mesLinha = ((Number) l[1]).intValue();
			double total = l[2] == null ? 0 : ((Number) l[2]).doubleValue();
			double base = l[3] == null ? 0 : ((Number) l[3]).doubleValue();

			if (anoLinha == ano) {
				acumAtual += total;
				acumAtualBase += base;
				if (mesLinha == mes) {
					mesAtual = total;
					mesAtualBase = base;
				}
			} else {
				acumAnterior += total;
				acumAnteriorBase += base;
				if (mesLinha == mes) {
					mesAnterior = total;
					mesAnteriorBase = base;
				}
			}
		}

		dto.setFaturamentoMes(mesAtual);
		dto.setFaturamentoMesAnoAnterior(mesAnterior);
		dto.setFaturamentoAno(acumAtual);
		dto.setFaturamentoAnoAnterior(acumAnterior);
		dto.setFaturamentoMesBasePeso(mesAtualBase);
		dto.setFaturamentoMesAnoAnteriorBasePeso(mesAnteriorBase);
		dto.setFaturamentoAnoBasePeso(acumAtualBase);
		dto.setFaturamentoAnoAnteriorBasePeso(acumAnteriorBase);
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
	 * Só entram contratos que já existiam no mês selecionado: alguma versão do
	 * contrato (não necessariamente a atual) começou até o último dia do mês.
	 * Olhar só a dataInicial da versão atual deixava de fora, em meses
	 * passados, contratos que foram renovados depois.
	 */
	private List<InformacaoFinanceira> listarContratosAtivosSemFechamento(int mes, int ano, Date fimMes) {
		TypedQuery<InformacaoFinanceira> query = manager.createQuery("select i from InformacaoFinanceira i "
				+ "join fetch i.cliente c "
				+ "where i.status = true and i.situacaoContrato = 'Ativo' "
				+ "and (i.dataInicial <= :fimMes or exists (select v.id from InformacaoFinanceira v "
				+ "  where v.idInformacaoFinanceiraUnico = i.idInformacaoFinanceiraUnico and v.dataInicial <= :fimMes)) "
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
