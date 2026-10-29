package base.modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Não é entidade JPA. Guarda os indicadores do "Painel Executivo": carteira
 * de clientes, faturamento e peso coletado, no mês/ano selecionado, com as
 * respectivas comparações (variação de contratos no ano, faturamento vs
 * mesmo mês/ano anterior).
 */
public class PainelExecutivoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long contratosAtivos = 0L;
	private Long pontosColetaAtivos = 0L;
	private Long contratosNovosAno = 0L;
	private Long contratosEncerradosAno = 0L;
	private Long contratosSemFechamentoMes = 0L;
	private List<InformacaoFinanceira> contratosSemFechamentoLista = new ArrayList<>();

	private Double faturamentoMes = 0.;
	private Double faturamentoMesAnoAnterior = 0.;
	private Double faturamentoAno = 0.;
	private Double faturamentoAnoAnterior = 0.;

	private Double pesoColetadoMes = 0.;
	private Double pesoColetadoMesAnoAnterior = 0.;
	private Double pesoColetadoAno = 0.;
	private Double pesoColetadoAnoAnterior = 0.;

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

	public Long getContratosNovosAno() {
		return contratosNovosAno;
	}

	public void setContratosNovosAno(Long contratosNovosAno) {
		this.contratosNovosAno = contratosNovosAno;
	}

	public Long getContratosEncerradosAno() {
		return contratosEncerradosAno;
	}

	public void setContratosEncerradosAno(Long contratosEncerradosAno) {
		this.contratosEncerradosAno = contratosEncerradosAno;
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

	/** Novos contratos menos contratos encerrados no ano (pode ser negativo). */
	public Long getVariacaoContratosAno() {
		return contratosNovosAno - contratosEncerradosAno;
	}

	/** Faturamento do mês selecionado comparado ao mesmo mês do ano anterior. Null se não houve faturamento no ano anterior. */
	public Double getVariacaoFaturamentoMesPercentual() {
		if (faturamentoMesAnoAnterior == null || faturamentoMesAnoAnterior == 0) {
			return null;
		}
		return ((faturamentoMes - faturamentoMesAnoAnterior) / faturamentoMesAnoAnterior) * 100.;
	}

	/** Faturamento do ano selecionado comparado ao ano anterior. Null se não houve faturamento no ano anterior. */
	public Double getVariacaoFaturamentoAnoPercentual() {
		if (faturamentoAnoAnterior == null || faturamentoAnoAnterior == 0) {
			return null;
		}
		return ((faturamentoAno - faturamentoAnoAnterior) / faturamentoAnoAnterior) * 100.;
	}

	/** Peso coletado no mês selecionado comparado ao mesmo mês do ano anterior. Null se não houve coleta no ano anterior. */
	public Double getVariacaoPesoMesPercentual() {
		if (pesoColetadoMesAnoAnterior == null || pesoColetadoMesAnoAnterior == 0) {
			return null;
		}
		return ((pesoColetadoMes - pesoColetadoMesAnoAnterior) / pesoColetadoMesAnoAnterior) * 100.;
	}

	/** Peso coletado no ano selecionado comparado ao ano anterior. Null se não houve coleta no ano anterior. */
	public Double getVariacaoPesoAnoPercentual() {
		if (pesoColetadoAnoAnterior == null || pesoColetadoAnoAnterior == 0) {
			return null;
		}
		return ((pesoColetadoAno - pesoColetadoAnoAnterior) / pesoColetadoAnoAnterior) * 100.;
	}

	/** Faturamento do ano dividido pelo peso coletado no ano (R$/kg médio). Null se não houve coleta no ano. */
	public Double getFaturamentoPorQuiloAno() {
		if (pesoColetadoAno == null || pesoColetadoAno == 0) {
			return null;
		}
		return faturamentoAno / pesoColetadoAno;
	}

	/** Faturamento do ano anterior dividido pelo peso coletado no ano anterior (R$/kg médio). Null se não houve coleta no ano anterior. */
	public Double getFaturamentoPorQuiloAnoAnterior() {
		if (pesoColetadoAnoAnterior == null || pesoColetadoAnoAnterior == 0) {
			return null;
		}
		return faturamentoAnoAnterior / pesoColetadoAnoAnterior;
	}

	/** Variação do R$/kg médio do ano em relação ao ano anterior. Null se algum dos dois lados não puder ser calculado. */
	public Double getVariacaoFaturamentoPorQuiloPercentual() {
		Double atual = getFaturamentoPorQuiloAno();
		Double anterior = getFaturamentoPorQuiloAnoAnterior();
		if (atual == null || anterior == null || anterior == 0) {
			return null;
		}
		return ((atual - anterior) / anterior) * 100.;
	}

	/** Faturamento do mês dividido pelo peso coletado no mês (R$/kg médio). Null se não houve coleta no mês. */
	public Double getFaturamentoPorQuiloMes() {
		if (pesoColetadoMes == null || pesoColetadoMes == 0) {
			return null;
		}
		return faturamentoMes / pesoColetadoMes;
	}

	/** Faturamento do mesmo mês do ano anterior dividido pelo peso coletado nesse mês (R$/kg médio). Null se não houve coleta. */
	public Double getFaturamentoPorQuiloMesAnoAnterior() {
		if (pesoColetadoMesAnoAnterior == null || pesoColetadoMesAnoAnterior == 0) {
			return null;
		}
		return faturamentoMesAnoAnterior / pesoColetadoMesAnoAnterior;
	}

	/** Variação do R$/kg médio do mês em relação ao mesmo mês do ano anterior. Null se algum dos dois lados não puder ser calculado. */
	public Double getVariacaoFaturamentoPorQuiloMesPercentual() {
		Double atual = getFaturamentoPorQuiloMes();
		Double anterior = getFaturamentoPorQuiloMesAnoAnterior();
		if (atual == null || anterior == null || anterior == 0) {
			return null;
		}
		return ((atual - anterior) / anterior) * 100.;
	}

}
