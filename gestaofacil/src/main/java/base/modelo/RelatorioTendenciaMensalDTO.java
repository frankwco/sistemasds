package base.modelo;

import java.io.Serializable;

/** Custo, km rodado e custo/km de um mês — série histórica do painel de veículos. */
public class RelatorioTendenciaMensalDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	// "yyyy-MM"
	private String mesReferencia;
	private Double custoTotal;
	private Long kmRodado;
	private Double custoPorKm;

	public String getMesReferencia() {
		return mesReferencia;
	}

	public void setMesReferencia(String mesReferencia) {
		this.mesReferencia = mesReferencia;
	}

	public Double getCustoTotal() {
		return custoTotal;
	}

	public void setCustoTotal(Double custoTotal) {
		this.custoTotal = custoTotal;
	}

	public Long getKmRodado() {
		return kmRodado;
	}

	public void setKmRodado(Long kmRodado) {
		this.kmRodado = kmRodado;
	}

	public Double getCustoPorKm() {
		return custoPorKm;
	}

	public void setCustoPorKm(Double custoPorKm) {
		this.custoPorKm = custoPorKm;
	}

}
