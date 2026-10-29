package testes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Teste2 {

	public static void main(String[] args) {
		Date dataTeste = new Date();

		Calendar cal = Calendar.getInstance(); 
		cal.setTime(dataTeste ); 
		cal.add(Calendar.DATE, 168);
		dataTeste = cal.getTime();
		System.out.println(dataTeste);
	}

}
