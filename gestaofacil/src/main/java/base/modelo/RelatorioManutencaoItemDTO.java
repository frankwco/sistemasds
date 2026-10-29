package base.modelo;

import java.io.Serializable;
import java.util.Date;

/** Um Serviço do período com o tempo de inatividade calculado (RF25/RF26). */
public class RelatorioManutencaoItemDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Veiculo veiculo;
	private String tipoManutencao;
	private Date dataEntrada;
	private Date dataSaida;
	private Long downtimeDias;

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public String getTipoManutencao() {
		return tipoManutencao;
	}

	public void setTipoManutencao(String tipoManutencao) {
		this.tipoManutencao = tipoManutencao;
	}

	public Date getDataEntrada() {
		return dataEntrada;
	}

	public void setDataEntrada(Date dataEntrada) {
		this.dataEntrada = dataEntrada;
	}

	public Date getDataSaida() {
		return dataSaida;
	}

	public void setDataSaida(Date dataSaida) {
		this.dataSaida = dataSaida;
	}

	public Long getDowntimeDias() {
		return downtimeDias;
	}

	public void setDowntimeDias(Long downtimeDias) {
		this.downtimeDias = downtimeDias;
	}

}
