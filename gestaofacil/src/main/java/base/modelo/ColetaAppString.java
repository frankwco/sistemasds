package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import util.FormataDataMB;
import util.FormatarMB;


public class ColetaAppString {

	public ColetaAppString() {
		super();
	}


	private Long id;
	
	
	private Boolean importado=false;
	
	
	private String dataUltimaColeta;
	
	private String dataUltimaColetaString="";
	
	
	private String dataSincronizacao;
	
	private String observacao;
	
	
	//private PontoColeta pontoColetaSelecionado;
	
	
	private Veiculo veiculo;
	
	
	private String emailUsuario;
	
	private Double a;
	private Double e;
	private Double b;
	private Double a2;
	private Double a3;
	private Double totalColetado;

	private Boolean status=true;
	
	private String identificadorApp;
	
	private String latitude;
	private String longitude;
	
	private Boolean sincronizado=false;
	private Boolean alterado=true;
	
	
	private String assinatura="";
	
	private String tipo="";
	
	private String emailNovo="";
	
	private String emailEnviadoPara="";
	
	
	
	
	
	private Integer diaUltimaColeta=0;
	private Integer mesUltimaColeta=0;
	private Integer anoUltimaColeta=0;
	

	private String diaMesAno="0";
	
	private Integer horaUltimaColeta=0;
	private Integer minutoUltimaColeta=0;
	private Integer segundoUltimaColeta=0;
	

	private Boolean acessoTotal;
	
	public Boolean getImportado() {
		return importado;
	}
	public void setImportado(Boolean importado) {
		this.importado = importado;
	}
	public String getEmailEnviadoPara() {
		return emailEnviadoPara;
	}
	public void setEmailEnviadoPara(String emailEnviadoPara) {
		this.emailEnviadoPara = emailEnviadoPara;
	}
	public String getDiaMesAno() {
		return diaMesAno;
	}
	public void setDiaMesAno(String diaMesAno) {
		this.diaMesAno = diaMesAno;
	}
	public String getEmailNovo() {
		return emailNovo;
	}
	public void setEmailNovo(String emailNovo) {
		this.emailNovo = emailNovo;
	}
	public Boolean getAcessoTotal() {
		if(acessoTotal==null) {
			acessoTotal=false;
		}
		return acessoTotal;
	}
	public void setAcessoTotal(Boolean acessoTotal) {
		this.acessoTotal = acessoTotal;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}
	public String getDataUltimaColetaString() {
		return dataUltimaColetaString;
	}
	public void setDataUltimaColetaString(String dataUltimaColetaString) {
		this.dataUltimaColetaString = dataUltimaColetaString;
	}
	public Integer getHoraUltimaColeta() {
		if(horaUltimaColeta==null) {
			horaUltimaColeta=0;
		}
		return horaUltimaColeta;
	}
	public void setHoraUltimaColeta(Integer horaUltimaColeta) {
		this.horaUltimaColeta = horaUltimaColeta;
	}
	public Integer getMinutoUltimaColeta() {
		if(minutoUltimaColeta==null) {
			minutoUltimaColeta=0;
		}
		return minutoUltimaColeta;
	}
	public void setMinutoUltimaColeta(Integer minutoUltimaColeta) {
		this.minutoUltimaColeta = minutoUltimaColeta;
	}
	public Integer getSegundoUltimaColeta() {
		if(segundoUltimaColeta==null) {
			segundoUltimaColeta=0;
		}
		return segundoUltimaColeta;
	}
	public void setSegundoUltimaColeta(Integer segundoUltimaColeta) {
		this.segundoUltimaColeta = segundoUltimaColeta;
	}
	
	public Veiculo getVeiculo() {
		return veiculo;
	}
	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}
	
	
	
	public Double getTotalColetado() {
		return totalColetado;
	}
	public void setTotalColetado(Double totalColetado) {
		this.totalColetado = totalColetado;
	}
	public String getAssinatura() {
		return assinatura;
	}
	public void setAssinatura(String assinatura) {
		this.assinatura = assinatura;
	}
	public Integer getDiaUltimaColeta() {
		return diaUltimaColeta;
	}
	public void setDiaUltimaColeta(Integer diaUltimaColeta) {
		this.diaUltimaColeta = diaUltimaColeta;
	}
	public Integer getMesUltimaColeta() {
		return mesUltimaColeta;
	}
	public void setMesUltimaColeta(Integer mesUltimaColeta) {
		this.mesUltimaColeta = mesUltimaColeta;
	}
	public Integer getAnoUltimaColeta() {
		return anoUltimaColeta;
	}
	public void setAnoUltimaColeta(Integer anoUltimaColeta) {
		this.anoUltimaColeta = anoUltimaColeta;
	}
	public Boolean getAlterado() {
		return alterado;
	}
	public void setAlterado(Boolean alterado) {
		this.alterado = alterado;
	}
	public Boolean getSincronizado() {
		return sincronizado;
	}
	public void setSincronizado(Boolean sincronizado) {
		this.sincronizado = sincronizado;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getObservacao() {
		return observacao;
	}
	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	
	
//	public PontoColeta getPontoColetaSelecionado() {
//		return pontoColetaSelecionado;
//	}
//	public void setPontoColetaSelecionado(PontoColeta pontoColetaSelecionado) {
//		this.pontoColetaSelecionado = pontoColetaSelecionado;
//	}
	public String getEmailUsuario() {
		return emailUsuario;
	}
	public void setEmailUsuario(String emailUsuario) {
		this.emailUsuario = emailUsuario;
	}
	public Double getA() {
		return a;
	}
	public void setA(Double a) {
		this.a = a;
	}
	public Double getE() {
		return e;
	}
	public void setE(Double e) {
		this.e = e;
	}
	public Double getB() {
		return b;
	}
	public void setB(Double b) {
		this.b = b;
	}
	public Double getA2() {
		return a2;
	}
	public void setA2(Double a2) {
		this.a2 = a2;
	}
	public Double getA3() {
		return a3;
	}
	public void setA3(Double a3) {
		this.a3 = a3;
	}
	public Boolean getStatus() {
		return status;
	}
	public void setStatus(Boolean status) {
		this.status = status;
	}
	public String getIdentificadorApp() {
		if(identificadorApp==null) {
			identificadorApp="";
		}
		return identificadorApp;
	}
	public void setIdentificadorApp(String identificadorApp) {
		this.identificadorApp = identificadorApp;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getDataUltimaColeta() {
		return dataUltimaColeta;
	}
	public void setDataUltimaColeta(String dataUltimaColeta) {
		this.dataUltimaColeta = dataUltimaColeta;
	}
	public String getDataSincronizacao() {
		return dataSincronizacao;
	}
	public void setDataSincronizacao(String dataSincronizacao) {
		this.dataSincronizacao = dataSincronizacao;
	}
	
	

	

}
