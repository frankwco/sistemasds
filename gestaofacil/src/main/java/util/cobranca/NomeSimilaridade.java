package util.cobranca;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Confere, por aproximação, se o nome do sacado que veio no relatório do banco parece ser o
 * mesmo cliente que o código de contrato já resolveu — como segunda checagem além do código,
 * já que o cliente que criou o registro no banco pode ter digitado o "Seu Número" errado.
 *
 * Não é uma comparação exata: nomes vêm com diferenças de pontuação, abreviação, "&" que às
 * vezes falta, "LTDA"/"ME" que às vezes não vem, etc. Por isso normaliza os dois lados e mede
 * a distância de edição (Levenshtein) sobre o texto já normalizado.
 */
public class NomeSimilaridade {

	private static final double LIMIAR_SIMILARIDADE = 0.55;

	private static final Pattern SUFIXOS_EMPRESARIAIS = Pattern
			.compile("\\b(LTDA|ME|EPP|EIRELI|SA|S A|SS|CIA|MATRIZ|FILIAL)\\b");

	private NomeSimilaridade() {
	}

	/** true se o nome do relatório parecer ser o mesmo cliente (razão social OU nome fantasia). */
	public static boolean pareceMesmoNome(String nomeRelatorio, String nomeRazaoSocial, String nomeFantasia) {
		String a = normalizar(nomeRelatorio);
		if (a.isEmpty()) {
			return true; // sem nome pra comparar, não reprova ninguém por falta de dado
		}
		return similar(a, normalizar(nomeRazaoSocial)) || similar(a, normalizar(nomeFantasia));
	}

	private static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String limpo = semAcento.toUpperCase().replaceAll("[^A-Z0-9 ]", " ");
		limpo = SUFIXOS_EMPRESARIAIS.matcher(limpo).replaceAll(" ");
		return limpo.replaceAll("\\s+", " ").trim();
	}

	private static boolean similar(String a, String b) {
		if (a.isEmpty() || b.isEmpty()) {
			return false;
		}
		if (a.equals(b)) {
			return true;
		}
		String menor = a.length() <= b.length() ? a : b;
		String maior = a.length() <= b.length() ? b : a;
		if (menor.length() >= 4 && maior.contains(menor)) {
			return true;
		}
		int distancia = levenshtein(a, b);
		int maiorTamanho = Math.max(a.length(), b.length());
		double similaridade = 1.0 - ((double) distancia / maiorTamanho);
		return similaridade >= LIMIAR_SIMILARIDADE;
	}

	private static int levenshtein(String a, String b) {
		int[] anterior = new int[b.length() + 1];
		int[] atual = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) {
			anterior[j] = j;
		}
		for (int i = 1; i <= a.length(); i++) {
			atual[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
			}
			int[] troca = anterior;
			anterior = atual;
			atual = troca;
		}
		return anterior[b.length()];
	}

}
