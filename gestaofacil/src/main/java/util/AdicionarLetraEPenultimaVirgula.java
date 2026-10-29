package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdicionarLetraEPenultimaVirgula {
	public static String substituirOcorrencia(String text, String replaceFrom, String replaceTo, int occuranceIndex) {
		text = new StringBuffer(text).reverse().toString();
		StringBuffer sb = new StringBuffer();
		Pattern p = Pattern.compile(replaceFrom);
		Matcher m = p.matcher(text);
		int count = 0;
		while (m.find()) {
			if (count++ == occuranceIndex - 1) {
				m.appendReplacement(sb, replaceTo);
			}
		}
		m.appendTail(sb);
		return sb.reverse().toString();
	}
}
