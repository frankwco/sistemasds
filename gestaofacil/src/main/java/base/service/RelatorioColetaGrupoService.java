package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import base.modelo.ColetaGrupoMensalDTO;
import base.modelo.GrupoResiduo;

/**
 * Monta o relatório de peso coletado por GrupoResiduo, mês a mês, no ano
 * selecionado, comparando com o total do ano anterior (crescimento).
 *
 * Importante: a soma é feita no banco (SUM/GROUP BY), não trazendo os
 * ItensColeta um a um pra somar em Java — uma tabela de coletas com anos de
 * histórico pode ter centenas de milhares de linhas, e carregar cada uma
 * como entidade (mesmo com join fetch) é lento só pelo volume. Cada consulta
 * aqui devolve, no máximo, uma linha por grupo de resíduo.
 */
public class RelatorioColetaGrupoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	public List<ColetaGrupoMensalDTO> gerarRelatorio(Integer ano) {

		int anoAnterior = ano - 1;

		List<GrupoResiduo> grupos = manager
				.createQuery("from GrupoResiduo order by ordem asc", GrupoResiduo.class).getResultList();

		Map<Long, ColetaGrupoMensalDTO> porGrupo = new LinkedHashMap<>();
		for (GrupoResiduo grupo : grupos) {
			porGrupo.put(grupo.getId(), new ColetaGrupoMensalDTO(grupo));
		}

		// Ano selecionado: uma consulta agregada por mês (12 consultas leves,
		// cada uma filtrando por um intervalo de datas indexado).
		for (int mes = 0; mes < 12; mes++) {
			Calendar calInicio = new GregorianCalendar(ano, mes, 1);
			Date inicioMes = calInicio.getTime();
			Date fimMes = new GregorianCalendar(ano, mes, calInicio.getActualMaximum(Calendar.DAY_OF_MONTH))
					.getTime();

			int mesAtual = mes;
			for (Object[] linha : somarPesoPorGrupo(inicioMes, fimMes)) {
				ColetaGrupoMensalDTO dto = porGrupo.get((Long) linha[0]);
				if (dto != null) {
					dto.somarMes(mesAtual, (Double) linha[1]);
				}
			}
		}

		// Ano anterior: só precisamos do total, pra calcular o crescimento.
		Date inicioAnoAnterior = new GregorianCalendar(anoAnterior, Calendar.JANUARY, 1).getTime();
		Date fimAnoAnterior = new GregorianCalendar(anoAnterior, Calendar.DECEMBER, 31).getTime();
		for (Object[] linha : somarPesoPorGrupo(inicioAnoAnterior, fimAnoAnterior)) {
			ColetaGrupoMensalDTO dto = porGrupo.get((Long) linha[0]);
			if (dto != null) {
				dto.somarAnoAnterior((Double) linha[1]);
			}
		}

		return new ArrayList<>(porGrupo.values());
	}

	/** Retorna, para o intervalo de datas informado, [grupoResiduo.id, soma(quantidadeColetada)] por grupo. */
	private List<Object[]> somarPesoPorGrupo(Date dataInicio, Date dataFim) {
		String jpql = "select g.id, sum(i.quantidadeColetada) "
				+ "from ItensColeta i join i.coleta c join i.grupoResiduo g "
				+ "where i.status = true and c.status = true "
				+ "and c.dataColeta between :dataInicio and :dataFim "
				+ "group by g.id";

		TypedQuery<Object[]> query = manager.createQuery(jpql, Object[].class);
		query.setParameter("dataInicio", dataInicio, TemporalType.DATE);
		query.setParameter("dataFim", dataFim, TemporalType.DATE);
		return query.getResultList();
	}

}
