package util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Formatacoes {
	
	public static String formatarData(Date dt) {
		if (dt != null) {
			DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
			return format.format(dt);

		} else {
			return "";
		}
	}
	
	public static String formatarInvertido(Date dt) {
		if (dt != null) {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			return format.format(dt);

		} else {
			return "";
		}
	}

	public static String formatarNumeroTresCasasDecimais(Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
		return new DecimalFormat("#,###.000", symbols).format(valor);
	}

	public static String formatarNumeroDuasCasasDecimais(Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
		return new DecimalFormat("#,###.00", symbols).format(valor);
	}
}
