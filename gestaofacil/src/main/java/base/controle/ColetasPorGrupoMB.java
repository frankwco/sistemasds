package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.ColetaGrupoMensalDTO;
import base.service.RelatorioColetaGrupoService;

@ViewScoped
@Named("coletasPorGrupoMB")
public class ColetasPorGrupoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String[] NOMES_MESES = { "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set",
			"Out", "Nov", "Dez" };

	private Integer ano;

	private List<ColetaGrupoMensalDTO> linhas;

	private Double totalAno = 0.;
	private Double totalAnoAnterior = 0.;
	private ColetaGrupoMensalDTO grupoMaiorVolume;

	@Inject
	private RelatorioColetaGrupoService relatorioColetaGrupoService;

	@PostConstruct
	public void inicializar() {
		ano = Calendar.getInstance().get(Calendar.YEAR);
		buscar();
	}

	public void buscar() {
		linhas = relatorioColetaGrupoService.gerarRelatorio(ano);

		totalAno = 0.;
		totalAnoAnterior = 0.;
		grupoMaiorVolume = null;
		for (ColetaGrupoMensalDTO dto : linhas) {
			totalAno += dto.getTotalAno();
			totalAnoAnterior += dto.getTotalAnoAnterior();
			if (grupoMaiorVolume == null || dto.getTotalAno() > grupoMaiorVolume.getTotalAno()) {
				grupoMaiorVolume = dto;
			}
		}
	}

	/** Crescimento do total geral coletado no ano em relação ao ano anterior. Null se não havia coleta no ano anterior. */
	public Double getCrescimentoGeralPercentual() {
		if (totalAnoAnterior == null || totalAnoAnterior == 0) {
			return null;
		}
		return ((totalAno - totalAnoAnterior) / totalAnoAnterior) * 100.;
	}

	public String getCrescimentoGeralStatCard() {
		Double c = getCrescimentoGeralPercentual();
		if (c == null || c == 0) {
			return "sc-info";
		}
		return c > 0 ? "sc-success" : "sc-danger";
	}

	public List<Integer> getAnosDisponiveis() {
		int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> anos = new ArrayList<>();
		for (int a = anoAtual; a >= anoAtual - 6; a--) {
			anos.add(a);
		}
		return anos;
	}

	public String[] getNomesMeses() {
		return NOMES_MESES;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public List<ColetaGrupoMensalDTO> getLinhas() {
		return linhas;
	}

	public Double getTotalAno() {
		return totalAno;
	}

	public Double getTotalAnoAnterior() {
		return totalAnoAnterior;
	}

	public ColetaGrupoMensalDTO getGrupoMaiorVolume() {
		return grupoMaiorVolume;
	}

}
