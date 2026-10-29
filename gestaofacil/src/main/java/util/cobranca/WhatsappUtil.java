package util.cobranca;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Monta o link "wa.me" pra abrir uma conversa do WhatsApp já com uma mensagem preenchida. */
public class WhatsappUtil {

	private WhatsappUtil() {
	}

	/** Só dígitos, com o 55 do Brasil na frente (se o telefone cadastrado já não tiver). */
	public static String normalizarNumero(String telefone) {
		if (telefone == null) {
			return "";
		}
		String digitos = telefone.replaceAll("[^0-9]", "");
		if (digitos.isEmpty()) {
			return "";
		}
		// DDD + número (10 ou 11 dígitos) sem o código do país ainda
		if (digitos.length() == 10 || digitos.length() == 11) {
			digitos = "55" + digitos;
		}
		return digitos;
	}

	/** @return o link wa.me, ou null se o telefone estiver vazio/não reconhecível. */
	public static String montarLink(String telefone, String mensagem) {
		String numero = normalizarNumero(telefone);
		if (numero.length() < 12) {
			return null;
		}
		try {
			return "https://wa.me/" + numero + "?text=" + URLEncoder.encode(mensagem, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "https://wa.me/" + numero;
		}
	}

}
