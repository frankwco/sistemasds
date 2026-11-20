package base.modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Não é entidade JPA. Guarda os indicadores do "Painel Executivo" para o mês
 * selecionado e para o acumulado do ano (janeiro até o mês selecionado),
 * sempre comparados com o mesmo período do ano anterior.
 *
 * Os valores "acumulados" (faturamentoAno, pesoColetadoAno...) NÃO são o ano
 * inteiro: vão de janeiro até o mês selecionado, nos dois anos. Comparar um
 * ano incompleto com o ano anterior inteiro fazia a variação sair sempre
 * negativa até dezembro.
 */
public class PainelExecutivoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long contratosAtivos = 0L;
	private Long pontosColetaAtivos = 0L;
	/** Clientes cujo primeiro contrato começou no acumulado do ano. */
	private Long clientesNovos = 0L;
	/** Clientes que tiveram o último contrato inativado no acumulado do ano e não têm contrato ativo hoje. */
	private Long clientesSaida = 0L;
	private Long contratosSemFechamentoMes = 0L;
	private List<InformacaoFinanceira> contratosSemFechamentoLista = new ArrayList<>();

	/** Valor apurado (valorFinal) nos fechamentos, todos os clientes. */
	private Double faturamentoMes = 0.;
	private Double faturamentoMesAnoAnterior = 0.;
	private Double faturamentoAno = 0.;
	private Double faturamentoAnoAnterior = 0.;

	/**
	 * Mesmo valor apurado, mas sem os clientes com ocultarRelatorio = true —
	 * mesma base do peso coletado, usada só no R$/kg (senão o numerador tem
	 * clientes que o denominador não tem).
	 */
	private Double faturamentoMesBasePeso = 0.;
	private Double faturamentoMesAnoAnteriorBasePeso = 0.;
	private Double faturamentoAnoBasePeso = 0.;
	private Double faturamentoAnoAnteriorBasePeso = 0.;

	private Double pesoColetadoMes = 0.;
	private Double pesoColetadoMesAnoAnterior = 0.;
	private Double pesoColetadoAno = 0.;
	private Double pesoColetadoAnoAnterior = 0.;

	/** (atual − anterior) ÷ anterior × 100. Null se não der pra comparar. */
	private static Double variacao(Double atual, Double anterior) {
		if (atual == null || anterior == null || anterior == 0) {
			return null;
		}
		return ((atual - anterior) / anterior) * 100.;
	}

	private static Double divisao(Double valor, Double peso) {
		if (valor == null || peso == null || peso == 0) {
			return null;
		}
		return valor / peso;
	}

	/** Clientes novos menos clientes que saíram (pode ser negativo). */
	public Long getSaldoClientes() {
		return clientesNovos - clientesSaida;
	}

	public Double getVariacaoFaturamentoMesPercentual() {
		return variacao(faturamentoMes, faturamentoMesAnoAnterior);
	}

	public Double getVariacaoFaturamentoAnoPercentual() {
		return variacao(faturamentoAno, faturamentoAnoAnterior);
	}

	public Double getVariacaoPesoMesPercentual() {
		return variacao(pesoColetadoMes, pesoColetadoMesAnoAnterior);
	}

	public Double getVariacaoPesoAnoPercentual() {
		return variacao(pesoColetadoAno, pesoColetadoAnoAnterior);
	}

	public Double getFaturamentoPorQuiloMes() {
		return divisao(faturamentoMesBasePeso, pesoColetadoMes);
	}

	public Double getFaturamentoPorQuiloMesAnoAnterior() {
		return divisao(faturamentoMesAnoAnteriorBasePeso, pesoColetadoMesAnoAnterior);
	}

	public Double getVariacaoFaturamentoPorQuiloMesPercentual() {
		return variacao(getFaturamentoPorQuiloMes(), getFaturamentoPorQuiloMesAnoAnterior());
	}

	public Double getFaturamentoPorQuiloAno() {
		return divisao(faturamentoAnoBasePeso, pesoColetadoAno);
	}

	public Double getFaturamentoPorQuiloAnoAnterior() {
		return divisao(faturamentoAnoAnteriorBasePeso, pesoColetadoAnoAnterior);
	}

	public Double getVariacaoFaturamentoPorQuiloPercentual() {
		return variacao(getFaturamentoPorQuiloAno(), getFaturamentoPorQuiloAnoAnterior());
	}

	public Long getContratosAtivos() {
		return contratosAtivos;
	}

	public void setContratosAtivos(Long contratosAtivos) {
		this.contratosAtivos = contratosAtivos;
	}

	public Long getPontosColetaAtivos() {
		return pontosColetaAtivos;
	}

	public void setPontosColetaAtivos(Long pontosColetaAtivos) {
		this.pontosColetaAtivos = pontosColetaAtivos;
	}

	public Long getClientesNovos() {
		return clientesNovos;
	}

	public void setClientesNovos(Long clientesNovos) {
		this.clientesNovos = clientesNovos;
	}

	public Long getClientesSaida() {
		return clientesSaida;
	}

	public void setClientesSaida(Long clientesSaida) {
		this.clientesSaida = clientesSaida;
	}

	public Long getContratosSemFechamentoMes() {
		return contratosSemFechamentoMes;
	}

	public void setContratosSemFechamentoMes(Long contratosSemFechamentoMes) {
		this.contratosSemFechamentoMes = contratosSemFechamentoMes;
	}

	public List<InformacaoFinanceira> getContratosSemFechamentoLista() {
		return contratosSemFechamentoLista;
	}

	public void setContratosSemFechamentoLista(List<InformacaoFinanceira> contratosSemFechamentoLista) {
		this.contratosSemFechamentoLista = contratosSemFechamentoLista;
	}

	public Double getFaturamentoMes() {
		return faturamentoMes;
	}

	public void setFaturamentoMes(Double faturamentoMes) {
		this.faturamentoMes = faturamentoMes;
	}

	public Double getFaturamentoMesAnoAnterior() {
		return faturamentoMesAnoAnterior;
	}

	public void setFaturamentoMesAnoAnterior(Double faturamentoMesAnoAnterior) {
		this.faturamentoMesAnoAnterior = faturamentoMesAnoAnterior;
	}

	public Double getFaturamentoAno() {
		return faturamentoAno;
	}

	public void setFaturamentoAno(Double faturamentoAno) {
		this.faturamentoAno = faturamentoAno;
	}

	public Double getFaturamentoAnoAnterior() {
		return faturamentoAnoAnterior;
	}

	public void setFaturamentoAnoAnterior(Double faturamentoAnoAnterior) {
		this.faturamentoAnoAnterior = faturamentoAnoAnterior;
	}

	public Double getFaturamentoMesBasePeso() {
		return faturamentoMesBasePeso;
	}

	public void setFaturamentoMesBasePeso(Double faturamentoMesBasePeso) {
		this.faturamentoMesBasePeso = faturamentoMesBasePeso;
	}

	public Double getFaturamentoMesAnoAnteriorBasePeso() {
		return faturamentoMesAnoAnteriorBasePeso;
	}

	public void setFaturamentoMesAnoAnteriorBasePeso(Double faturamentoMesAnoAnteriorBasePeso) {
		this.faturamentoMesAnoAnteriorBasePeso = faturamentoMesAnoAnteriorBasePeso;
	}

	public Double getFaturamentoAnoBasePeso() {
		return faturamentoAnoBasePeso;
	}

	public void setFaturamentoAnoBasePeso(Double faturamentoAnoBasePeso) {
		this.faturamentoAnoBasePeso = faturamentoAnoBasePeso;
	}

	public Double getFaturamentoAnoAnteriorBasePeso() {
		return faturamentoAnoAnteriorBasePeso;
	}

	public void setFaturamentoAnoAnteriorBasePeso(Double faturamentoAnoAnteriorBasePeso) {
		this.faturamentoAnoAnteriorBasePeso = faturamentoAnoAnteriorBasePeso;
	}

	public Double getPesoColetadoMes() {
		return pesoColetadoMes;
	}

	public void setPesoColetadoMes(Double pesoColetadoMes) {
		this.pesoColetadoMes = pesoColetadoMes;
	}

	public Double getPesoColetadoMesAnoAnterior() {
		return pesoColetadoMesAnoAnterior;
	}

	public void setPesoColetadoMesAnoAnterior(Double pesoColetadoMesAnoAnterior) {
		this.pesoColetadoMesAnoAnterior = pesoColetadoMesAnoAnterior;
	}

	public Double getPesoColetadoAno() {
		return pesoColetadoAno;
	}

	public void setPesoColetadoAno(Double pesoColetadoAno) {
		this.pesoColetadoAno = pesoColetadoAno;
	}

	public Double getPesoColetadoAnoAnterior() {
		return pesoColetadoAnoAnterior;
	}

	public void setPesoColetadoAnoAnterior(Double pesoColetadoAnoAnterior) {
		this.pesoColetadoAnoAnterior = pesoColetadoAnoAnterior;
	}

}
