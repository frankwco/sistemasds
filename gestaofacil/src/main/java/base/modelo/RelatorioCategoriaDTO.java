package base.modelo;

import java.io.Serializable;

/** Despesa total por categoria e seu percentual do total do período (RF24). */
public class RelatorioCategoriaDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String categoria;
	private Double valorTotal;
	private Double percentual;

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public Double getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(Double valorTotal) {
		this.valorTotal = valorTotal;
	}

	public Double getPercentual() {
		return percentual;
	}

	public void setPercentual(Double percentual) {
		this.percentual = percentual;
	}

}
