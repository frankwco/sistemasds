package base.controle;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
		// Abre no mês anterior: o fechamento de um mês só é feito no mês
		// seguinte, então o mês corrente apareceria com faturamento ~0,
		// variação ~-100% e quase todos os contratos "sem fechamento".
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
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

	/** Ex.: "Out/2026". */
	public String getRotuloMes() {
		return abreviar(mes) + "/" + ano;
	}

	/** Ex.: "Out/2025". */
	public String getRotuloMesAnoAnterior() {
		return abreviar(mes) + "/" + (ano - 1);
	}

	/** Ex.: "Jan–Out/2026" (ou "Jan/2026" quando o mês selecionado é janeiro). */
	public String getRotuloAcumulado() {
		return rotuloAcumulado(ano);
	}

	/** Ex.: "Jan–Out/2025". */
	public String getRotuloAcumuladoAnoAnterior() {
		return rotuloAcumulado(ano - 1);
	}

	private String rotuloAcumulado(int a) {
		if (mes == 1) {
			return "Jan/" + a;
		}
		return "Jan–" + abreviar(mes) + "/" + a;
	}

	private static String abreviar(int m) {
		return NOMES_MESES[m - 1].substring(0, 3);
	}

	/** Mês selecionado é o mês corrente (ou futuro): fechamento ainda não foi feito. */
	public boolean isMesEmAndamento() {
		Calendar hoje = Calendar.getInstance();
		int atual = hoje.get(Calendar.YEAR) * 12 + hoje.get(Calendar.MONTH) + 1;
		return ano * 12 + mes >= atual;
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
		long v = dados.getSaldoClientes();
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

	/** "+5,3%", "-12,0%" ou "sem base de comparação" quando não dá pra comparar. */
	public String formatarVariacao(Double percentual) {
		if (percentual == null) {
			return "sem base de comparação";
		}
		DecimalFormat df = new DecimalFormat("#,##0.0", new DecimalFormatSymbols(new Locale("pt", "BR")));
		return (percentual > 0 ? "+" : "") + df.format(percentual) + "%";
	}

	/** Saldo de clientes com sinal: "+3", "-2", "0". */
	public String getSaldoClientesFormatado() {
		long v = dados.getSaldoClientes();
		return (v > 0 ? "+" : "") + v;
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
