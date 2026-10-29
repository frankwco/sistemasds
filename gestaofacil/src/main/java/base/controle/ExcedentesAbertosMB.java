package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.ExcedenteAbertoAgrupadoDTO;
import base.modelo.ExcedenteAbertoDTO;
import base.service.FechamentoMesService;

@ViewScoped
@Named("excedentesAbertosMB")
public class ExcedentesAbertosMB implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private FechamentoMesService fechamentoMesService;

	private List<ExcedenteAbertoAgrupadoDTO> listaExcedentes = new ArrayList<>();
	private Double valorTotalGeral = 0.;
	private Double valorTotalAtivos = 0.;
	private Double valorTotalRenovados = 0.;
	private Double valorTotalOutros = 0.;
	private Double valorTotalTodosContratos = 0.;
	private boolean carregado = false;

	private String filtroSituacao = "TODOS";

	@PostConstruct
	public void inicializar() {
		// A busca real só roda em buscar(), disparado via ajax (p:remoteCommand autoRun)
		// depois que a tela já apareceu com o indicador de carregamento — evita travar o
		// primeiro carregamento da página com a query pesada.
	}

	public void buscar() {
		List<ExcedenteAbertoDTO> itens = fechamentoMesService.listarExcedentesAbertosTodos();
		Set<Long> idsComVersaoAtiva = new HashSet<>(fechamentoMesService.listarIdsContratosComVersaoAtiva());
		Set<Long> idsComVersaoRenovada = new HashSet<>(fechamentoMesService.listarIdsContratosComVersaoRenovada());

		// Agrupa por contrato (idInformacaoFinanceiraUnico): a lista já vem ordenada por
		// grupo/ano/mês, então o último item processado de cada grupo é o mais recente —
		// usamos ele pra representar as datas do contrato na linha agrupada. A classificação
		// (Ativo/Renovado/Outros) do GRUPO, porém, não vem do item mais recente — vem dos
		// conjuntos idsComVersaoAtiva/idsComVersaoRenovada (mesmo critério "alguma versão do
		// histórico é X" usado no card do início), com prioridade Ativo > Renovado > Outros.
		Map<Long, ExcedenteAbertoAgrupadoDTO> grupos = new LinkedHashMap<>();
		for (ExcedenteAbertoDTO item : itens) {
			ExcedenteAbertoAgrupadoDTO grupo = grupos.get(item.getIdInformacaoFinanceiraUnico());
			if (grupo == null) {
				grupo = new ExcedenteAbertoAgrupadoDTO();
				grupo.setIdInformacaoFinanceiraUnico(item.getIdInformacaoFinanceiraUnico());
				grupo.setClienteNome(item.getClienteNome());
				grupo.setClienteTelefone(item.getClienteTelefone());
				String classificacao;
				if (idsComVersaoAtiva.contains(item.getIdInformacaoFinanceiraUnico())) {
					classificacao = "Ativo";
				} else if (idsComVersaoRenovada.contains(item.getIdInformacaoFinanceiraUnico())) {
					classificacao = "Renovado";
				} else {
					classificacao = "Outros";
				}
				grupo.setSituacaoContrato(classificacao);
				grupos.put(item.getIdInformacaoFinanceiraUnico(), grupo);
			}
			grupo.setDataInicial(item.getDataInicial());
			grupo.setDataFinal(item.getDataFinal());
			grupo.adicionarItem(item.getMes(), item.getAno(), item.getValorFaturar());
		}

		listaExcedentes = new ArrayList<>(grupos.values());

		valorTotalGeral = 0.;
		valorTotalAtivos = 0.;
		valorTotalRenovados = 0.;
		valorTotalOutros = 0.;
		for (ExcedenteAbertoAgrupadoDTO grupo : listaExcedentes) {
			if ("Ativo".equals(grupo.getSituacaoContrato())) {
				valorTotalAtivos += grupo.getValorTotal();
				valorTotalGeral += grupo.getValorTotal();
			} else if ("Renovado".equals(grupo.getSituacaoContrato())) {
				valorTotalRenovados += grupo.getValorTotal();
				valorTotalGeral += grupo.getValorTotal();
			} else {
				valorTotalOutros += grupo.getValorTotal();
			}
		}

		valorTotalTodosContratos = fechamentoMesService.buscarTotalExcedentesAbertosTodos();

		carregado = true;
	}

	/**
	 * Lista filtrada exibida na tabela, de acordo com o filtro selecionado
	 * (TODOS, Ativo, Renovado ou Outros). Os totais dos cards de resumo não são afetados por
	 * esse filtro — refletem sempre o total geral.
	 */
	public List<ExcedenteAbertoAgrupadoDTO> getListaExcedentesFiltrada() {
		if ("TODOS".equals(filtroSituacao)) {
			return listaExcedentes;
		}
		List<ExcedenteAbertoAgrupadoDTO> filtrada = new ArrayList<>();
		for (ExcedenteAbertoAgrupadoDTO grupo : listaExcedentes) {
			if (filtroSituacao.equals(grupo.getSituacaoContrato())) {
				filtrada.add(grupo);
			}
		}
		return filtrada;
	}

	public List<ExcedenteAbertoAgrupadoDTO> getListaExcedentes() {
		return listaExcedentes;
	}

	public Double getValorTotalGeral() {
		return valorTotalGeral;
	}

	public Double getValorTotalAtivos() {
		return valorTotalAtivos;
	}

	public Double getValorTotalRenovados() {
		return valorTotalRenovados;
	}

	public Double getValorTotalOutros() {
		return valorTotalOutros;
	}

	public Double getValorTotalTodosContratos() {
		return valorTotalTodosContratos;
	}

	public boolean isCarregado() {
		return carregado;
	}

	public String getFiltroSituacao() {
		return filtroSituacao;
	}

	public void setFiltroSituacao(String filtroSituacao) {
		this.filtroSituacao = filtroSituacao;
	}

}
