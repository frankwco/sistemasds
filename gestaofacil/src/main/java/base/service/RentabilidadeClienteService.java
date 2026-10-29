package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import base.modelo.Cliente;
import base.modelo.FechamentoMes;
import base.modelo.RentabilidadeClienteDTO;
import dao.GenericDAO;

/**
 * Agrega os FechamentoMes do período em uma linha por cliente, comparando
 * peso contratado x peso realmente coletado x valor faturado. Não recalcula
 * nada: usa os valores que o fechamento do mês já apurou.
 */
public class RentabilidadeClienteService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<FechamentoMes> daoFechamentoMes;

	public List<RentabilidadeClienteDTO> gerarRelatorio(Integer mes, Integer ano) {

		StringBuilder condicao = new StringBuilder();
		condicao.append("ano=").append(ano);
		if (mes != null && mes > 0) {
			condicao.append(" and mes=").append(mes);
		} else {
			condicao.append(" and mes>0");
		}
		condicao.append(" order by informacaoFinanceira.cliente.nomeRazaoSocial asc, mes asc");

		List<FechamentoMes> fechamentos = daoFechamentoMes.listar(FechamentoMes.class, condicao.toString());

		Map<Long, RentabilidadeClienteDTO> porCliente = new LinkedHashMap<>();

		for (FechamentoMes fm : fechamentos) {
			if (fm.getInformacaoFinanceira() == null || fm.getInformacaoFinanceira().getCliente() == null) {
				continue;
			}

			Cliente cliente = fm.getInformacaoFinanceira().getCliente();

			RentabilidadeClienteDTO dto = porCliente.get(cliente.getId());
			if (dto == null) {
				dto = new RentabilidadeClienteDTO(cliente);
				porCliente.put(cliente.getId(), dto);
			}

			Double pesoContratadoMes = fm.getInformacaoFinanceira().getPesoMensal();
			Double pesoRealMes = (fm.getPesoColetadoGruposContrato() == null ? 0. : fm.getPesoColetadoGruposContrato())
					+ (fm.getPesoColetadoGrupoParte() == null ? 0. : fm.getPesoColetadoGrupoParte());
			Double valorFaturadoMes = fm.getValorFinal();
			String cobraExcedente = fm.getInformacaoFinanceira().getCobraExcedente();

			dto.somarFechamento(pesoContratadoMes, pesoRealMes, valorFaturadoMes, cobraExcedente);
		}

		List<RentabilidadeClienteDTO> resultado = new ArrayList<>(porCliente.values());
		resultado.sort(Comparator.comparing(d -> d.getCliente().getNomeRazaoSocial()));
		return resultado;
	}

}
