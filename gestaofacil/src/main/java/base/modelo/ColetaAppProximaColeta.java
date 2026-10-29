package base.modelo;

import java.util.Date;


public class ColetaAppProximaColeta {

	public ColetaAppProximaColeta() {
		super();
	}

	private PontoColeta pontoColeta;
	private Date dataProximaColeta=null;
	private String mensagem="";
	
	public PontoColeta getPontoColeta() {
		return pontoColeta;
	}
	public void setPontoColeta(PontoColeta pontoColeta) {
		this.pontoColeta = pontoColeta;
	}
	public Date getDataProximaColeta() {
		return dataProximaColeta;
	}
	public void setDataProximaColeta(Date dataProximaColeta) {
		this.dataProximaColeta = dataProximaColeta;
	}
	public String getMensagem() {
		return mensagem;
	}
	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
	
	


	

}
