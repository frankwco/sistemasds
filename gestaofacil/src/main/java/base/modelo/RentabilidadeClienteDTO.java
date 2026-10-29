package base.modelo;

import java.io.Serializable;

/**
 * Linha do relatório "Rentabilidade por Cliente": não é entidade JPA, é
 * apenas o agregado (peso contratado x peso real x faturamento) de um
 * cliente para o período pesquisado, montado a partir dos FechamentoMes.
 */
public class RentabilidadeClienteDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Cliente cliente;
	private Double pesoContratado = 0.;
	private Double pesoReal = 0.;
	private Double valorFaturado = 0.;
	private Integer quantidadeFechamentos = 0;

	// De todos os fechamentos somados aqui, pelo menos um veio de contrato que
	// cobra excedente (InformacaoFinanceira.cobraExcedente != 'Não') e/ou pelo
	// menos um veio de contrato de valor fixo (cobraExcedente == 'Não').
	private boolean temFechamentoComExcedente = false;
	private boolean temFechamentoSemExcedente = false;

	public RentabilidadeClienteDTO(Cliente cliente) {
		this.cliente = cliente;
	}

	public void somarFechamento(Double pesoContratadoMes, Double pesoRealMes, Double valorFaturadoMes,
			String cobraExcedente) {
		this.pesoContratado += (pesoContratadoMes == null ? 0. : pesoContratadoMes);
		this.pesoReal += (pesoRealMes == null ? 0. : pesoRealMes);
		this.valorFaturado += (valorFaturadoMes == null ? 0. : valorFaturadoMes);
		this.quantidadeFechamentos++;

		if (cobraExcedente != null && !cobraExcedente.equals("Não")) {
			temFechamentoComExcedente = true;
		} else {
			temFechamentoSemExcedente = true;
		}
	}

	public Cliente getCliente() {
		return cliente;
	}

	public Double getPesoContratado() {
		return pesoContratado;
	}

	public Double getPesoReal() {
		return pesoReal;
	}

	public Double getValorFaturado() {
		return valorFaturado;
	}

	public Integer getQuantidadeFechamentos() {
		return quantidadeFechamentos;
	}

	/**
	 * "Cobra excedente" (todos os fechamentos do período vieram de contrato(s)
	 * que cobram o que passar do peso contratado), "Não cobra excedente"
	 * (valor fixo, independente do peso) ou "Misto" (cliente com mais de um
	 * contrato no período, com regras diferentes).
	 */
	public String getRegimeExcedente() {
		if (temFechamentoComExcedente && temFechamentoSemExcedente) {
			return "Misto";
		}
		if (temFechamentoComExcedente) {
			return "Cobra excedente";
		}
		return "Não cobra excedente";
	}

	/** Diferença percentual entre o peso realmente coletado e o peso contratado. Positivo = coletou mais do que o contratado. */
	public Double getDesvioPesoPercentual() {
		if (pesoContratado == null || pesoContratado == 0) {
			return null;
		}
		return ((pesoReal - pesoContratado) / pesoContratado) * 100.;
	}

	/** Valor faturado dividido pelo peso realmente coletado (R$/kg efetivo). */
	public Double getValorPorQuiloEfetivo() {
		if (pesoReal == null || pesoReal == 0) {
			return null;
		}
		return valorFaturado / pesoReal;
	}

	/**
	 * Classificação da rentabilidade, com faixa de tolerância de 10% em torno
	 * do peso contratado. Coletar acima do contratado só é tratado como risco
	 * de margem quando o contrato NÃO cobra excedente (valor fixo) — se cobra,
	 * o excesso já vira faturamento extra, então não é um problema.
	 */
	public String getSituacao() {
		if (pesoContratado == null || pesoContratado == 0) {
			return "Sem peso contratado";
		}
		Double desvio = getDesvioPesoPercentual();
		if (desvio == null) {
			return "Sem coleta no período";
		}
		if (desvio > 10.) {
			if (temFechamentoSemExcedente) {
				return "Acima do contratado - risco de margem";
			}
			return "Acima do contratado (excedente cobrado)";
		}
		if (desvio < -10.) {
			return "Abaixo do contratado";
		}
		return "Dentro do esperado";
	}

	/**
	 * Cor sugerida para a coluna "Situação" na tela, já resolvida aqui para
	 * não depender de comparação de texto (e acento) na view.
	 */
	public String getSituacaoCor() {
		if (pesoContratado == null || pesoContratado == 0) {
			return "#757575";
		}
		Double desvio = getDesvioPesoPercentual();
		if (desvio == null) {
			return "#757575";
		}
		if (desvio > 10.) {
			return temFechamentoSemExcedente ? "#c62828" : "#2e7d32";
		}
		if (desvio < -10.) {
			return "#f57f17";
		}
		return "#2e7d32";
	}

}
