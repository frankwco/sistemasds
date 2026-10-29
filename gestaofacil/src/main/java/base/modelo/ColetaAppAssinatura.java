package base.modelo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.validator.constraints.Length;

import util.FormataDataMB;
import util.FormatarMB;

@Entity
@Table(name = "coleta_app_assinatura")
public class ColetaAppAssinatura implements Serializable {

	public ColetaAppAssinatura() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	private ColetaApp coletaApp;
	
	private String assinatura="";
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataInclusao=new Date();
	
	
	@Lob
	@Column(length=100000)
	private byte[] assinaturaByte;
	
	private Boolean status=true;	
	
	@Column(length = 4000)
	private String clienteEnderecoCompleto;
	@Column(length = 4000)
	private String pontoColetaEnderecoCompleto;
	@Column(length = 4000)
	private String dadosMotoristaVeiculo;
	@Column(length = 4000)
	private String lixoColetado;
	
	private String codigoImpressao;
	
	private String situacao;
	
	
	public String getSituacao() {
		return situacao;
	}


	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}


	public String getCodigoImpressao() {
		return codigoImpressao;
	}


	public void setCodigoImpressao(String codigoImpressao) {
		this.codigoImpressao = codigoImpressao;
	}


	public Boolean getStatus() {
		return status;
	}


	public void setStatus(Boolean status) {
		this.status = status;
	}


	public ColetaApp getColetaApp() {
		return coletaApp;
	}


	public void setColetaApp(ColetaApp coletaApp) {
		this.coletaApp = coletaApp;
	}


	public Date getDataInclusao() {
		return dataInclusao;
	}


	public void setDataInclusao(Date dataInclusao) {
		this.dataInclusao = dataInclusao;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String getAssinatura() {
		return assinatura;
	}


	public void setAssinatura(String assinatura) {
		this.assinatura = assinatura;
	}


	public byte[] getAssinaturaByte() {
		return assinaturaByte;
	}


	public void setAssinaturaByte(byte[] assinaturaByte) {
		this.assinaturaByte = assinaturaByte;
	}
	
	public String getClienteEnderecoCompleto() {
		return clienteEnderecoCompleto;
	}
	public void setClienteEnderecoCompleto(String clienteEnderecoCompleto) {
		this.clienteEnderecoCompleto = clienteEnderecoCompleto;
	}
	public String getPontoColetaEnderecoCompleto() {
		return pontoColetaEnderecoCompleto;
	}
	public void setPontoColetaEnderecoCompleto(String pontoColetaEnderecoCompleto) {
		this.pontoColetaEnderecoCompleto = pontoColetaEnderecoCompleto;
	}
	public String getDadosMotoristaVeiculo() {
		return dadosMotoristaVeiculo;
	}
	public void setDadosMotoristaVeiculo(String dadosMotoristaVeiculo) {
		this.dadosMotoristaVeiculo = dadosMotoristaVeiculo;
	}
	
	


	public String getLixoColetado() {
		return lixoColetado;
	}


	public void setLixoColetado(String lixoColetado) {
		this.lixoColetado = lixoColetado;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assinatura == null) ? 0 : assinatura.hashCode());
		result = prime * result + Arrays.hashCode(assinaturaByte);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		ColetaAppAssinatura other = (ColetaAppAssinatura) obj;
		if (assinatura == null) {
			if (other.assinatura != null)
				return false;
		} else if (!assinatura.equals(other.assinatura))
			return false;
		if (!Arrays.equals(assinaturaByte, other.assinaturaByte))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	

	
	

}
