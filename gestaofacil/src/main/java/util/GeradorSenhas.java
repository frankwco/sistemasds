package util;

import java.util.Random;

public class GeradorSenhas {
	public static String gerarSenha() {
		Random random = new Random();
		String resultado = "";
		String valores[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "14","12","53","27","19" };
		for (int i = 0; i <= 21; i++) {
			int a = random.nextInt(valores.length);
			resultado += valores[a];
		}
		return resultado;
	}
}
