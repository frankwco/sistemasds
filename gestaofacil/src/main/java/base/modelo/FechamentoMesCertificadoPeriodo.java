package base.modelo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FechamentoMesCertificadoPeriodo {

	private Integer anoInicial;
	private Integer anoFinal;
	private Integer mesInicial = 1;
	private Integer mesFinal=12;
	private Date dataAtual = new Date();
	
	public Integer getAnoInicial() {
		if(anoInicial == null) {
			DateFormat format= new SimpleDateFormat("yyyy");
			anoInicial = Integer.parseInt(format.format(dataAtual).toString());
		}
		return anoInicial;
	}
	public void setAnoInicial(Integer anoInicial) {
		this.anoInicial = anoInicial;
	}
	public Integer getAnoFinal() {
		if(anoFinal == null) {
			DateFormat format= new SimpleDateFormat("yyyy");
			anoFinal = Integer.parseInt(format.format(dataAtual).toString());
		}
		return anoFinal;
	}
	public void setAnoFinal(Integer anoFinal) {
		this.anoFinal = anoFinal;
	}
	public Integer getMesInicial() {
		return mesInicial;
	}
	public void setMesInicial(Integer mesInicial) {
		this.mesInicial = mesInicial;
	}
	public Integer getMesFinal() {
		return mesFinal;
	}
	public void setMesFinal(Integer mesFinal) {
		if(mesFinal == null) {
			DateFormat format= new SimpleDateFormat("MM");
			mesFinal = Integer.parseInt(format.format(dataAtual).toString());
		}
		this.mesFinal = mesFinal;
	}
	
	
}
