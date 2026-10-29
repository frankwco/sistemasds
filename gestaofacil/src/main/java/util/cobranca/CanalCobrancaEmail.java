package util.cobranca;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import base.modelo.Cliente;
import base.modelo.RelatorioCobranca;
import base.modelo.RelatorioCobrancaItem;
import util.EnviarEmail;
import util.email.EmailTemplate;

public class CanalCobrancaEmail implements CanalCobranca {

	private static final String COR_COBRANCA = "#c62828";
	private static final String WHATSAPP_EMPRESA_EXIBICAO = "(44) 9.9932-3410";
	private static final String WHATSAPP_EMPRESA_E164 = "5544999323410";

	@Override
	public String getNome() {
		return "EMAIL";
	}

	@Override
	public boolean enviar(Cliente cliente, List<RelatorioCobrancaItem> itensEmAberto) {
		String destinatario = cliente.getEmail() != null ? cliente.getEmail().trim() : "";
		if (destinatario.length() < 5 || itensEmAberto == null || itensEmAberto.isEmpty()) {
			return false;
		}

		String titulo = "Aviso de pendência financeira - " + cliente.getNomeRazaoSocial();
		String corpo = montarCorpo(cliente, itensEmAberto);

		// Envio síncrono (não uma thread solta): só sabemos que realmente foi enviado
		// esperando a resposta do servidor SMTP, senão o histórico registraria cobranças
		// que nunca saíram.
		Boolean enviado = EnviarEmail.enviarEmail(destinatario, titulo, EmailTemplate.build(COR_COBRANCA, corpo));
		return Boolean.TRUE.equals(enviado);
	}

	private String montarCorpo(Cliente cliente, List<RelatorioCobrancaItem> itens) {
		SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");
		double valorTotal = 0.;
		for (RelatorioCobrancaItem item : itens) {
			valorTotal += item.getValor() != null ? item.getValor() : 0.;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<p style='font-size:14px;color:#212121;'>Prezado(a) <b>").append(cliente.getNomeRazaoSocial())
				.append("</b>,</p>");
		sb.append("<p style='font-size:14px;color:#212121;'>Somos da <b>DS Transportes</b>, empresa responsável ")
				.append("pela coleta de resíduos de saúde. Identificamos os seguintes títulos em aberto:</p>");

		sb.append("<table class='residuos' width='100%' cellpadding='8' cellspacing='0' "
				+ "style='border-collapse:collapse;font-size:13px;'>");
		sb.append("<tr style='background:#f5f5f5;'>")
				.append("<th align='left' style='border-bottom:2px solid #e0e0e0;'>Vencimento</th>")
				.append("<th align='right' style='border-bottom:2px solid #e0e0e0;'>Valor</th></tr>");
		for (RelatorioCobrancaItem item : itens) {
			sb.append("<tr>")
					.append("<td style='border-bottom:1px solid #eee;'>")
					.append(item.getDataVencimento() != null ? formatoData.format(item.getDataVencimento()) : "-")
					.append("</td>")
					.append("<td align='right' style='border-bottom:1px solid #eee;'>R$ ")
					.append(String.format("%,.2f", item.getValor() != null ? item.getValor() : 0.))
					.append("</td></tr>");
		}
		sb.append("</table>");

		sb.append("<p style='font-size:15px;color:#212121;margin-top:20px;'><b>Total em aberto: R$ ")
				.append(String.format("%,.2f", valorTotal)).append("</b></p>");

		sb.append("<div class='info-box'>")
				.append("Não está com o boleto em mãos? Solicite a 2ª via pelo nosso WhatsApp oficial:<br/><br/>")
				.append("<a href='").append(linkWhatsapp(cliente))
				.append("' target='_blank' style='display:inline-block;background:#25D366;color:#fff;")
				.append("text-decoration:none;padding:10px 22px;border-radius:6px;font-weight:bold;font-size:13px;'>")
				.append("Falar no WhatsApp — ").append(WHATSAPP_EMPRESA_EXIBICAO).append("</a>")
				.append("</div>");

		sb.append("<div class='warn-box'>")
				.append("<b>Cuidado com golpes:</b> não realizamos cobranças por ligação de número desconhecido ")
				.append("nem pedimos dados bancários, senha, cartão ou chave PIX por telefone/WhatsApp. Fale sempre ")
				.append("pelo número oficial acima e, na dúvida, confirme com a empresa antes de pagar qualquer ")
				.append("boleto recebido por outro canal.</div>");

		sb.append("<p style='font-size:12px;color:#757575;margin-top:16px;'>")
				.append("Essa informação é baseada em relatório do dia ").append(getDataRelatorioFormatada(itens))
				.append(" e caso o pagamento já tenha sido efetuado, por favor desconsidere este aviso.</p>");

		return sb.toString();
	}

	/** Data em que o banco gerou o relatório (lida do próprio CSV) — cai para a data de
	 * importação só em lotes antigos, de antes de existir essa captura. */
	private String getDataRelatorioFormatada(List<RelatorioCobrancaItem> itens) {
		if (itens.isEmpty() || itens.get(0).getRelatorioCobranca() == null) {
			return "-";
		}
		RelatorioCobranca relatorio = itens.get(0).getRelatorioCobranca();
		Date data = relatorio.getDataCriacaoArquivo() != null ? relatorio.getDataCriacaoArquivo()
				: relatorio.getDataImportacao();
		return data != null ? new SimpleDateFormat("dd/MM/yyyy").format(data) : "-";
	}

	private String linkWhatsapp(Cliente cliente) {
		String mensagem = "Olá! Sou " + cliente.getNomeRazaoSocial()
				+ " e gostaria de solicitar a 2ª via do meu boleto.";
		try {
			return "https://wa.me/" + WHATSAPP_EMPRESA_E164 + "?text=" + URLEncoder.encode(mensagem, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "https://wa.me/" + WHATSAPP_EMPRESA_E164;
		}
	}

}
