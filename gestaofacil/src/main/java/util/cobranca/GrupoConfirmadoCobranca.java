package util.cobranca;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import base.modelo.Cliente;
import base.modelo.RelatorioCobranca;
import base.modelo.RelatorioCobrancaItem;

/**
 * Agrupa os títulos confirmados de um mesmo cliente, pra tela mostrar o nome uma vez só
 * e disparar um único email de cobrança por cliente (não um por título vencido).
 */
public class GrupoConfirmadoCobranca implements Serializable {

	private static final long serialVersionUID = 1L;

	private Cliente cliente;
	private List<RelatorioCobrancaItem> itens = new ArrayList<>();
	private RelatorioCobranca loteReferencia;

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public List<RelatorioCobrancaItem> getItens() {
		return itens;
	}

	public void adicionar(RelatorioCobrancaItem item) {
		itens.add(item);
	}

	/** Lote mais recente importado (não necessariamente o que criou os títulos deste grupo —
	 * títulos vencidos que continuam aparecendo semana após semana não são duplicados, então
	 * ficam presos ao lote que os criou primeiro). Usado só pra citar a data certa na mensagem. */
	public void setLoteReferencia(RelatorioCobranca loteReferencia) {
		this.loteReferencia = loteReferencia;
	}

	public Integer getQuantidadeTitulos() {
		return itens.size();
	}

	public Double getValorTotal() {
		double soma = 0.;
		for (RelatorioCobrancaItem item : itens) {
			soma += item.getValor() != null ? item.getValor() : 0.;
		}
		return soma;
	}

	public String getValorTotalFormatado() {
		return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(getValorTotal());
	}

	/** Nome(s) do sacado exatamente como veio no relatório do banco, pra conferência manual ao lado do cliente. */
	public String getNomesSacadoRelatorio() {
		Set<String> nomes = new LinkedHashSet<>();
		for (RelatorioCobrancaItem item : itens) {
			if (item.getNomeSacadoRelatorio() != null && !item.getNomeSacadoRelatorio().trim().isEmpty()) {
				nomes.add(item.getNomeSacadoRelatorio().trim());
			}
		}
		return String.join(" / ", nomes);
	}

	/** true se algum título do grupo teve o nome do sacado não batendo com o cliente resolvido pelo código. */
	public boolean isTemNomeDivergente() {
		for (RelatorioCobrancaItem item : itens) {
			if (Boolean.TRUE.equals(item.getNomeDivergente())) {
				return true;
			}
		}
		return false;
	}

	/** Uma linha por parcela, já formatada ("15/07/2026 — R$ 125,00"), pra listar na coluna. */
	public List<String> getParcelasFormatadas() {
		SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");
		NumberFormat formatoValor = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
		List<String> linhas = new ArrayList<>();
		for (RelatorioCobrancaItem item : itens) {
			String data = item.getDataVencimento() != null ? formatoData.format(item.getDataVencimento()) : "-";
			String valor = formatoValor.format(item.getValor() != null ? item.getValor() : 0.);
			linhas.add(data + " — " + valor);
		}
		return linhas;
	}

	public String getLinkWhatsappTelefone1() {
		return WhatsappUtil.montarLink(cliente.getTelefone1(), getMensagemWhatsapp());
	}

	public String getLinkWhatsappTelefone2() {
		return WhatsappUtil.montarLink(cliente.getTelefone2(), getMensagemWhatsapp());
	}

	/** Texto de cobrança pronto (mesmo usado nos links wa.me), pra reaproveitar em outros canais (ex.: app mobile). */
	public String getMensagemWhatsapp() {
		SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");
		NumberFormat formatoValor = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

		StringBuilder sb = new StringBuilder();
		sb.append("Olá, ").append(cliente.getNomeRazaoSocial())
				.append("! Aqui é da *DS Transportes*, da coleta de resíduos de saúde. ")
				.append("Identificamos os seguintes títulos em aberto:\n\n");
		for (RelatorioCobrancaItem item : itens) {
			String data = item.getDataVencimento() != null ? formatoData.format(item.getDataVencimento()) : "-";
			String valor = formatoValor.format(item.getValor() != null ? item.getValor() : 0.);
			sb.append("- Vencimento ").append(data).append(": ").append(valor).append("\n");
		}
		sb.append("\nTotal em aberto: ").append(getValorTotalFormatado());
		sb.append("\n\nEssa informação é baseada em relatório do dia ").append(getDataRelatorioFormatada())
				.append(" e caso o pagamento já tenha sido efetuado, por favor desconsidere esta mensagem.");
		return sb.toString();
	}

	/** Data em que o banco gerou o relatório (lida do próprio CSV) — cai para a data de
	 * importação só em lotes antigos, de antes de existir essa captura. Usa sempre o lote mais
	 * recente (loteReferencia), não o que originalmente criou os títulos deste grupo — mesma
	 * data mostrada no "Resumo da importação" da tela. */
	private String getDataRelatorioFormatada() {
		RelatorioCobranca relatorio = loteReferencia != null ? loteReferencia
				: (!itens.isEmpty() ? itens.get(0).getRelatorioCobranca() : null);
		if (relatorio == null) {
			return "-";
		}
		Date data = relatorio.getDataCriacaoArquivo() != null ? relatorio.getDataCriacaoArquivo()
				: relatorio.getDataImportacao();
		return data != null ? new SimpleDateFormat("dd/MM/yyyy").format(data) : "-";
	}

}
