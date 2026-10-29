package base.modelo;

import java.io.Serializable;

/** Custo médio e avaliação de cada oficina, pelos Serviços do período (RF23). */
public class RelatorioOficinaDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nome;
	private Integer quantidadeServicos;
	private Double custoMedio;
	private Double avaliacao;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Integer getQuantidadeServicos() {
		return quantidadeServicos;
	}

	public void setQuantidadeServicos(Integer quantidadeServicos) {
		this.quantidadeServicos = quantidadeServicos;
	}

	public Double getCustoMedio() {
		return custoMedio;
	}

	public void setCustoMedio(Double custoMedio) {
		this.custoMedio = custoMedio;
	}

	public Double getAvaliacao() {
		return avaliacao;
	}

	public void setAvaliacao(Double avaliacao) {
		this.avaliacao = avaliacao;
	}

	/** PrimeFaces p:rating só aceita valor inteiro; usado apenas para exibição. */
	public Integer getAvaliacaoEstrelas() {
		return avaliacao == null ? 0 : (int) Math.round(avaliacao);
	}

}
