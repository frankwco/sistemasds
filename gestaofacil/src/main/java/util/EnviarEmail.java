package util;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class EnviarEmail {
	public static Boolean enviarEmail(String destinatario, String titulo, String mensagem) {
		Boolean flag = false;
		try {	Properties prop = new Properties();
		prop.put("mail.smtp.host", "smtp.hostinger.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "587");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //prop.put("mail.smtp.starttls.enable", "true");
		//prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");
		Session session = Session.getInstance(prop, new Authenticator() {	
		    @Override
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication("nao-responda@dsresiduos.com.br", "h5ai-aui1-lsvb-gtnu");
		    }
		});
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress("nao-responda@dsresiduos.com.br"));
		message.setRecipients(
		  Message.RecipientType.TO, InternetAddress.parse(destinatario));
		message.setSubject(titulo);

		String msg = mensagem;

		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(msg, "text/html");

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mimeBodyPart);

		message.setContent(multipart);

		
			Transport.send(message);
		} catch (Exception e) {
			System.out.println("AQUii");
			// TODO Auto-generated catch block
			e.printStackTrace();
			ExibirMensagem.exibirMensagem(Mensagem.ERRO_EMAIL +" "+e);
			return false;
		}
		return true;
	}
}
