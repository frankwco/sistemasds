package util.email;

/**
 * Template base para todos os e-mails HTML da DS Resíduos.
 *
 * Uso simples:
 *   return EmailTemplate.build(corpoHtml);
 *
 * Uso com cor de cabeçalho personalizada (ex.: azul para outro módulo):
 *   return EmailTemplate.build("#1565c0", corpoHtml);
 *
 * Cada e-mail monta apenas o <corpoHtml> — o bloco de conteúdo que vai
 * dentro do card, sem se preocupar com DOCTYPE, estilos, cabeçalho ou rodapé.
 */
public class EmailTemplate {

	private static final String COR_PADRAO = "#2e7d32";

	/** Monta o e-mail completo usando a cor padrão (verde DS Resíduos). */
	public static String build(String corpoHtml) {
		return build(COR_PADRAO, corpoHtml);
	}

	/**
	 * Monta o e-mail completo com uma cor de cabeçalho personalizada.
	 *
	 * @param corCabecalhoHex cor hexadecimal do cabeçalho, ex: "#2e7d32"
	 * @param corpoHtml       conteúdo interno do card (apenas o miolo)
	 */
	public static String build(String corCabecalhoHex, String corpoHtml) {
		StringBuilder sb = new StringBuilder();

		// ── head + estilos globais ─────────────────────────────────────────────────
		sb.append("<!DOCTYPE html><html lang='pt-BR'>")
		  .append("<head>")
		  .append("<meta charset='UTF-8'/>")
		  .append("<meta name='viewport' content='width=device-width,initial-scale=1'/>")
		  .append("<style>")
		  .append("body{margin:0;padding:0;background:#f0f2f5;font-family:Arial,Helvetica,sans-serif;}")
		  .append(".wrapper{width:100%;background:#f0f2f5;padding:20px 0;}")
		  .append(".card{width:100%;max-width:760px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden;border:1px solid #ddd;}")
		  .append(".body-pad{padding:32px 40px;}")
		  .append(".nps-btn{display:inline-block;width:44px;height:44px;line-height:44px;border-radius:50%;")
		  .append("text-align:center;font-weight:bold;font-size:15px;text-decoration:none;color:#fff;margin:3px;}")
		  .append(".info-box{background:#e3f2fd;border-radius:4px;padding:12px 16px;margin:16px 0 24px;}")
		  .append(".warn-box{background:#fff8e1;border-left:5px solid #ffa000;border-radius:4px;padding:16px 20px;margin-bottom:24px;}")
		  .append(".section-label{color:#212121;font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;")
		  .append("border-bottom:2px solid #e0e0e0;padding-bottom:8px;margin:0 0 12px;}")
		  .append("@media only screen and (max-width:620px){")
		  .append(".card{border-radius:0;border-left:none;border-right:none;}")
		  .append(".body-pad{padding:20px 16px;}")
		  .append(".nps-btn{width:34px;height:34px;line-height:34px;font-size:13px;margin:2px;}")
		  .append("table.residuos td,table.residuos th{padding:8px 8px!important;font-size:12px!important;}")
		  .append("}")
		  .append("</style>")
		  .append("</head>")
		  .append("<body>")
		  .append("<div class='wrapper'>")
		  .append("<div class='card'>")

		  // ── cabeçalho colorido ─────────────────────────────────────────────────
		  .append("<div style='background:").append(corCabecalhoHex).append(";padding:26px 40px;text-align:center;'>")
		  .append("<h1 style='color:#fff;margin:0;font-size:24px;letter-spacing:2px;font-weight:bold;'>DS Res&iacute;duos</h1>")
		  .append("<p style='color:#a5d6a7;margin:6px 0 0;font-size:11px;letter-spacing:1px;text-transform:uppercase;'>")
		  .append("Gest&atilde;o de Res&iacute;duos de Sa&uacute;de</p>")
		  .append("</div>")

		  // ── conteúdo do e-mail ─────────────────────────────────────────────────
		  .append("<div class='body-pad'>")
		  .append(corpoHtml)
		  .append("</div>")

		  // ── rodapé ────────────────────────────────────────────────────────────
		  .append("<div style='background:#f5f5f5;padding:16px 40px;text-align:center;border-top:3px solid ").append(corCabecalhoHex).append(";'>")
		  .append("<p style='color:#9e9e9e;font-size:11px;margin:0;'>")
		  .append("DS Res&iacute;duos &mdash; Gest&atilde;o Ambiental Respons&aacute;vel</p>")
		  .append("<p style='color:#bdbdbd;font-size:10px;margin:4px 0 0;'>dsresiduos.com.br</p>")
		  .append("</div>")

		  .append("</div>")  // close .card
		  .append("</div>")  // close .wrapper
		  .append("</body></html>");

		return sb.toString();
	}
}
