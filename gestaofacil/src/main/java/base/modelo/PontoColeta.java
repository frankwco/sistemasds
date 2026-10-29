package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import base.modelo.Pessoa;
import dao.GenericDAO;

@Entity
@Table(name = "ponto_coleta")
@PrimaryKeyJoinColumn(name = "id")
public class PontoColeta extends Pessoa implements Serializable {

	public PontoColeta() {
		super();
	}

	// atributos da classe usuario + serialversionUID auto
	private static final long serialVersionUID = 1L;
//	@Id
//	@GeneratedValue(strategy = GenerationType.AUTO)
//	private Long id;

	private Boolean acessoSistema = false;
	//private Boolean status;

	private Boolean emitirContrato = true;
	//private Integer diaVencimentoFatura = 10;
	@ManyToOne
	private Cliente cliente;

	private String razaoSocialCliente = "";

	private String modeloCadastroPonto;

	@ManyToOne
	private Rota rota;

	private String nomeRota;

	private Integer ordem = 1;

	private String periodicidadeColeta="Quinzenal";

	private String gruposColetados="";

	private Boolean contratoAtivo = true;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataPenultimaColeta;
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataUltimaColeta;
	
	private Integer diaUltimaColeta=0;
	private Integer mesUltimaColeta=0;
	private Integer anoUltimaColeta=0;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataUltimaAlteracao = new Date();
	
	
	private Integer horaUltimaColeta=0;
	private Integer minutoUltimaColeta=0;
	private Integer segundoUltimaColeta=0;
	
	private Integer quantidadeColetaMes=0;
	private Integer quantidadeColetadaMes=0;
	private Integer mesColeta=0;
	private Integer anoColeta=0;
	
	private String aviso="";
	
	
	private Boolean situacaoAviso=false;
	
	private Boolean primeiraColeta=true;
	


	public Boolean getPrimeiraColeta() {
		if(primeiraColeta==null) {
			primeiraColeta=false;
		}
		return primeiraColeta;
	}
	public void setPrimeiraColeta(Boolean primeiraColeta) {
		this.primeiraColeta = primeiraColeta;
	}
	public Boolean getSituacaoAviso() {
		if(situacaoAviso==null) {
			situacaoAviso=false;
		}
		return situacaoAviso;
	}
	public void setSituacaoAviso(Boolean situacaoAviso) {
		this.situacaoAviso = situacaoAviso;
	}
	public String getAviso() {
		if(aviso==null) {
			aviso="";
		}
		return aviso;
	}
	public void setAviso(String aviso) {
		this.aviso = aviso;
	}
	public Integer getQuantidadeColetaMes() {
		if(quantidadeColetaMes==null) {
			quantidadeColetaMes=0;
		}
		return quantidadeColetaMes;
	}
	public void setQuantidadeColetaMes(Integer quantidadeColetaMes) {
		this.quantidadeColetaMes = quantidadeColetaMes;
	}
	public Integer getQuantidadeColetadaMes() {
		if(quantidadeColetadaMes==null) {
			quantidadeColetadaMes=0;
		}		
		return quantidadeColetadaMes;
	}
	public void setQuantidadeColetadaMes(Integer quantidadeColetadaMes) {
		this.quantidadeColetadaMes = quantidadeColetadaMes;
	}
	public Integer getMesColeta() {
		return mesColeta;
	}
	public void setMesColeta(Integer mesColeta) {
		this.mesColeta = mesColeta;
	}
	public Integer getAnoColeta() {
		return anoColeta;
	}
	public void setAnoColeta(Integer anoColeta) {
		this.anoColeta = anoColeta;
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

	public Date getDataUltimaAlteracao() {
		return dataUltimaAlteracao;
	}

	public void setDataUltimaAlteracao(Date dataUltimaAlteracao) {
		this.dataUltimaAlteracao = dataUltimaAlteracao;
	}

	public Integer getDiaUltimaColeta() {
		if(diaUltimaColeta==null) {
			diaUltimaColeta=0;
		}
		return diaUltimaColeta;
	}

	public void setDiaUltimaColeta(Integer diaUltimaColeta) {
		this.diaUltimaColeta = diaUltimaColeta;
	}

	public Integer getMesUltimaColeta() {
		if(mesUltimaColeta==null) {
			mesUltimaColeta=0;
		}
		return mesUltimaColeta;
	}

	public void setMesUltimaColeta(Integer mesUltimaColeta) {
		this.mesUltimaColeta = mesUltimaColeta;
	}

	public Integer getAnoUltimaColeta() {
		if(anoUltimaColeta==null) {
			anoUltimaColeta=0;
		}
		return anoUltimaColeta;
	}

	public void setAnoUltimaColeta(Integer anoUltimaColeta) {
		this.anoUltimaColeta = anoUltimaColeta;
	}

	public Date getDataPenultimaColeta() {
		return dataPenultimaColeta;
	}

	public void setDataPenultimaColeta(Date dataPenultimaColeta) {
		this.dataPenultimaColeta = dataPenultimaColeta;
	}

	public Date getDataUltimaColeta() {
		return dataUltimaColeta;
	}

	public void setDataUltimaColeta(Date dataUltimaColeta) {
		this.dataUltimaColeta = dataUltimaColeta;
	}

	public Boolean getContratoAtivo() {
		return contratoAtivo;
	}

	public void setContratoAtivo(Boolean contratoAtivo) {
		this.contratoAtivo = contratoAtivo;
	}

	public String getGruposColetados() {
		if(gruposColetados==null) {
			gruposColetados="";
		}
		return gruposColetados;
	}

	public void setGruposColetados(String gruposColetados) {
		this.gruposColetados = gruposColetados;
	}

	public String getNomeRota() {
		return nomeRota;
	}

	public void setNomeRota(String nomeRota) {
		this.nomeRota = nomeRota;
	}

	public Rota getRota() {
		return rota;
	}

	public void setRota(Rota rota) {
		this.rota = rota;
	}

	public Integer getOrdem() {
		if (ordem == null) {
			ordem = 0;
		}
		return ordem;
	}

	public void setOrdem(Integer ordem) {
		this.ordem = ordem;
	}

	public String getPeriodicidadeColeta() {
		if(periodicidadeColeta==null) {
			periodicidadeColeta="Quinzenal";
		}
		return periodicidadeColeta;
	}

	public void setPeriodicidadeColeta(String periodicidadeColeta) {
		this.periodicidadeColeta = periodicidadeColeta;
	}

	public String getRazaoSocialCliente() {
		return razaoSocialCliente;
	}

	public void setRazaoSocialCliente(String razaoSocialCliente) {
		this.razaoSocialCliente = razaoSocialCliente;
	}

	public Boolean getAcessoSistema() {
		return acessoSistema;
	}

	public void setAcessoSistema(Boolean acessoSistema) {
		this.acessoSistema = acessoSistema;
	}

	/*public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}*/

	public Boolean getEmitirContrato() {
		return emitirContrato;
	}

	public void setEmitirContrato(Boolean emitirContrato) {
		this.emitirContrato = emitirContrato;
	}

	/*public Integer getDiaVencimentoFatura() {
		return diaVencimentoFatura;
	}

	public void setDiaVencimentoFatura(Integer diaVencimentoFatura) {
		this.diaVencimentoFatura = diaVencimentoFatura;
	}*/

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public String getModeloCadastroPonto() {
		return modeloCadastroPonto;
	}

	public void setModeloCadastroPonto(String modeloCadastroPonto) {
		this.modeloCadastroPonto = modeloCadastroPonto;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((acessoSistema == null) ? 0 : acessoSistema.hashCode());
		result = prime * result + ((cliente == null) ? 0 : cliente.hashCode());
		//result = prime * result + ((diaVencimentoFatura == null) ? 0 : diaVencimentoFatura.hashCode());
		result = prime * result + ((emitirContrato == null) ? 0 : emitirContrato.hashCode());
		result = prime * result + ((modeloCadastroPonto == null) ? 0 : modeloCadastroPonto.hashCode());
		result = prime * result + ((nomeRota == null) ? 0 : nomeRota.hashCode());
		result = prime * result + ((ordem == null) ? 0 : ordem.hashCode());
		result = prime * result + ((periodicidadeColeta == null) ? 0 : periodicidadeColeta.hashCode());
		result = prime * result + ((razaoSocialCliente == null) ? 0 : razaoSocialCliente.hashCode());
		result = prime * result + ((rota == null) ? 0 : rota.hashCode());
		//result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PontoColeta other = (PontoColeta) obj;
		if (acessoSistema == null) {
			if (other.acessoSistema != null)
				return false;
		} else if (!acessoSistema.equals(other.acessoSistema))
			return false;
		if (cliente == null) {
			if (other.cliente != null)
				return false;
		} else if (!cliente.equals(other.cliente))
			return false;
		/*if (diaVencimentoFatura == null) {
			if (other.diaVencimentoFatura != null)
				return false;
		} else if (!diaVencimentoFatura.equals(other.diaVencimentoFatura))
			return false;*/
		if (emitirContrato == null) {
			if (other.emitirContrato != null)
				return false;
		} else if (!emitirContrato.equals(other.emitirContrato))
			return false;
		if (modeloCadastroPonto == null) {
			if (other.modeloCadastroPonto != null)
				return false;
		} else if (!modeloCadastroPonto.equals(other.modeloCadastroPonto))
			return false;
		if (nomeRota == null) {
			if (other.nomeRota != null)
				return false;
		} else if (!nomeRota.equals(other.nomeRota))
			return false;
		if (ordem == null) {
			if (other.ordem != null)
				return false;
		} else if (!ordem.equals(other.ordem))
			return false;
		if (periodicidadeColeta == null) {
			if (other.periodicidadeColeta != null)
				return false;
		} else if (!periodicidadeColeta.equals(other.periodicidadeColeta))
			return false;
		if (razaoSocialCliente == null) {
			if (other.razaoSocialCliente != null)
				return false;
		} else if (!razaoSocialCliente.equals(other.razaoSocialCliente))
			return false;
		if (rota == null) {
			if (other.rota != null)
				return false;
		} else if (!rota.equals(other.rota))
			return false;
		/*if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;*/
		return true;
	}
	@Override
	public String toString() {
		return "PontoColeta [razaoSocialCliente=" + razaoSocialCliente + "]";
	}
	
	

}
