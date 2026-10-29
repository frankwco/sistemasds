package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.PainelExecutivoDTO;
import base.service.RelatorioPainelExecutivoService;

@ViewScoped
@Named("painelExecutivoMB")
public class PainelExecutivoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String[] NOMES_MESES = { "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho",
			"Agosto", "Setembro", "Outubro", "Novembro", "Dezembro" };

	private Integer mes;
	private Integer ano;

	private PainelExecutivoDTO dados = new PainelExecutivoDTO();
	private boolean carregado = false;

	@Inject
	private RelatorioPainelExecutivoService relatorioPainelExecutivoService;

	@PostConstruct
	public void inicializar() {
		Calendar cal = Calendar.getInstance();
		mes = cal.get(Calendar.MONTH) + 1;
		ano = cal.get(Calendar.YEAR);
		// Não busca aqui: a tela abre com o "shell" (filtro, menu) e um
		// p:remoteCommand autoRun busca os dados via ajax logo em seguida.
		// Antes disso, a página inteira ficava travada até a consulta
		// terminar, parecendo que o sistema tinha travado.
	}

	public void buscar() {
		dados = relatorioPainelExecutivoService.gerarRelatorio(mes, ano);
		carregado = true;
	}

	public boolean isCarregado() {
		return carregado;
	}

	public String getNomeMesSelecionado() {
		return NOMES_MESES[mes - 1];
	}

	public List<Integer> getAnosDisponiveis() {
		int anoAtual = Calendar.getInstance().get(Calendar.YEAR);
		List<Integer> anos = new ArrayList<>();
		for (int a = anoAtual; a >= anoAtual - 6; a--) {
			anos.add(a);
		}
		return anos;
	}

	public String getVariacaoContratosStatCard() {
		long v = dados.getVariacaoContratosAno();
		if (v > 0) {
			return "sc-success";
		}
		if (v < 0) {
			return "sc-danger";
		}
		return "sc-info";
	}

	public String getVariacaoFaturamentoMesStatCard() {
		return statCardPorPercentual(dados.getVariacaoFaturamentoMesPercentual());
	}

	public String getVariacaoFaturamentoAnoStatCard() {
		return statCardPorPercentual(dados.getVariacaoFaturamentoAnoPercentual());
	}

	public String getVariacaoPesoMesStatCard() {
		return statCardPorPercentual(dados.getVariacaoPesoMesPercentual());
	}

	public String getVariacaoPesoAnoStatCard() {
		return statCardPorPercentual(dados.getVariacaoPesoAnoPercentual());
	}

	public String getVariacaoFaturamentoPorQuiloStatCard() {
		return statCardPorPercentual(dados.getVariacaoFaturamentoPorQuiloPercentual());
	}

	public String getVariacaoFaturamentoPorQuiloMesStatCard() {
		return statCardPorPercentual(dados.getVariacaoFaturamentoPorQuiloMesPercentual());
	}

	private String statCardPorPercentual(Double percentual) {
		if (percentual == null || percentual == 0) {
			return "sc-info";
		}
		return percentual > 0 ? "sc-success" : "sc-danger";
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

	public PainelExecutivoDTO getDados() {
		return dados;
	}

}
