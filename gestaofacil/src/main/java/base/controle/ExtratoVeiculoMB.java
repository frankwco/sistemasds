package base.controle;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.LancamentoVeiculo;
import base.modelo.Veiculo;
import dao.GenericDAO;

/**
 * Extrato unificado do veículo (RF30) — como despesa/abastecimento/serviço
 * já são um único {@link LancamentoVeiculo}, a "combinação numa única linha
 * do tempo" é só uma listagem ordenada por data, sem UNION.
 */
@ViewScoped
@Named("extratoVeiculoMB")
public class ExtratoVeiculoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private Date dataInicio;
	private Date dataFim;
	private List<LancamentoVeiculo> listExtrato;
	private Double totalPeriodo;

	@Inject
	private GenericDAO<LancamentoVeiculo> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@PostConstruct
	public void inicializar() {
		dataFim = new Date();
		dataInicio = new Date(dataFim.getTime() - (30L * 24 * 60 * 60 * 1000));
		listExtrato = new ArrayList<>();
		totalPeriodo = 0d;
	}

	public void buscar() {
		listExtrato = new ArrayList<>();
		totalPeriodo = 0d;

		if (veiculo == null || dataInicio == null || dataFim == null) {
			return;
		}

		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
		listExtrato = dao.listar(LancamentoVeiculo.class,
				"veiculo.id=" + veiculo.getId() + " and data between '" + formato.format(dataInicio) + "' and '"
						+ formato.format(dataFim) + "' order by data desc");

		for (LancamentoVeiculo item : listExtrato) {
			if (item.getValor() != null) {
				totalPeriodo += item.getValor();
			}
		}
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

	public List<LancamentoVeiculo> getListExtrato() {
		return listExtrato;
	}

	public void setListExtrato(List<LancamentoVeiculo> listExtrato) {
		this.listExtrato = listExtrato;
	}

	public Double getTotalPeriodo() {
		return totalPeriodo;
	}

	public void setTotalPeriodo(Double totalPeriodo) {
		this.totalPeriodo = totalPeriodo;
	}

}
