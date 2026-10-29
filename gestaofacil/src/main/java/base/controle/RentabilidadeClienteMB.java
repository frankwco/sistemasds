package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.RentabilidadeClienteDTO;
import base.service.RentabilidadeClienteService;

@ViewScoped
@Named("rentabilidadeClienteMB")
public class RentabilidadeClienteMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer mes = 0;
	private Integer ano;

	private List<RentabilidadeClienteDTO> listaRentabilidade;

	private Double totalPesoContratado = 0.;
	private Double totalPesoReal = 0.;
	private Double totalValorFaturado = 0.;

	@Inject
	private RentabilidadeClienteService rentabilidadeClienteService;

	@PostConstruct
	public void inicializar() {
		ano = Calendar.getInstance().get(Calendar.YEAR);
		buscar();
	}

	public void buscar() {
		listaRentabilidade = rentabilidadeClienteService.gerarRelatorio(mes, ano);

		totalPesoContratado = 0.;
		totalPesoReal = 0.;
		totalValorFaturado = 0.;

		for (RentabilidadeClienteDTO dto : listaRentabilidade) {
			totalPesoContratado += dto.getPesoContratado();
			totalPesoReal += dto.getPesoReal();
			totalValorFaturado += dto.getValorFaturado();
		}
	}

	public List<Integer> getAnosDisponiveis() {
		int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> anos = new ArrayList<>();
		for (int a = anoAtual + 1; a >= anoAtual - 6; a--) {
			anos.add(a);
		}
		return anos;
	}

	public Integer getMes() {
		return mes;
	}

	public void setMes(Integer mes) {
		this.mes = mes;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public List<RentabilidadeClienteDTO> getListaRentabilidade() {
		return listaRentabilidade;
	}

	public Double getTotalPesoContratado() {
		return totalPesoContratado;
	}

	public Double getTotalPesoReal() {
		return totalPesoReal;
	}

	public Double getTotalValorFaturado() {
		return totalValorFaturado;
	}

}
