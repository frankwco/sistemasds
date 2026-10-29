package base.modelo;

import java.io.Serializable;

/** Consumo médio (km/l) de um veículo no período (RF21). */
public class RelatorioConsumoVeiculoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private Long kmRodado;
	private Double litrosTotal;
	private Double kmPorLitro;

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public Long getKmRodado() {
		return kmRodado;
	}

	public void setKmRodado(Long kmRodado) {
		this.kmRodado = kmRodado;
	}

	public Double getLitrosTotal() {
		return litrosTotal;
	}

	public void setLitrosTotal(Double litrosTotal) {
		this.litrosTotal = litrosTotal;
	}

	public Double getKmPorLitro() {
		return kmPorLitro;
	}

	public void setKmPorLitro(Double kmPorLitro) {
		this.kmPorLitro = kmPorLitro;
	}

}
