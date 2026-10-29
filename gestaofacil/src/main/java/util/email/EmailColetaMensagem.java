package util.email;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import base.modelo.ColetaApp;
import util.FormatarMB;

public class EmailColetaMensagem {
	
	
	public static String getMensagemComMTROld(ColetaApp co) {
		StringBuilder mensagem = new StringBuilder();
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		
		mensagem.append("Olá, estamos enviando o relatório da coleta de resíduos realizada no dia "
		        + format.format(co.getDataUltimaColeta()) + ".<br/><br/>");

		mensagem.append("<h2 style='color:red;'>ATENÇÃO: Alterações importantes em ARMAZENADOR TEMPORÁRIO, TRANSPORTADOR e DESTINADOR</h2><br/><br/>");

		mensagem.append("<h3>Informações atualizadas (Setembro/2025) sobre o preenchimento do MTR (Manifesto de Transporte de Resíduos):</h3>");
		mensagem.append("<p>Essas mudanças impactam diretamente o uso do <strong>SINIR (PR)</strong> e do <strong>SIGOR (SP)</strong>.</p>");

		mensagem.append("<p>Para acessar o documento completo em PDF, clique no link abaixo:</p>");
		mensagem.append("<p><a href='https://dsresiduos.com.br/mtr-sinir.php' target='_blank'>dsresiduos.com.br/mtr-sinir.php</a></p><br/>");

		mensagem.append("<p><strong>Ponto de coleta:</strong> " 
		        + co.getPontoColetaSelecionado().getNomeRazaoSocialNomeFantasiaEndereco() + " - " 
		        + co.getPontoColetaSelecionado().getCpfCnpj() + ".</p><br/>");

		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A")) {
		    mensagem.append("Grupo A1: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getA()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getA()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("E")) {
		    mensagem.append("Grupo E: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getE()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getE()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("B")) {
		    mensagem.append("Grupo B: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getB()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getB()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A2")) {
		    mensagem.append("Grupo A2: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getA2()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getA2()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A3")) {
		    mensagem.append("Grupo A3: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getA3()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getA3()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A4")) {
		    mensagem.append("Grupo A4: " 
		            + new FormatarMB().formatarNumeroTresCasasDecimais(co.getA4()) + " | " 
		            + new FormatarMB().formatarNumeroQuatroCasasDecimaisComMTR(co.getA4()) + "<br/>");
		}

		mensagem.append("<p><em>Observação: O site do MTR/SINIR exige 4 dígitos após a vírgula. "
		        + "Para pesos <strong>abaixo de 100g</strong> (0,1000 no SINIR) não é necessária a emissão do manifesto.</em></p><br/>");

		mensagem.append("<h3>Dados do Transporte</h3>");
		mensagem.append("<p>" + co.getDadosTransporte() + "</p><br/>");

		mensagem.append("<p>Em caso de dúvidas ou se você quiser acompanhar as suas coletas, fechamentos, contratos e outras informações, acesse: "
		        + "<a href='https://dsresiduos.com.br' target='_blank'>dsresiduos.com.br</a></p>");

		mensagem.append("<p>Em caso de dúvidas, estamos à disposição pelo telefone: (44) 9 9932-3410.</p>");

		mensagem.append("<p>Somos gratos pela parceria. Muito obrigado!</p>");


		
		return mensagem.toString();
	}
	
	public static String getMensagemComMTR(ColetaApp co) {
		StringBuilder m = new StringBuilder();
		DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
		FormatarMB fmb = new FormatarMB();
		String grupos = co.getPontoColetaSelecionado().getGruposColetados();

		// ── greeting ──────────────────────────────────────────────────────────────
		m.append("<p style='color:#424242;font-size:15px;line-height:1.7;margin:0 0 24px;'>")
		 .append("Ol&aacute;, estamos enviando o <strong>relat&oacute;rio de coleta de res&iacute;duos</strong> realizada no dia ")
		 .append("<strong style='color:#2e7d32;'>").append(fmt.format(co.getDataUltimaColeta())).append("</strong>.")
		 .append("</p>")

		 // ── alert box ─────────────────────────────────────────────────────────────
		 .append("<div style='background:#fff8e1;border-left:5px solid #ffa000;border-radius:4px;padding:16px 20px;margin-bottom:24px;'>")
		 .append("<p style='color:#e65100;font-weight:bold;font-size:14px;margin:0 0 8px;'>&#9888; Aten&ccedil;&atilde;o &mdash; Altera&ccedil;&otilde;es Importantes</p>")
		 .append("<p style='color:#5d4037;font-size:13px;line-height:1.6;margin:0 0 6px;'>")
		 .append("Informa&ccedil;&otilde;es atualizadas <strong>(Setembro/2025)</strong> sobre o preenchimento do MTR ")
		 .append("(Manifesto de Transporte de Res&iacute;duos). Altera&ccedil;&otilde;es em <strong>ARMAZENADOR TEMPOR&Aacute;RIO</strong>, ")
		 .append("<strong>TRANSPORTADOR</strong> e <strong>DESTINADOR</strong>.</p>")
		 .append("<p style='color:#5d4037;font-size:13px;line-height:1.6;margin:0 0 10px;'>")
		 .append("Essas mudan&ccedil;as impactam diretamente o uso do <strong>SINIR (PR)</strong> e do <strong>SIGOR (SP)</strong>.</p>")
		 .append("<p style='font-size:13px;margin:0;'>&#128196; Documento completo em PDF: ")
		 .append("<a href='https://dsresiduos.com.br/mtr-sinir.php' style='color:#1565c0;font-weight:bold;text-decoration:none;'>")
		 .append("dsresiduos.com.br/mtr-sinir.php</a></p>")
		 .append("</div>")

		 // ── collection point ──────────────────────────────────────────────────────
		 .append("<div style='background:#e8f5e9;border-radius:6px;padding:14px 20px;margin-bottom:24px;'>")
		 .append("<p style='color:#1b5e20;font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;margin:0 0 6px;'>")
		 .append("Ponto de Coleta</p>")
		 .append("<p style='color:#2e7d32;font-size:15px;font-weight:bold;margin:0 0 4px;'>")
		 .append(co.getPontoColetaSelecionado().getNomeRazaoSocialNomeFantasiaEndereco()).append("</p>")
		 .append("<p style='color:#616161;font-size:13px;margin:0;'>CPF/CNPJ: <strong>")
		 .append(co.getPontoColetaSelecionado().getCpfCnpj()).append("</strong></p>")
		 .append("</div>")

		 // ── residues table ────────────────────────────────────────────────────────
		 .append("<p style='color:#212121;font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;")
		 .append("border-bottom:2px solid #e0e0e0;padding-bottom:8px;margin:0 0 12px;'>Res&iacute;duos Coletados</p>")
		 .append("<div style='overflow-x:auto;'>")
		 .append("<table class='residuos' width='100%' cellpadding='0' cellspacing='0' style='border-collapse:collapse;font-size:14px;margin-bottom:8px;min-width:320px;'>")
		 .append("<tr style='background:#f5f5f5;'>")
		 .append("<th style='text-align:left;color:#757575;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;padding:10px 14px;border-bottom:2px solid #e0e0e0;'>Grupo</th>")
		 .append("<th style='text-align:right;color:#757575;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;padding:10px 14px;border-bottom:2px solid #e0e0e0;'>Quantidade</th>")
		 .append("<th style='text-align:right;color:#757575;font-size:11px;text-transform:uppercase;letter-spacing:0.5px;padding:10px 14px;border-bottom:2px solid #e0e0e0;'>Informar MTR (4 d&iacute;gitos ap&oacute;s a v&iacute;rgula</strong>)</th>")
		 .append("</tr>");

		if (grupos.contains("A")) {
			m.append("<tr>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo A1</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getA())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getA())).append("</td>")
			 .append("</tr>");
		}
		if (grupos.contains("E")) {
			m.append("<tr style='background:#fafafa;'>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo E</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getE())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getE())).append("</td>")
			 .append("</tr>");
		}
		if (grupos.contains("B")) {
			m.append("<tr>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo B</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getB())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getB())).append("</td>")
			 .append("</tr>");
		}
		if (grupos.contains("A2")) {
			m.append("<tr style='background:#fafafa;'>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo A2</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getA2())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getA2())).append("</td>")
			 .append("</tr>");
		}
		if (grupos.contains("A3")) {
			m.append("<tr>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo A3</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getA3())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getA3())).append("</td>")
			 .append("</tr>");
		}
		if (grupos.contains("A4")) {
			m.append("<tr style='background:#fafafa;'>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#333;font-weight:bold;'>Grupo A4</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#555;text-align:right;'>")
			 .append(fmb.formatarNumeroTresCasasDecimais(co.getA4())).append("</td>")
			 .append("<td style='padding:11px 14px;border-bottom:1px solid #f0f0f0;color:#2e7d32;font-weight:bold;text-align:right;'>")
			 .append(fmb.formatarNumeroQuatroCasasDecimaisComMTR(co.getA4())).append("</td>")
			 .append("</tr>");
		}

		m.append("</table>")
		 .append("</div>") // end overflow wrapper

		 // ── MTR note ──────────────────────────────────────────────────────────────
		 .append("<div style='background:#e3f2fd;border-radius:4px;padding:12px 16px;margin:16px 0 24px;'>")
		 .append("<p style='color:#1565c0;font-size:13px;line-height:1.6;margin:0;'>")
		 .append("<strong>&#8505; Observa&ccedil;&atilde;o:</strong> O site do MTR/SINIR exige <strong>4 d&iacute;gitos ap&oacute;s a v&iacute;rgula</strong>. ")
		 .append("Para pesos <strong>abaixo de 100&nbsp;g</strong> (0,1000 no SINIR) n&atilde;o &eacute; necess&aacute;ria a emiss&atilde;o do manifesto.")
		 .append("</p>")
		 .append("</div>")

		 // ── transport data ────────────────────────────────────────────────────────
		 .append("<p style='color:#212121;font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;")
		 .append("border-bottom:2px solid #e0e0e0;padding-bottom:8px;margin:0 0 12px;'>Dados do Transporte</p>")
		 .append("<p style='color:#424242;font-size:14px;line-height:1.7;margin:0 0 28px;'>")
		 .append(co.getDadosTransporte()).append("</p>")

		 // ── NPS survey ────────────────────────────────────────────────────────────
		 .append("<div style='background:#f9f9f9;border:1px solid #e0e0e0;border-radius:8px;padding:24px 20px;margin-bottom:28px;text-align:center;'>")
		 .append("<p style='color:#1b5e20;font-size:11px;font-weight:bold;text-transform:uppercase;letter-spacing:1px;margin:0 0 6px;'>")
		 .append("&#11088; Pesquisa de Satisfa&ccedil;&atilde;o</p>")
		 .append("<p style='color:#212121;font-size:16px;font-weight:bold;margin:0 0 6px;'>")
		 .append("Como voc&ecirc; avalia o nosso servi&ccedil;o?</p>")
		 .append("<p style='color:#757575;font-size:13px;margin:0 0 20px;'>")
		 .append("Clique em um n&uacute;mero de 0 a 10</p>")
		 .append("<div style='text-align:center;'>")
		 // detractors 0-6 (red shades)
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=0&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#c62828;'>0</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=1&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#d32f2f;'>1</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=2&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#e53935;'>2</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=3&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#f44336;'>3</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=4&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#ef6c00;'>4</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=5&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#f57c00;'>5</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=6&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#fb8c00;'>6</a>")
		 // passives 7-8 (yellow/amber)
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=7&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#f9a825;'>7</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=8&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#f9a825;'>8</a>")
		 // promoters 9-10 (green)
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=9&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#388e3c;'>9</a>")
		 .append("<a href='https://alcsistemas.com.br/gestaofacil/rest/avaliacaoNPS/registrar?nota=10&coletaId=").append(co.getId()).append("' class='nps-btn' style='background:#2e7d32;'>10</a>")
		 .append("</div>")
		 .append("<table width='100%' cellpadding='0' cellspacing='0' style='margin-top:10px;'><tr>")
		 .append("<td style='text-align:left;'><span style='color:#e53935;font-size:11px;font-weight:bold;'>Muito Insatisfeito</span></td>")
		 .append("<td style='text-align:right;'><span style='color:#2e7d32;font-size:11px;font-weight:bold;'>Muito Satisfeito</span></td>")
		 .append("</tr></table>")
		 .append("</div>")

		 // ── footer links ──────────────────────────────────────────────────────────
		 .append("<div style='border-top:1px solid #e0e0e0;padding-top:20px;'>")
		 .append("<p style='color:#616161;font-size:13px;line-height:1.7;margin:0 0 6px;'>")
		 .append("Acompanhe suas coletas, fechamentos, contratos e outras informa&ccedil;&otilde;es em ")
		 .append("<a href='https://dsresiduos.com.br' style='color:#2e7d32;font-weight:bold;text-decoration:none;'>")
		 .append("dsresiduos.com.br</a>.</p>")
		 .append("<p style='color:#616161;font-size:13px;margin:0;'>D&uacute;vidas? Ligue: <strong>(44) 9 9932-3410</strong></p>")
		 .append("</div>");

		return EmailTemplate.build(m.toString());
	}

	public static String getMensagemSemMTR(ColetaApp co) {
		StringBuilder mensagem = new StringBuilder();
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		
		 mensagem.append("Olá, estamos enviando o relatório da coleta de resíduos realizada no dia "
				+ format.format(co.getDataUltimaColeta()) + "." + "<br/><br/>");

		mensagem.append("Ponto de coleta: " + co.getPontoColetaSelecionado().getNomeRazaoSocial() + " - "
				+ co.getPontoColetaSelecionado().getCpfCnpj() + ".<br/><br/>");

		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A")) {
			mensagem.append("Grupo A1...........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("E")) {
			mensagem.append("Grupo E...........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getE()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("B")) {
			mensagem.append("Grupo B...........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getB()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A2")) {
			mensagem.append("Grupo A2...........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA2()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A3")) {
			mensagem.append("Grupo A3...........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA3()) + "<br/>");
		}

		mensagem.append("<br/>Você pode acompanhar as suas coletas, fechamentos, contratos e outras informações acessando o site dsresiduos.com.br<br/><br/>");

		mensagem.append("Em caso de dúvida estamos a disposição - (44) 9 9932-3410");

		
		return mensagem.toString();
	}
	
	public static String getColetas(ColetaApp co) {
		StringBuilder mensagem = new StringBuilder();
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
	

		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A")) {
			mensagem.append("Grupo A1..........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("E")) {
			mensagem.append("Grupo E............: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getE()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("B")) {
			mensagem.append("Grupo B............: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getB()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A2")) {
			mensagem.append("Grupo A2..........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA2()) + "<br/>");
		}
		if (co.getPontoColetaSelecionado().getGruposColetados().contains("A3")) {
			mensagem.append("Grupo A3..........: "
					+ new FormatarMB().formatarNumeroTresCasasDecimais(co.getA3()) + "<br/>");
		}

		mensagem.append("<br/>Total..................: "+new FormatarMB().formatarNumeroTresCasasDecimais(co.getA()+co.getE()+co.getB()+co.getA2()+co.getA3()));

		

		
		return mensagem.toString();
	}

}
