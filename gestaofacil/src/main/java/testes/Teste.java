package testes;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import util.AdicionarLetraEPenultimaVirgula;

public class Teste {

	public static String limpar(String expressao) {
		List<String> textos = new ArrayList<>();
		for (int x = 0; x < expressao.length(); x++) {
			char ca = expressao.charAt(x);
			if (ca == '@') {
				String textoSubstituir = "";
				for (int y = x; y < expressao.length(); y++) {
					if (expressao.charAt(y) != ';') {
						textoSubstituir += expressao.charAt(y);
					} else {
						textoSubstituir += ";";
						break;
					}
				}
				textos.add(textoSubstituir);
			}
		}
		for (String s : textos) {
			expressao = expressao.replace(s, "");
		}
		return expressao;
	}

	public static void main(String[] args) {
		DateFormat formatD=new SimpleDateFormat("d");
		DateFormat formatM=new SimpleDateFormat("M");
		DateFormat formatA=new SimpleDateFormat("yyyy");
		Calendar c = Calendar.getInstance();
		Date data = c.getTime();
		Locale brasil = new Locale("pt", "BR"); //Retorna do país e a língua
		DateFormat f2 = DateFormat.getDateInstance(DateFormat.FULL, brasil);
		System.out.println(f2.format(new Date()));
	}
	
	private static String replaceOccurance(String text, String replaceFrom, String replaceTo, int occuranceIndex)
	{
	    StringBuffer sb = new StringBuffer();
	    Pattern p = Pattern.compile(replaceFrom);
	    Matcher m = p.matcher(text);
	    int count = 0;
	    while (m.find())
	    {
	        if (count++ == occuranceIndex - 1)
	        {
	            m.appendReplacement(sb, replaceTo);
	        }
	    }
	    m.appendTail(sb);
	    return sb.toString();
	}

}
