package testes;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

public class TesteBi {
	public static void main(String[] s) {
		Calendar dataPesquisaInicial = Calendar.getInstance();
		dataPesquisaInicial.setTime(new Date());	
		dataPesquisaInicial.set(Calendar.MONTH, 6);
		System.out.println(dataPesquisaInicial.get(Calendar.MONTH));
		System.out.println(dataPesquisaInicial.DAY_OF_MONTH);
	}
}
