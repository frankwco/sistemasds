package base.modelo;

import java.io.Serializable;

/**
 * Custo total, km rodado e custo/km de um veículo num período (RF19/RF20).
 * A lista já ordenada por custo/km desc já é o ranking do RF27.
 */
public class RelatorioCustoVeiculoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private Double custoTotal;
	private Long kmRodado;
	private Double custoPorKm;

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
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
