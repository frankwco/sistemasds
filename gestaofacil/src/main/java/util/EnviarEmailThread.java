package util;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Properties;

import javax.inject.Inject;
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
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;


import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import base.modelo.LogTarefas;
import base.service.LogTarefasService;

@Component
@Scope("prototype")
public class EnviarEmailThread implements Runnable {
	String destinatario;
	String titulo;
	String mensagem;


	public EnviarEmailThread(String destinatario, String titulo, String mensagem) {
		this.destinatario = destinatario;
		this.titulo = titulo;
		this.mensagem = mensagem;
	}

	public void run() { // Rotina de envio dos Emails
		Boolean flag = false;
		LogTarefas log = new LogTarefas();
		log.setDataRegistro(new Date());
		log.setDescricao("Envio de email de coleta para " + destinatario + " - título: " + titulo);
		log.setTipo("envio e-mail");
		
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
			flag=true;
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
			log.setDescricao(log.getDescricao() + " - erro: " + e);
		}

//		try {
//			SimpleEmail email = new SimpleEmail();
//			// Utilize o hostname do seu provedor de email
//			// System.out.println("alterando hostname...");
//			email.setHostName("smtp.gmail.com");
//			// Quando a porta utilizada não é a padrão (gmail = 465)
//			email.setSmtpPort(465);
//			// Adicione os destinatórios
//
//			email.addTo(destinatario);
//
//			// Configure o seu email do qual enviar�
//			email.setFrom("gestaolite@gmail.com");
//			;
//			// Adicione um assunto
//			email.setSubject(titulo);
//			// Adicione a mensagem do email
//			
//			email.setMsg(mensagem);
//			// Para autenticar no servidor � necess�rio chamar os dois métodos
//			// abaixo
//			// System.out.println("autenticando...");
//			email.setSSL(true);
//			// email.setAuthentication("questionarioifprparanavai@gmail.com",
//			// "aquelagwh28");
//			email.setAuthentication("gestaolite@gmail.com", "litegwh28");
//			// System.out.println("enviando...");
//			email.send();
//			// System.out.println("Email enviado!");
//
//			flag = true;
//
//		} catch (EmailException e) {
//			log.setDescricao(log.getDescricao() + " - erro: " + e);
//			e.printStackTrace();
//			// ExibirMensagem.exibirMensagem(Mensagem.ERRO_EMAIL + " " + e);
//
//		}

		if (flag) {
			log.setErro(false);

			// return true;
		} else {
			log.setErro(true);
			// return false;
		}
		//System.out.println("Início: "+new Date());
		EntityManagerFactory fa = Persistence.createEntityManagerFactory("cronosPU");

		EntityManager em = fa.createEntityManager();
		em.getTransaction().begin();

		em.persist(log);
		
		em.getTransaction().commit();
		em.close();
		fa.close();
		//System.out.println("Fim: "+new Date());
		//logTarefasService.inserirAlterar(log);
	}

}
