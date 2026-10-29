package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RetornarDatas {
	
	public static void main(String[] args) {
//		System.out.println(RetornarDatas().ultimoDiaMes(4, 2020));
	}
	
	
	/**retorna uma data com o ultimo dia do mes no formato yyyy-MM-dd**/
	public static String ultimoDiaMes(int mes, int ano) {
		 Calendar cal = Calendar.getInstance();
		    cal.set(Calendar.MONTH, (mes-1));
		    cal.set(Calendar.YEAR, ano);
		    cal.set(Calendar.DAY_OF_MONTH, 1);// This is necessary to get proper results
		    cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
		    cal.getTime();
		   
		    return dataFormatada(cal.getTime());
		    
	}
	
	/**retorna uma data com o ultimo dia do mes no formato yyyy-MM-dd**/
	public static String dataFormatada(Date dataFormatar) {
	
		    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		    return format.format(dataFormatar);
		    
	}

}
