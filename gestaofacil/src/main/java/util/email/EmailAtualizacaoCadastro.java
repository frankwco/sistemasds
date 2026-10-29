package util.email;

import base.modelo.Cliente;

public class EmailAtualizacaoCadastro {

public static String getMensagemAtualizacaoCadastroEmail(Cliente c, String senha) {
StringBuilder corpo = new StringBuilder();

// -- greeting
corpo.append("<p style='color:#424242;font-size:15px;line-height:1.7;margin:0 0 24px;'>")
     .append("Ol&aacute;, <strong>").append(c.getNomeRazaoSocial()).append("</strong>. ")
     .append("O seu cadastro foi atualizado com sucesso.")
     .append("</p>");

// -- portal access
corpo.append("<p class='section-label'>Acesso ao Portal</p>")
     .append("<div style='background:#e8f5e9;border-radius:6px;padding:16px 20px;margin-bottom:24px;'>")
     .append("<p style='color:#424242;font-size:14px;line-height:1.7;margin:0 0 10px;'>")
     .append("Acesse <a href='https://dsresiduos.com.br' style='color:#2e7d32;font-weight:bold;text-decoration:none;'>dsresiduos.com.br</a> ")
     .append("e clique em <em>&ldquo;Voc&ecirc; &eacute; nosso cliente?&rdquo;</em> para visualizar pesos coletados, romaneios, ")
     .append("certificado de coleta, contrato e muito mais.")
     .append("</p>")
     .append("<p style='color:#424242;font-size:14px;margin:0;'>")
     .append("Utilize seu e-mail e a senha: <strong style='color:#2e7d32;font-size:16px;'>").append(senha).append("</strong>")
     .append("</p>")
     .append("</div>");

// -- MTR reminder
corpo.append("<div class='warn-box'>")
     .append("<p style='color:#e65100;font-weight:bold;font-size:14px;margin:0 0 8px;'>&#9888; Lembrete &mdash; Lan&ccedil;amento no SINIR/MTR</p>")
     .append("<p style='color:#5d4037;font-size:13px;line-height:1.6;margin:0 0 8px;'>")
     .append("Voc&ecirc; deve realizar o lan&ccedil;amento das coletas na plataforma do Governo Federal (SINIR-MTR), ")
     .append("sob pena de multa em caso de omiss&atilde;o. Conforme a <strong>Portaria N&ordm; 280/2020</strong> do Minist&eacute;rio do Meio Ambiente, ")
     .append("a obrigatoriedade &eacute; do <strong>gerador de res&iacute;duo</strong>.")
     .append("</p>")
     .append("<p style='color:#5d4037;font-size:13px;line-height:1.6;margin:0 0 8px;'>")
     .append("Para emitir o MTR acesse: <a href='https://mtr.sinir.gov.br/#/' style='color:#1565c0;font-weight:bold;text-decoration:none;'>")
     .append("mtr.sinir.gov.br</a> utilizando suas credenciais.")
     .append("</p>")
     .append("<p style='color:#5d4037;font-size:13px;margin:0;'>")
     .append("No nosso site voc&ecirc; encontra um <strong>tutorial passo a passo</strong>.")
     .append("</p>")
     .append("</div>");

// -- footer links
corpo.append("<div style='border-top:1px solid #e0e0e0;padding-top:20px;'>")
     .append("<p style='color:#616161;font-size:13px;margin:0 0 6px;'>D&uacute;vidas? Ligue: <strong>(44) 9 9932-3410</strong></p>")
     .append("<p style='color:#616161;font-size:13px;margin:0;'>Somos gratos pela parceria. Muito obrigado!</p>")
     .append("</div>");

return EmailTemplate.build(corpo.toString());
}

}