package util;



public class EnviarEmailIF {
	public static Boolean enviarEmail(String destinatario, String titulo, String mensagem) {
	
		Boolean flag = false;
		
		
		
		//try {
//		//	SimpleEmail email = new SimpleEmail();
//			// Utilize o hostname do seu provedor de email
//			//System.out.println("alterando hostname...");
//			email.setHostName("smtp.gmail.com");
//			// Quando a porta utilizada não é a padrão (gmail = 465)
//			email.setSmtpPort(465);
//			// Adicione os destinatórios
//System.out.println("DES : "+destinatario);
//			email.addTo(destinatario);
//
//			// Configure o seu email do qual enviar�
//			email.setFrom("sistemasifprparanavai@gmail.com");;
//			// Adicione um assunto
//			email.setSubject(titulo);
//			// Adicione a mensagem do email
//			email.setMsg(mensagem);
//			// Para autenticar no servidor � necess�rio chamar os dois métodos
//			// abaixo
//		//	System.out.println("autenticando...");
//			email.setSSL(true);
//	//		email.setAuthentication("questionarioifprparanavai@gmail.com", "aquelagwh28");
//			email.setAuthentication("sistemasifprparanavai@gmail.com", "!@#sistemas159/*");
//		//	System.out.println("enviando...");
//			email.send();
//		//	System.out.println("Email enviado!");
//			
//			flag = true;
//		
//		} catch (EmailException e) {
//			e.printStackTrace();
//			ExibirMensagem.exibirMensagem(Mensagem.ERRO_EMAIL +" "+e);
//
//		}
//		
		
		if (flag) {
	
			return true;
		} else {
		
			return false;
		}
	}
}
