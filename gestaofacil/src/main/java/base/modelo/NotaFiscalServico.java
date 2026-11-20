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

/**
 * NFS-e emitida (ou tentativa de emissão) pelo Sistema Nacional NFS-e.
 */
@Entity
@Table(name = "nota_fiscal_servico")
public class NotaFiscalServico implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String SITUACAO_EMITIDA = "EMITIDA";
	public static final String SITUACAO_REJEITADA = "REJEITADA";
	/** Falha de comunicação: a nota pode ou não ter sido gerada - consultar. */
	public static final String SITUACAO_PENDENTE = "PENDENTE";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	private Cliente cliente;

	@ManyToOne
	private FechamentoMes fechamentoMes;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataEmissao = new Date();

	@Temporal(TemporalType.DATE)
	private Date competencia;

	private Double valor = 0.;

	@Column(length = 2000)
	private String descricaoServico;

	@Column(length = 6)
	private String codigoTributacaoNacional;
	@Column(length = 3)
	private String codigoTributacaoMunicipal;
	@Column(length = 7)
	private String codigoMunicipioPrestacao;
	private Integer retencaoIssqn = 1;
	private Double aliquotaIss = 0.;

	private Integer ambiente;
	@Column(length = 5)
	private String serieDps;
	private Long numeroDps;
	@Column(length = 45)
	private String idDps;

	@Column(length = 60)
	private String chaveAcesso;
	@Column(length = 20)
	private String numeroNfse;

	@Column(length = 20)
	private String situacao;
	@Column(length = 4000)
	private String mensagemRetorno;

	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String xmlDps;
	@Lob
	@Column(columnDefinition = "LONGTEXT")
	private String xmlNfse;

	private String usuarioEmissao;

	private Boolean status = true;

	public boolean isEmitida() {
		return SITUACAO_EMITIDA.equals(situacao);
	}

	public boolean isPendente() {
		return SITUACAO_PENDENTE.equals(situacao);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public FechamentoMes getFechamentoMes() {
		return fechamentoMes;
	}

	public void setFechamentoMes(FechamentoMes fechamentoMes) {
		this.fechamentoMes = fechamentoMes;
	}

	public Date getDataEmissao() {
		return dataEmissao;
	}

	public void setDataEmissao(Date dataEmissao) {
		this.dataEmissao = dataEmissao;
	}

	public Date getCompetencia() {
		return competencia;
	}

	public void setCompetencia(Date competencia) {
		this.competencia = competencia;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public String getDescricaoServico() {
		return descricaoServico;
	}

	public void setDescricaoServico(String descricaoServico) {
		this.descricaoServico = descricaoServico;
	}

	public String getCodigoTributacaoNacional() {
		return codigoTributacaoNacional;
	}

	public void setCodigoTributacaoNacional(String codigoTributacaoNacional) {
		this.codigoTributacaoNacional = codigoTributacaoNacional;
	}

	public String getCodigoTributacaoMunicipal() {
		return codigoTributacaoMunicipal;
	}

	public void setCodigoTributacaoMunicipal(String codigoTributacaoMunicipal) {
		this.codigoTributacaoMunicipal = codigoTributacaoMunicipal;
	}

	public String getCodigoMunicipioPrestacao() {
		return codigoMunicipioPrestacao;
	}

	public void setCodigoMunicipioPrestacao(String codigoMunicipioPrestacao) {
		this.codigoMunicipioPrestacao = codigoMunicipioPrestacao;
	}

	public Integer getRetencaoIssqn() {
		return retencaoIssqn;
	}

	public void setRetencaoIssqn(Integer retencaoIssqn) {
		this.retencaoIssqn = retencaoIssqn;
	}

	public Double getAliquotaIss() {
		return aliquotaIss;
	}

	public void setAliquotaIss(Double aliquotaIss) {
		this.aliquotaIss = aliquotaIss;
	}

	public Integer getAmbiente() {
		return ambiente;
	}

	public void setAmbiente(Integer ambiente) {
		this.ambiente = ambiente;
	}

	public String getSerieDps() {
		return serieDps;
	}

	public void setSerieDps(String serieDps) {
		this.serieDps = serieDps;
	}

	public Long getNumeroDps() {
		return numeroDps;
	}

	public void setNumeroDps(Long numeroDps) {
		this.numeroDps = numeroDps;
	}

	public String getIdDps() {
		return idDps;
	}

	public void setIdDps(String idDps) {
		this.idDps = idDps;
	}

	public String getChaveAcesso() {
		return chaveAcesso;
	}

	public void setChaveAcesso(String chaveAcesso) {
		this.chaveAcesso = chaveAcesso;
	}

	public String getNumeroNfse() {
		return numeroNfse;
	}

	public void setNumeroNfse(String numeroNfse) {
		this.numeroNfse = numeroNfse;
	}

	public String getSituacao() {
		return situacao;
	}

	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}

	public String getMensagemRetorno() {
		return mensagemRetorno;
	}

	public void setMensagemRetorno(String mensagemRetorno) {
		if (mensagemRetorno != null && mensagemRetorno.length() > 4000) {
			mensagemRetorno = mensagemRetorno.substring(0, 4000);
		}
		this.mensagemRetorno = mensagemRetorno;
	}

	public String getXmlDps() {
		return xmlDps;
	}

	public void setXmlDps(String xmlDps) {
		this.xmlDps = xmlDps;
	}

	public String getXmlNfse() {
		return xmlNfse;
	}

	public void setXmlNfse(String xmlNfse) {
		this.xmlNfse = xmlNfse;
	}

	public String getUsuarioEmissao() {
		return usuarioEmissao;
	}

	public void setUsuarioEmissao(String usuarioEmissao) {
		this.usuarioEmissao = usuarioEmissao;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
