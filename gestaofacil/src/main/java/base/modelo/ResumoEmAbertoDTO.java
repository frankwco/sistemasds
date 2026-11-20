package base.modelo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Soma de valores em aberto (fechamentos não faturados) separada pela forma de cobrança do
 * contrato, com a contagem de contratos distintos (idInformacaoFinanceiraUnico).
 *
 * A separação segue a MESMA regra que o fechamento usa pra calcular o valorFaturar
 * (FechamentoMesMB, "if formaCobranca equals Anual"):
 * - Anual: o valor fixo já é cobrado por boleto/parcelas do contrato, então o que fica em
 *   aberto é só o excedente (o que passou da franquia).
 * - Qualquer outra forma (Mensal, ou vazia em cadastro antigo): o valor em aberto é a fatura
 *   inteira do mês, que ainda precisa ser faturada.
 * (Antes, forma vazia caía como "Anual (excedente)", mas o valorFaturar dela é a fatura
 * inteira.)
 *
 * Também guarda o mês mais antigo ainda pendente de cada grupo (formato aaaamm), pra tela
 * mostrar "desde mm/aaaa" e ficar fácil achar pendência antiga esquecida.
 *
 * Usado nos cards de resumo de fechamentoMes.xhtml e inicio.xhtml.
 */
public class ResumoEmAbertoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private double valorMensal = 0.;
	private double valorAnual = 0.;
	private Integer desdeMensal;
	private Integer desdeAnual;
	private final Set<Long> contratos = new HashSet<>();

	public static boolean isAnual(String formaCobranca) {
		return "Anual".equals(formaCobranca);
	}

	public void adicionar(Long idInformacaoFinanceiraUnico, String formaCobranca, Double valor) {
		adicionar(idInformacaoFinanceiraUnico, formaCobranca, valor, null);
	}

	/** @param periodo mês mais antigo do valor, no formato aaaamm (ex.: 202403), ou null. */
	public void adicionar(Long idInformacaoFinanceiraUnico, String formaCobranca, Double valor, Integer periodo) {
		if (valor == null) {
			return;
		}
		if (isAnual(formaCobranca)) {
			valorAnual += valor;
			desdeAnual = menor(desdeAnual, periodo);
		} else {
			valorMensal += valor;
			desdeMensal = menor(desdeMensal, periodo);
		}
		if (idInformacaoFinanceiraUnico != null) {
			contratos.add(idInformacaoFinanceiraUnico);
		}
	}

	private static Integer menor(Integer atual, Integer novo) {
		if (novo == null) {
			return atual;
		}
		return atual == null || novo < atual ? novo : atual;
	}

	private static String formatarPeriodo(Integer p) {
		if (p == null) {
			return "";
		}
		int mes = p % 100;
		return (mes < 10 ? "0" : "") + mes + "/" + (p / 100);
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

	/** Mês mais antigo pendente entre os mensais, "mm/aaaa" ou "" se não souber. */
	public String getDesdeMensal() {
		return formatarPeriodo(desdeMensal);
	}

	/** Mês mais antigo pendente entre os excedentes de anuais, "mm/aaaa" ou "". */
	public String getDesdeAnual() {
		return formatarPeriodo(desdeAnual);
	}
}
