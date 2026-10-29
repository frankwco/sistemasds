package base.modelo;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Linha do relatório "Coletas por Grupo": não é entidade JPA. Guarda, para
 * um GrupoResiduo, o peso coletado em cada mês do ano selecionado (a partir
 * dos ItensColeta) e o total do ano anterior, para calcular o crescimento.
 */
public class ColetaGrupoMensalDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private GrupoResiduo grupo;
	private Double[] pesoPorMes = new Double[12]; // índice 0 = Janeiro ... 11 = Dezembro
	private Double totalAnoAnterior = 0.;

	public ColetaGrupoMensalDTO(GrupoResiduo grupo) {
		this.grupo = grupo;
		Arrays.fill(pesoPorMes, 0.);
	}

	public void somarMes(int mesIndiceZeroA11, Double peso) {
		pesoPorMes[mesIndiceZeroA11] += (peso == null ? 0. : peso);
	}

	public void somarAnoAnterior(Double peso) {
		totalAnoAnterior += (peso == null ? 0. : peso);
	}

	public GrupoResiduo getGrupo() {
		return grupo;
	}

	public Double[] getPesoPorMes() {
		return pesoPorMes;
	}

	public Double getTotalAnoAnterior() {
		return totalAnoAnterior;
	}

	public Double getTotalAno() {
		double soma = 0.;
		for (Double v : pesoPorMes) {
			soma += (v == null ? 0. : v);
		}
		return soma;
	}

	/** Crescimento do total do ano selecionado em relação ao total do ano anterior. Null se não havia coleta no ano anterior. */
	public Double getCrescimentoPercentual() {
		if (totalAnoAnterior == null || totalAnoAnterior == 0) {
			return null;
		}
		return ((getTotalAno() - totalAnoAnterior) / totalAnoAnterior) * 100.;
	}

	/** Cor sugerida para exibir o crescimento na tela, já resolvida aqui para não depender de comparação de texto na view. */
	public String getCrescimentoCor() {
		Double c = getCrescimentoPercentual();
		if (c == null || c == 0) {
			return "#757575";
		}
		return c > 0 ? "#2e7d32" : "#c62828";
	}

}
