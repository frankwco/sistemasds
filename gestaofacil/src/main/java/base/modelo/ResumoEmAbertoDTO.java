package base.modelo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Soma de valores em aberto (fechamentos não faturados) separada pela forma de cobrança do
 * contrato, com a contagem de contratos distintos (idInformacaoFinanceiraUnico).
 *
 * - Mensal: contratos cobrados todo mês — o valor em aberto é a fatura inteira do mês.
 * - Anual: contratos com valor fixo pago em parcelas — o valor em aberto é só o excedente
 *   (o que passou da franquia). Qualquer forma de cobrança diferente de 'Mensal' cai aqui,
 *   mesmo critério de FechamentoMesService.listarExcedentesAbertosTodos().
 *
 * Usado nos cards de resumo de fechamentoMes.xhtml.
 */
public class ResumoEmAbertoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private double valorMensal = 0.;
	private double valorAnual = 0.;
	private final Set<Long> contratos = new HashSet<>();

	public void adicionar(Long idInformacaoFinanceiraUnico, String formaCobranca, Double valor) {
		if (valor == null) {
			return;
		}
		if ("Mensal".equals(formaCobranca)) {
			valorMensal += valor;
		} else {
			valorAnual += valor;
		}
		if (idInformacaoFinanceiraUnico != null) {
			contratos.add(idInformacaoFinanceiraUnico);
		}
	}

	public double getValorMensal() {
		return valorMensal;
	}

	public double getValorAnual() {
		return valorAnual;
	}

	public double getTotal() {
		return valorMensal + valorAnual;
	}

	public int getQuantidadeContratos() {
		return contratos.size();
	}
}
