package base.modelo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import base.modelo.Pessoa;
import dao.GenericDAO;

@Entity
@Table(name = "informacoes_financeiras")
public class InformacaoFinanceira implements Serializable {

	public InformacaoFinanceira() {
		super();
	}

	// atributos da classe usuario + serialversionUID auto
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	private PontoColeta pontoColetaGerarContrato;

	@ManyToOne
	private Cliente cliente;

	private String formaPagamento = "Mensal";
	
	

//	private Boolean vigente = true;
	@Temporal(TemporalType.DATE)
	private Date dataInicial = new Date();
	@Temporal(TemporalType.DATE)
	private Date dataFinal = new Date();
	private Date dataEmissaoContrato = new Date();
	private Date dataPrimeiroPagamento = new Date();
	private Date dataUltimoPagamento = new Date();
	private String aviso="";
	private Boolean situacaoAviso=false;

	private Boolean status;
	private String responsavel;
	private String existeValorFixoMensal = "não";
	private String cobraExcedente = "Não";
	private Double valorExcedente = 0.;
	private Double pesoMensal = 0.;
	private Double valorFixoMensalPonto = 0.;
	private Double valorFixoMensalTotal = 0.;
	private Boolean cobradoPorQuilo = false;
	private Double valorPorQuilo = 0.;
	private Integer diaVencimentoFatura;
	private String formaCobranca = "Anual";
	private String romaneioContratoComDadosDo;
	private boolean contratoGeradoPeloCliente = false;
	private boolean gerarContratoParaEstaInformacao = true;
	private Long geradoPelasInformacoesFinanceirasID;
	private Long idInformacaoFinanceiraUnico;
	private Integer codigoContratoAtual = 0;
	private String recebimentoBoletoDeposito;
	private String banco;
	private String agencia;
	private String conta;
	private Boolean comunicarEmail = true;
	private Integer quantidadeMesesContrato = 12;
	private String situacaoContrato;// A princípio vai ter a situação Ativo e Inativo e agora Renovado
	@Column(length = 5000)
	private String observacaoSituacaoContrato;

	private Boolean fechamentoDataDiferente = false;
	private Integer diaFechamento = 0;
	@Temporal(TemporalType.DATE)
	private Date dataUltimoFechamento;
	private Integer mesInicio = 0;
	private Integer anoInicio = 0;

	private String periodicidadeColeta = "Quinzenal";

	@ManyToOne
	private Contrato contrato;

	@Transient
	private String textoDocumentoContrato;

	@OneToMany(mappedBy = "informacoesFinanceiras", fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesFinanceirasPontoColeta;

	public List<ItensInformacaoFinanceiraPontoColeta> getListaItensInformacoesFinanceirasPontoColeta() {

		return listaItensInformacoesFinanceirasPontoColeta;
	}
	
	

	public String getListaItensInformacoesFinanceiraPontoColetaString() {
		String retorno = "Sem ponto";
		if (listaItensInformacoesFinanceirasPontoColeta != null
				&& listaItensInformacoesFinanceirasPontoColeta.size() > 0) {
			retorno = "";
			int contador = 1;
			for (int x = 0; x < listaItensInformacoesFinanceirasPontoColeta.size(); x++) {
				ItensInformacaoFinanceiraPontoColeta l = listaItensInformacoesFinanceirasPontoColeta.get(x);
				if (l.getPontoColeta() != null && l.getStatus() == true) {

					retorno += "<b>" + (contador) + "-</b> " + " (" + l.getPontoColeta().getId() + ") "
							+ l.getPontoColeta().getNomeRazaoSocialNomeFantasiaEndereco() + " ";
					contador++;
				}
			}
		}
		return retorno;
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
		return aviso;
	}



	public void setAviso(String aviso) {
		this.aviso = aviso;
	}



	public Boolean getFechamentoDataDiferente() {
		if (fechamentoDataDiferente == null) {
			fechamentoDataDiferente = false;
		}
		return fechamentoDataDiferente;
	}

	public void setFechamentoDataDiferente(Boolean fechamentoDataDiferente) {
		this.fechamentoDataDiferente = fechamentoDataDiferente;
	}

	public Integer getDiaFechamento() {
		if (diaFechamento == null) {
			diaFechamento = 0;
		}
		return diaFechamento;
	}

	public void setDiaFechamento(Integer diaFechamento) {
		this.diaFechamento = diaFechamento;
	}

	public Date getDataUltimoFechamento() {
		return dataUltimoFechamento;
	}

	public void setDataUltimoFechamento(Date dataUltimoFechamento) {
		this.dataUltimoFechamento = dataUltimoFechamento;
	}

	public Date getDataUltimoPagamento() {
		return dataUltimoPagamento;
	}

	public void setDataUltimoPagamento(Date dataUltimoPagamento) {
		this.dataUltimoPagamento = dataUltimoPagamento;
	}

	public String getObservacaoSituacaoContrato() {
		if (observacaoSituacaoContrato == null) {
			observacaoSituacaoContrato = "";
		}
		return observacaoSituacaoContrato;
	}

	public void setObservacaoSituacaoContrato(String observacaoSituacaoContrato) {
		this.observacaoSituacaoContrato = observacaoSituacaoContrato;
	}

	public String getSituacaoContrato() {
		if (situacaoContrato == null) {
			situacaoContrato = "Ativo";
		}
		return situacaoContrato;
	}

	public void setSituacaoContrato(String situacaoContrato) {
		this.situacaoContrato = situacaoContrato;
	}

	public Long getIdInformacaoFinanceiraUnico() {
		return idInformacaoFinanceiraUnico;
	}

	public void setIdInformacaoFinanceiraUnico(Long idInformacaoFinanceiraUnico) {
		this.idInformacaoFinanceiraUnico = idInformacaoFinanceiraUnico;
	}

	public String getFormaCobrancaRecebimento() {
		return "* " + formaCobranca + "<br/>* " + recebimentoBoletoDeposito + "<br/>";
	}

	public void setListaItensInformacoesFinanceirasPontoColeta(
			List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesFinanceirasPontoColeta) {
		this.listaItensInformacoesFinanceirasPontoColeta = listaItensInformacoesFinanceirasPontoColeta;
	}

	public Integer getQuantidadeMesesContrato() {
		return quantidadeMesesContrato;
	}

	public void setQuantidadeMesesContrato(Integer quantidadeMesesContrato) {
		this.quantidadeMesesContrato = quantidadeMesesContrato;
	}

	public Boolean getComunicarEmail() {
		return comunicarEmail;
	}

	public void setComunicarEmail(Boolean comunicarEmail) {
		this.comunicarEmail = comunicarEmail;
	}

	public String getBanco() {
		return banco;
	}

	public void setBanco(String banco) {
		this.banco = banco;
	}

	public String getAgencia() {
		return agencia;
	}

	public void setAgencia(String agencia) {
		this.agencia = agencia;
	}

	public String getConta() {
		return conta;
	}

	public void setConta(String conta) {
		this.conta = conta;
	}

	public String getRecebimentoBoletoDeposito() {
		return recebimentoBoletoDeposito;
	}

	public void setRecebimentoBoletoDeposito(String recebimentoBoletoDeposito) {
		this.recebimentoBoletoDeposito = recebimentoBoletoDeposito;
	}

	public Integer getCodigoContratoAtual() {
		if (codigoContratoAtual == null) {
			codigoContratoAtual = 0;
		}
		return codigoContratoAtual;
	}

	public void setCodigoContratoAtual(Integer codigoContratoAtual) {
		this.codigoContratoAtual = codigoContratoAtual;
	}

	public PontoColeta getPontoColetaGerarContrato() {
		return pontoColetaGerarContrato;
	}

	public void setPontoColetaGerarContrato(PontoColeta pontoColetaGerarContrato) {
		this.pontoColetaGerarContrato = pontoColetaGerarContrato;
	}

	public Date getDataEmissaoContrato() {
		return dataEmissaoContrato;
	}

	public void setDataEmissaoContrato(Date dataEmissaoContrato) {
		this.dataEmissaoContrato = dataEmissaoContrato;
	}

	public Date getDataPrimeiroPagamento() {
		return dataPrimeiroPagamento;
	}

	public void setDataPrimeiroPagamento(Date dataPrimeiroPagamento) {
		this.dataPrimeiroPagamento = dataPrimeiroPagamento;
	}

	public boolean isContratoGeradoPeloCliente() {
		return contratoGeradoPeloCliente;
	}

	public void setContratoGeradoPeloCliente(boolean contratoGeradoPeloCliente) {
		this.contratoGeradoPeloCliente = contratoGeradoPeloCliente;
	}

	public boolean isGerarContratoParaEstaInformacao() {
		return gerarContratoParaEstaInformacao;
	}

	public void setGerarContratoParaEstaInformacao(boolean gerarContratoParaEstaInformacao) {
		this.gerarContratoParaEstaInformacao = gerarContratoParaEstaInformacao;
	}

	public String getFormaCobranca() {
		return formaCobranca;
	}

	public void setFormaCobranca(String formaCobranca) {
		this.formaCobranca = formaCobranca;
	}

	public String getRomaneioContratoComDadosDo() {
		return romaneioContratoComDadosDo;
	}

	public void setRomaneioContratoComDadosDo(String romaneioContratoComDadosDo) {
		this.romaneioContratoComDadosDo = romaneioContratoComDadosDo;
	}

	public Long getGeradoPelasInformacoesFinanceirasID() {
		return geradoPelasInformacoesFinanceirasID;
	}

	public void setGeradoPelasInformacoesFinanceirasID(Long geradoPelasInformacoesFinanceirasID) {
		this.geradoPelasInformacoesFinanceirasID = geradoPelasInformacoesFinanceirasID;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

//	public Boolean getVigente() {
//		return vigente;
//	}
//
//	public void setVigente(Boolean vigente) {
//		this.vigente = vigente;
//	}

	public Date getDataInicial() {
		return dataInicial;
	}

	public void setDataInicial(Date dataInicial) {
		this.dataInicial = dataInicial;
	}

	public Date getDataFinal() {
		return dataFinal;
	}

	public void setDataFinal(Date dataFinal) {
		this.dataFinal = dataFinal;
	}

	public String getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(String responsavel) {
		this.responsavel = responsavel;
	}

	public String getExisteValorFixoMensal() {
		return existeValorFixoMensal;
	}

	public void setExisteValorFixoMensal(String existeValorFixoMensal) {
		this.existeValorFixoMensal = existeValorFixoMensal;
	}

	public String getCobraExcedente() {
		return cobraExcedente;
	}

	public void setCobraExcedente(String cobraExcedente) {
		this.cobraExcedente = cobraExcedente;
	}

	public Double getValorExcedente() {
		if (valorExcedente == null) {
			valorExcedente = 0.;
		}
		return valorExcedente;
	}

	public void setValorExcedente(Double valorExcedente) {
		this.valorExcedente = valorExcedente;
	}

	public Double getPesoMensal() {
		return pesoMensal;
	}

	public void setPesoMensal(Double pesoMensal) {
		this.pesoMensal = pesoMensal;
	}

	public String getPeriodicidadeColeta() {
		if (periodicidadeColeta == null) {
			periodicidadeColeta = "";
		}
		return periodicidadeColeta;
	}

	public void setPeriodicidadeColeta(String periodicidadeColeta) {
		this.periodicidadeColeta = periodicidadeColeta;
	}

	public Double getValorFixoMensalPonto() {
//		if(valorFixoMensalPonto==null) {
//			valor
//		}
		return valorFixoMensalPonto;
	}

	public void setValorFixoMensalPonto(Double valorFixoMensalPonto) {
		this.valorFixoMensalPonto = valorFixoMensalPonto;
	}

	public Double getValorFixoMensalTotal() {
		return valorFixoMensalTotal;
	}

	public void setValorFixoMensalTotal(Double valorFixoMensalTotal) {
		this.valorFixoMensalTotal = valorFixoMensalTotal;
	}

	public Boolean getCobradoPorQuilo() {
		return cobradoPorQuilo;
	}

	public void setCobradoPorQuilo(Boolean cobradoPorQuilo) {
		this.cobradoPorQuilo = cobradoPorQuilo;
	}

	public Double getValorPorQuilo() {
		return valorPorQuilo;
	}

	public void setValorPorQuilo(Double valorPorQuilo) {
		this.valorPorQuilo = valorPorQuilo;
	}

	public Integer getDiaVencimentoFatura() {
		return diaVencimentoFatura;
	}

	public void setDiaVencimentoFatura(Integer diaVencimentoFatura) {
		this.diaVencimentoFatura = diaVencimentoFatura;
	}

	public Contrato getContrato() {
		return contrato;
	}

	public void setContrato(Contrato contrato) {
		this.contrato = contrato;
	}

	public String getTextoDocumentoContrato() {
		return textoDocumentoContrato;
	}

	public void setTextoDocumentoContrato(String textoDocumentoContrato) {
		this.textoDocumentoContrato = textoDocumentoContrato;
	}

	public Integer getMesInicio() {
		return mesInicio;
	}

	public void setMesInicio(Integer mesInicio) {
		this.mesInicio = mesInicio;
	}

	public Integer getAnoInicio() {
		return anoInicio;
	}

	public void setAnoInicio(Integer anoInicio) {
		this.anoInicio = anoInicio;
	}

	public String getFormaPagamento() {
		if (formaPagamento == null) {
			formaPagamento = "Mensal";
		}
		return formaPagamento;
	}

	public void setFormaPagamento(String formaPagamento) {
		this.formaPagamento = formaPagamento;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agencia == null) ? 0 : agencia.hashCode());
		result = prime * result + ((anoInicio == null) ? 0 : anoInicio.hashCode());
		result = prime * result + ((banco == null) ? 0 : banco.hashCode());
		result = prime * result + ((cliente == null) ? 0 : cliente.hashCode());
		result = prime * result + ((cobraExcedente == null) ? 0 : cobraExcedente.hashCode());
		result = prime * result + ((cobradoPorQuilo == null) ? 0 : cobradoPorQuilo.hashCode());
		result = prime * result + ((codigoContratoAtual == null) ? 0 : codigoContratoAtual.hashCode());
		result = prime * result + ((comunicarEmail == null) ? 0 : comunicarEmail.hashCode());
		result = prime * result + ((conta == null) ? 0 : conta.hashCode());
		result = prime * result + ((contrato == null) ? 0 : contrato.hashCode());
		result = prime * result + (contratoGeradoPeloCliente ? 1231 : 1237);
		result = prime * result + ((dataEmissaoContrato == null) ? 0 : dataEmissaoContrato.hashCode());
		result = prime * result + ((dataFinal == null) ? 0 : dataFinal.hashCode());
		result = prime * result + ((dataInicial == null) ? 0 : dataInicial.hashCode());
		result = prime * result + ((dataPrimeiroPagamento == null) ? 0 : dataPrimeiroPagamento.hashCode());
		result = prime * result + ((dataUltimoFechamento == null) ? 0 : dataUltimoFechamento.hashCode());
		result = prime * result + ((dataUltimoPagamento == null) ? 0 : dataUltimoPagamento.hashCode());
		result = prime * result + ((diaFechamento == null) ? 0 : diaFechamento.hashCode());
		result = prime * result + ((diaVencimentoFatura == null) ? 0 : diaVencimentoFatura.hashCode());
		result = prime * result + ((existeValorFixoMensal == null) ? 0 : existeValorFixoMensal.hashCode());
		result = prime * result + ((fechamentoDataDiferente == null) ? 0 : fechamentoDataDiferente.hashCode());
		result = prime * result + ((formaCobranca == null) ? 0 : formaCobranca.hashCode());
		result = prime * result
				+ ((geradoPelasInformacoesFinanceirasID == null) ? 0 : geradoPelasInformacoesFinanceirasID.hashCode());
		result = prime * result + (gerarContratoParaEstaInformacao ? 1231 : 1237);
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((idInformacaoFinanceiraUnico == null) ? 0 : idInformacaoFinanceiraUnico.hashCode());
		result = prime * result + ((listaItensInformacoesFinanceirasPontoColeta == null) ? 0
				: listaItensInformacoesFinanceirasPontoColeta.hashCode());
		result = prime * result + ((mesInicio == null) ? 0 : mesInicio.hashCode());
		result = prime * result + ((observacaoSituacaoContrato == null) ? 0 : observacaoSituacaoContrato.hashCode());
		result = prime * result + ((periodicidadeColeta == null) ? 0 : periodicidadeColeta.hashCode());
		result = prime * result + ((pesoMensal == null) ? 0 : pesoMensal.hashCode());
		result = prime * result + ((pontoColetaGerarContrato == null) ? 0 : pontoColetaGerarContrato.hashCode());
		result = prime * result + ((quantidadeMesesContrato == null) ? 0 : quantidadeMesesContrato.hashCode());
		result = prime * result + ((recebimentoBoletoDeposito == null) ? 0 : recebimentoBoletoDeposito.hashCode());
		result = prime * result + ((responsavel == null) ? 0 : responsavel.hashCode());
		result = prime * result + ((romaneioContratoComDadosDo == null) ? 0 : romaneioContratoComDadosDo.hashCode());
		result = prime * result + ((situacaoContrato == null) ? 0 : situacaoContrato.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((textoDocumentoContrato == null) ? 0 : textoDocumentoContrato.hashCode());
		result = prime * result + ((valorExcedente == null) ? 0 : valorExcedente.hashCode());
		result = prime * result + ((valorFixoMensalPonto == null) ? 0 : valorFixoMensalPonto.hashCode());
		result = prime * result + ((valorFixoMensalTotal == null) ? 0 : valorFixoMensalTotal.hashCode());
		result = prime * result + ((valorPorQuilo == null) ? 0 : valorPorQuilo.hashCode());
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
		InformacaoFinanceira other = (InformacaoFinanceira) obj;
		if (agencia == null) {
			if (other.agencia != null)
				return false;
		} else if (!agencia.equals(other.agencia))
			return false;
		if (anoInicio == null) {
			if (other.anoInicio != null)
				return false;
		} else if (!anoInicio.equals(other.anoInicio))
			return false;
		if (banco == null) {
			if (other.banco != null)
				return false;
		} else if (!banco.equals(other.banco))
			return false;
		if (cliente == null) {
			if (other.cliente != null)
				return false;
		} else if (!cliente.equals(other.cliente))
			return false;
		if (cobraExcedente == null) {
			if (other.cobraExcedente != null)
				return false;
		} else if (!cobraExcedente.equals(other.cobraExcedente))
			return false;
		if (cobradoPorQuilo == null) {
			if (other.cobradoPorQuilo != null)
				return false;
		} else if (!cobradoPorQuilo.equals(other.cobradoPorQuilo))
			return false;
		if (codigoContratoAtual == null) {
			if (other.codigoContratoAtual != null)
				return false;
		} else if (!codigoContratoAtual.equals(other.codigoContratoAtual))
			return false;
		if (comunicarEmail == null) {
			if (other.comunicarEmail != null)
				return false;
		} else if (!comunicarEmail.equals(other.comunicarEmail))
			return false;
		if (conta == null) {
			if (other.conta != null)
				return false;
		} else if (!conta.equals(other.conta))
			return false;
		if (contrato == null) {
			if (other.contrato != null)
				return false;
		} else if (!contrato.equals(other.contrato))
			return false;
		if (contratoGeradoPeloCliente != other.contratoGeradoPeloCliente)
			return false;
		if (dataEmissaoContrato == null) {
			if (other.dataEmissaoContrato != null)
				return false;
		} else if (!dataEmissaoContrato.equals(other.dataEmissaoContrato))
			return false;
		if (dataFinal == null) {
			if (other.dataFinal != null)
				return false;
		} else if (!dataFinal.equals(other.dataFinal))
			return false;
		if (dataInicial == null) {
			if (other.dataInicial != null)
				return false;
		} else if (!dataInicial.equals(other.dataInicial))
			return false;
		if (dataPrimeiroPagamento == null) {
			if (other.dataPrimeiroPagamento != null)
				return false;
		} else if (!dataPrimeiroPagamento.equals(other.dataPrimeiroPagamento))
			return false;
		if (dataUltimoFechamento == null) {
			if (other.dataUltimoFechamento != null)
				return false;
		} else if (!dataUltimoFechamento.equals(other.dataUltimoFechamento))
			return false;
		if (dataUltimoPagamento == null) {
			if (other.dataUltimoPagamento != null)
				return false;
		} else if (!dataUltimoPagamento.equals(other.dataUltimoPagamento))
			return false;
		if (diaFechamento == null) {
			if (other.diaFechamento != null)
				return false;
		} else if (!diaFechamento.equals(other.diaFechamento))
			return false;
		if (diaVencimentoFatura == null) {
			if (other.diaVencimentoFatura != null)
				return false;
		} else if (!diaVencimentoFatura.equals(other.diaVencimentoFatura))
			return false;
		if (existeValorFixoMensal == null) {
			if (other.existeValorFixoMensal != null)
				return false;
		} else if (!existeValorFixoMensal.equals(other.existeValorFixoMensal))
			return false;
		if (fechamentoDataDiferente == null) {
			if (other.fechamentoDataDiferente != null)
				return false;
		} else if (!fechamentoDataDiferente.equals(other.fechamentoDataDiferente))
			return false;
		if (formaCobranca == null) {
			if (other.formaCobranca != null)
				return false;
		} else if (!formaCobranca.equals(other.formaCobranca))
			return false;
		if (geradoPelasInformacoesFinanceirasID == null) {
			if (other.geradoPelasInformacoesFinanceirasID != null)
				return false;
		} else if (!geradoPelasInformacoesFinanceirasID.equals(other.geradoPelasInformacoesFinanceirasID))
			return false;
		if (gerarContratoParaEstaInformacao != other.gerarContratoParaEstaInformacao)
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (idInformacaoFinanceiraUnico == null) {
			if (other.idInformacaoFinanceiraUnico != null)
				return false;
		} else if (!idInformacaoFinanceiraUnico.equals(other.idInformacaoFinanceiraUnico))
			return false;
		if (listaItensInformacoesFinanceirasPontoColeta == null) {
			if (other.listaItensInformacoesFinanceirasPontoColeta != null)
				return false;
		} else if (!listaItensInformacoesFinanceirasPontoColeta
				.equals(other.listaItensInformacoesFinanceirasPontoColeta))
			return false;
		if (mesInicio == null) {
			if (other.mesInicio != null)
				return false;
		} else if (!mesInicio.equals(other.mesInicio))
			return false;
		if (observacaoSituacaoContrato == null) {
			if (other.observacaoSituacaoContrato != null)
				return false;
		} else if (!observacaoSituacaoContrato.equals(other.observacaoSituacaoContrato))
			return false;
		if (periodicidadeColeta == null) {
			if (other.periodicidadeColeta != null)
				return false;
		} else if (!periodicidadeColeta.equals(other.periodicidadeColeta))
			return false;
		if (pesoMensal == null) {
			if (other.pesoMensal != null)
				return false;
		} else if (!pesoMensal.equals(other.pesoMensal))
			return false;
		if (pontoColetaGerarContrato == null) {
			if (other.pontoColetaGerarContrato != null)
				return false;
		} else if (!pontoColetaGerarContrato.equals(other.pontoColetaGerarContrato))
			return false;
		if (quantidadeMesesContrato == null) {
			if (other.quantidadeMesesContrato != null)
				return false;
		} else if (!quantidadeMesesContrato.equals(other.quantidadeMesesContrato))
			return false;
		if (recebimentoBoletoDeposito == null) {
			if (other.recebimentoBoletoDeposito != null)
				return false;
		} else if (!recebimentoBoletoDeposito.equals(other.recebimentoBoletoDeposito))
			return false;
		if (responsavel == null) {
			if (other.responsavel != null)
				return false;
		} else if (!responsavel.equals(other.responsavel))
			return false;
		if (romaneioContratoComDadosDo == null) {
			if (other.romaneioContratoComDadosDo != null)
				return false;
		} else if (!romaneioContratoComDadosDo.equals(other.romaneioContratoComDadosDo))
			return false;
		if (situacaoContrato == null) {
			if (other.situacaoContrato != null)
				return false;
		} else if (!situacaoContrato.equals(other.situacaoContrato))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (textoDocumentoContrato == null) {
			if (other.textoDocumentoContrato != null)
				return false;
		} else if (!textoDocumentoContrato.equals(other.textoDocumentoContrato))
			return false;
		if (valorExcedente == null) {
			if (other.valorExcedente != null)
				return false;
		} else if (!valorExcedente.equals(other.valorExcedente))
			return false;
		if (valorFixoMensalPonto == null) {
			if (other.valorFixoMensalPonto != null)
				return false;
		} else if (!valorFixoMensalPonto.equals(other.valorFixoMensalPonto))
			return false;
		if (valorFixoMensalTotal == null) {
			if (other.valorFixoMensalTotal != null)
				return false;
		} else if (!valorFixoMensalTotal.equals(other.valorFixoMensalTotal))
			return false;
		if (valorPorQuilo == null) {
			if (other.valorPorQuilo != null)
				return false;
		} else if (!valorPorQuilo.equals(other.valorPorQuilo))
			return false;
		return true;
	}

}
