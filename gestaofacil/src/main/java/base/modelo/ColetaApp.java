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

@Entity
@Table(name = "coleta_app")
public class ColetaApp implements Serializable {

	public ColetaApp() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Boolean importado = false;

	// Vínculo com a Coleta gerada a partir desta ColetaApp (quando já
	// importada). Permite achar direto a Coleta pra propagar exclusão/edição,
	// sem precisar recasar por ponto+data (ambíguo se houver mais de uma
	// coleta no mesmo ponto no mesmo dia).
	private Long coletaId;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataUltimaColeta = new Date();

	private String dataUltimaColetaString = "";

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataSincronizacao = new Date();

	private String observacao;

	@ManyToOne
	private PontoColeta pontoColetaSelecionado;

	@ManyToOne
	private Veiculo veiculo;

	private String nomeMotorista = "";
	private String emailMotorista = "";

	private String emailUsuario;

	private Double a;
	private Double e;
	private Double b;
	private Double a2;
	private Double a3;
	private Double a4;
	private Double totalColetado;

	private Boolean status = true;

	private String identificadorApp;

	private String latitude;
	private String longitude;

	private Boolean sincronizado = false;
	private Boolean alterado = true;

	private String assinatura = "";

	private String tipo = "";

	private String emailNovo = "";

	private String emailEnviadoPara = "";

	private String versaoApp;
	
	private String situacao;

	@Lob
	@Column(length = 100000)
	private byte[] assinaturaByte;

	private Integer diaUltimaColeta = 0;
	private Integer mesUltimaColeta = 0;
	private Integer anoUltimaColeta = 0;

	@Column(unique = true)
	private String diaMesAno = "0";

	private Integer horaUltimaColeta = 0;
	private Integer minutoUltimaColeta = 0;
	private Integer segundoUltimaColeta = 0;

	@Transient
	private Boolean acessoTotal;
	
	
	public Double getA4() {
		return a4;
	}

	public void setA4(Double a4) {
		this.a4 = a4;
	}

	public String getSituacao() {
		if (situacao == null) {
			situacao = "";
		}
		return situacao;
	}

	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}

	public String getVersaoApp() {
		if (versaoApp == null) {
			versaoApp = "";
		}
		return versaoApp;
	}

	public void setVersaoApp(String versaoApp) {
		this.versaoApp = versaoApp;
	}

	public String getNomeMotorista() {
		if (nomeMotorista == null) {
			nomeMotorista = "";
		}
		return nomeMotorista;
	}

	public void setNomeMotorista(String nomeMotorista) {
		this.nomeMotorista = nomeMotorista;
	}

	public String getEmailMotorista() {
		if (emailMotorista == null) {
			emailMotorista = "";
		}
		return emailMotorista;
	}

	public void setEmailMotorista(String emailMotorista) {
		this.emailMotorista = emailMotorista;
	}

	public Boolean getImportado() {
		return importado;
	}

	public void setImportado(Boolean importado) {
		this.importado = importado;
	}

	public Long getColetaId() {
		return coletaId;
	}

	public void setColetaId(Long coletaId) {
		this.coletaId = coletaId;
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
		if (acessoTotal == null) {
			acessoTotal = false;
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
		if (horaUltimaColeta == null) {
			horaUltimaColeta = 0;
		}
		return horaUltimaColeta;
	}

	public void setHoraUltimaColeta(Integer horaUltimaColeta) {
		this.horaUltimaColeta = horaUltimaColeta;
	}

	public Integer getMinutoUltimaColeta() {
		if (minutoUltimaColeta == null) {
			minutoUltimaColeta = 0;
		}
		return minutoUltimaColeta;
	}

	public void setMinutoUltimaColeta(Integer minutoUltimaColeta) {
		this.minutoUltimaColeta = minutoUltimaColeta;
	}

	public Integer getSegundoUltimaColeta() {
		if (segundoUltimaColeta == null) {
			segundoUltimaColeta = 0;
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

	public Date getDataSincronizacao() {
		return dataSincronizacao;
	}

	public void setDataSincronizacao(Date dataSincronizacao) {
		this.dataSincronizacao = dataSincronizacao;
	}

	public byte[] getAssinaturaByte() {
		return assinaturaByte;
	}

	public void setAssinaturaByte(byte[] assinaturaByte) {
		this.assinaturaByte = assinaturaByte;
	}

	public Double getTotalColetado() {
		return totalColetado;
	}

	public void setTotalColetado(Double totalColetado) {
		this.totalColetado = totalColetado;
	}

	public String getAssinatura() {
		if (assinatura == null) {
			assinatura = "";
		}
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

	public Date getDataUltimaColeta() {
		return dataUltimaColeta;
	}

	public void setDataUltimaColeta(Date dataUltimaColeta) {
		this.dataUltimaColeta = dataUltimaColeta;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public PontoColeta getPontoColetaSelecionado() {
		return pontoColetaSelecionado;
	}

	public void setPontoColetaSelecionado(PontoColeta pontoColetaSelecionado) {
		this.pontoColetaSelecionado = pontoColetaSelecionado;
	}

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
		if (identificadorApp == null) {
			identificadorApp = "";
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

	/** Link do Google Maps para o local do registro da coleta; null se nao houver localizacao valida. */
	public String getUrlMapa() {
		if (latitude == null || longitude == null) {
			return null;
		}
		String lat = latitude.trim().replace(',', '.');
		String lng = longitude.trim().replace(',', '.');
		try {
			double dLat = Double.parseDouble(lat);
			double dLng = Double.parseDouble(lng);
			if (dLat == 0 && dLng == 0) {
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return "https://www.google.com/maps?q=" + lat + "," + lng;
	}

	public String getDataUsuarioVeiculo() {
		String retorno = "";
		retorno = "Data: " + new FormataDataMB().formatarDataHora(dataUltimaColeta) + "<br/>";
		retorno += "Usuário: "+ " (" + getVersaoApp() + ")" + emailUsuario + "<br/>";
		retorno += "Veículo: " + veiculo.getDescricao() + " / " + veiculo.getPlaca() + "<br/>";
		retorno += "Motorista: " + getNomeMotorista() + " / " + getEmailMotorista() + "<br/>";
		retorno += "Situação: " + getSituacao();
		return retorno;
	}

	public String getDadosTransporte() {
		StringBuilder retorno = new StringBuilder();
		retorno.append("<b>Veículo:</b> ");
		retorno.append(veiculo.getDescricao() != null ? veiculo.getDescricao() : "");
		retorno.append(" / ");
		retorno.append("Placa: " + veiculo.getPlaca() != null ? veiculo.getPlaca() : "");
		retorno.append(" ");
		if (!getNomeMotorista().equals("")) {
			retorno.append("<b>Motorista:</b> ");
			retorno.append(getNomeMotorista());

		}
		return retorno.toString();
	}

	public String getSoData() {
		String retorno = "";
		retorno = new FormataDataMB().formatarDataHora(dataUltimaColeta);

		return retorno;
	}

	public String getClienteEndereco() {
		return getPontoColetaSelecionado().getCliente().getNomeRazaoSocial() + "<br/>"
				+ getPontoColetaSelecionado().getCliente().getEnderecoCompleto();
	}

	public String getPontoColetaEndereco() {
		return getPontoColetaSelecionado().getNomeRazaoSocial() + "<br/>"
				+ getPontoColetaSelecionado().getEnderecoCompleto();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((a2 == null) ? 0 : a2.hashCode());
		result = prime * result + ((a3 == null) ? 0 : a3.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + ((dataUltimaColeta == null) ? 0 : dataUltimaColeta.hashCode());
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((emailUsuario == null) ? 0 : emailUsuario.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((identificadorApp == null) ? 0 : identificadorApp.hashCode());
		result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
		result = prime * result + ((observacao == null) ? 0 : observacao.hashCode());
		result = prime * result + ((pontoColetaSelecionado == null) ? 0 : pontoColetaSelecionado.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColetaApp other = (ColetaApp) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (a2 == null) {
			if (other.a2 != null)
				return false;
		} else if (!a2.equals(other.a2))
			return false;
		if (a3 == null) {
			if (other.a3 != null)
				return false;
		} else if (!a3.equals(other.a3))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (dataUltimaColeta == null) {
			if (other.dataUltimaColeta != null)
				return false;
		} else if (!dataUltimaColeta.equals(other.dataUltimaColeta))
			return false;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (emailUsuario == null) {
			if (other.emailUsuario != null)
				return false;
		} else if (!emailUsuario.equals(other.emailUsuario))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (identificadorApp == null) {
			if (other.identificadorApp != null)
				return false;
		} else if (!identificadorApp.equals(other.identificadorApp))
			return false;
		if (latitude == null) {
			if (other.latitude != null)
				return false;
		} else if (!latitude.equals(other.latitude))
			return false;
		if (longitude == null) {
			if (other.longitude != null)
				return false;
		} else if (!longitude.equals(other.longitude))
			return false;
		if (observacao == null) {
			if (other.observacao != null)
				return false;
		} else if (!observacao.equals(other.observacao))
			return false;
		if (pontoColetaSelecionado == null) {
			if (other.pontoColetaSelecionado != null)
				return false;
		} else if (!pontoColetaSelecionado.equals(other.pontoColetaSelecionado))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		return true;
	}

}
