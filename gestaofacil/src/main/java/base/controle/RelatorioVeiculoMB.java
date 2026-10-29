package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.RelatorioCategoriaDTO;
import base.modelo.RelatorioConsumoVeiculoDTO;
import base.modelo.RelatorioCustoVeiculoDTO;
import base.modelo.RelatorioManutencaoItemDTO;
import base.modelo.RelatorioOficinaDTO;
import base.modelo.RelatorioPostoDTO;
import base.modelo.Veiculo;
import base.service.RelatorioVeiculoService;
import dao.GenericDAO;

/**
 * Tela única de indicadores da frota (RF19-27): um filtro (veículo opcional
 * + período) e várias abas de resultado, todas recalculadas juntas em
 * {@link #buscar()} a partir do mesmo {@link RelatorioVeiculoService}.
 */
@ViewScoped
@Named("relatorioVeiculoMB")
public class RelatorioVeiculoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private Date dataInicio;
	private Date dataFim;

	private List<RelatorioCustoVeiculoDTO> listCusto = new ArrayList<>();
	private List<RelatorioConsumoVeiculoDTO> listConsumo = new ArrayList<>();
	private List<RelatorioPostoDTO> listPosto = new ArrayList<>();
	private List<RelatorioCategoriaDTO> listCategoria = new ArrayList<>();
	private List<RelatorioOficinaDTO> listOficina = new ArrayList<>();
	private List<RelatorioManutencaoItemDTO> listManutencao = new ArrayList<>();

	private Long quantidadePreventiva = 0L;
	private Long quantidadeCorretiva = 0L;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private RelatorioVeiculoService relatorioVeiculoService;

	@PostConstruct
	public void inicializar() {
		dataFim = new Date();
		dataInicio = new Date(dataFim.getTime() - (30L * 24 * 60 * 60 * 1000));
	}

	public void buscar() {
		if (dataInicio == null || dataFim == null) {
			return;
		}
		Long veiculoId = veiculo != null ? veiculo.getId() : null;

		listCusto = relatorioVeiculoService.custoPorVeiculo(dataInicio, dataFim, veiculoId);
		listConsumo = relatorioVeiculoService.consumoPorVeiculo(dataInicio, dataFim, veiculoId);
		listPosto = relatorioVeiculoService.consumoPorPosto(dataInicio, dataFim, veiculoId);
		listCategoria = relatorioVeiculoService.custoPorCategoria(dataInicio, dataFim, veiculoId);
		listOficina = relatorioVeiculoService.custoPorOficina(dataInicio, dataFim, veiculoId);
		listManutencao = relatorioVeiculoService.manutencoes(dataInicio, dataFim, veiculoId);

		quantidadePreventiva = 0L;
		quantidadeCorretiva = 0L;
		for (RelatorioManutencaoItemDTO item : listManutencao) {
			if ("Preventiva".equals(item.getTipoManutencao())) {
				quantidadePreventiva++;
			} else if ("Corretiva".equals(item.getTipoManutencao())) {
				quantidadeCorretiva++;
			}
		}
	}

	public Double getPercentualPreventiva() {
		long total = quantidadePreventiva + quantidadeCorretiva;
		return total > 0 ? (quantidadePreventiva * 100.0) / total : null;
	}

	public Double getPercentualCorretiva() {
		long total = quantidadePreventiva + quantidadeCorretiva;
		return total > 0 ? (quantidadeCorretiva * 100.0) / total : null;
	}

	public List<Veiculo> completarVeiculo(String str) {
		List<Veiculo> list = daoVeiculo.listaComStatus(Veiculo.class);
		List<Veiculo> selecionados = new ArrayList<>();
		for (Veiculo cur : list) {
			if (cur.getDescricao().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataFim() {
		return dataFim;
	}

	public void setDataFim(Date dataFim) {
		this.dataFim = dataFim;
	}

	public List<RelatorioCustoVeiculoDTO> getListCusto() {
		return listCusto;
	}

	public List<RelatorioConsumoVeiculoDTO> getListConsumo() {
		return listConsumo;
	}

	public List<RelatorioPostoDTO> getListPosto() {
		return listPosto;
	}

	public List<RelatorioCategoriaDTO> getListCategoria() {
		return listCategoria;
	}

	public List<RelatorioOficinaDTO> getListOficina() {
		return listOficina;
	}

	public List<RelatorioManutencaoItemDTO> getListManutencao() {
		return listManutencao;
	}

	public Long getQuantidadePreventiva() {
		return quantidadePreventiva;
	}

	public Long getQuantidadeCorretiva() {
		return quantidadeCorretiva;
	}

}
