package base.modelo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Configuração (registro único) da emissão de NFS-e pelo Sistema Nacional
 * (Sefin Nacional / Emissor Nacional - nfse.gov.br).
 *
 * Guarda os dados do prestador, o certificado digital A1 e os valores padrão
 * que se repetem em toda nota (código de tributação, descrição, alíquota...).
 */
@Entity
@Table(name = "nfse_configuracao")
public class NfseConfiguracao implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int AMBIENTE_PRODUCAO = 1;
	public static final int AMBIENTE_HOMOLOGACAO = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	/** 1 = Produção, 2 = Produção restrita (homologação/testes). */
	private Integer ambiente = AMBIENTE_HOMOLOGACAO;

	// ---------------- Prestador ----------------
	@Column(length = 14)
	private String cnpjPrestador;
	@Column(length = 15)
	private String inscricaoMunicipal;
	private String razaoSocialPrestador;
	/** Código IBGE do município do prestador (local de emissão). */
	@Column(length = 7)
	private String codigoMunicipioEmissor;
	@Column(length = 20)
	private String telefonePrestador;
	private String emailPrestador;

	/** 1 - Não optante; 2 - MEI; 3 - ME/EPP optante do Simples Nacional. */
	private Integer opcaoSimplesNacional = 3;
	/**
	 * Só para opSimpNac = 3: 1 - tributos federais e municipal pelo SN; 2 - federais
	 * pelo SN e ISSQN fora do SN; 3 - federais e municipal fora do SN.
	 */
	private Integer regimeApuracaoSimplesNacional = 1;
	/** 0 - Nenhum ... 9 - Outros. */
	private Integer regimeEspecialTributacao = 0;

	// ---------------- Numeração da DPS ----------------
	@Column(length = 5)
	private String serieDps = "900";
	private Long ultimoNumeroDps = 0L;

	// ---------------- Certificado digital A1 ----------------
	@Lob
	@Column(columnDefinition = "LONGBLOB")
	private byte[] certificado;
	private String nomeArquivoCertificado;
	private String senhaCertificado;
	/** "SHA256" (padrão) ou "SHA1" - algoritmo da assinatura XML. */
	@Column(length = 10)
	private String algoritmoAssinatura = "SHA256";

	// ---------------- Padrões do serviço ----------------
	/** Código de tributação nacional (6 dígitos: item, subitem, desdobro). */
	@Column(length = 6)
	private String codigoTributacaoNacional;
	/** Código de tributação municipal (3 dígitos), opcional. */
	@Column(length = 3)
	private String codigoTributacaoMunicipal;
	/** Código NBS (9 dígitos), opcional. */
	@Column(length = 9)
	private String codigoNbs;
	/** Descrição padrão. Aceita {MES}, {ANO}, {MES_NOME}, {CLIENTE} e {BOLETO}. */
	@Column(length = 2000)
	private String descricaoServicoPadrao = "COLETA, TRANSPORTE, TRATAMENTO e DESTIN. FINAL DE RESIDUOS DA SAUDE Mês {MES_NOME}/{ANO} Conforme contrato e demonstrativo anexo. BOLETO: {BOLETO}";
	/** "EMITENTE" ou "TOMADOR": de onde vem o município de prestação padrão. */
	@Column(length = 10)
	private String localPrestacaoPadrao = "TOMADOR";
	/** 1 - Operação tributável; 2 - Imunidade; 3 - Exportação; 4 - Não incidência. */
	private Integer tributacaoIssqn = 1;
	/** 1 - Não retido; 2 - Retido pelo tomador; 3 - Retido pelo intermediário. */
	private Integer retencaoIssqnPadrao = 1;
	/** Alíquota do ISSQN (%). Só é enviada quando maior que zero. */
	private Double aliquotaIss = 0.;
	/** Percentual aproximado de tributos do Simples Nacional (pTotTribSN). */
	private Double percentualTributosSimples = 0.;
	@Column(length = 2000)
	private String informacoesComplementares;

	private Boolean status = true;

	public boolean isProducao() {
		return ambiente != null && ambiente == AMBIENTE_PRODUCAO;
	}

	public boolean isCertificadoCarregado() {
		return certificado != null && certificado.length > 0;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getAmbiente() {
		return ambiente;
	}

	public void setAmbiente(Integer ambiente) {
		this.ambiente = ambiente;
	}

	public String getCnpjPrestador() {
		return cnpjPrestador;
	}

	public void setCnpjPrestador(String cnpjPrestador) {
		this.cnpjPrestador = cnpjPrestador;
	}

	public String getInscricaoMunicipal() {
		return inscricaoMunicipal;
	}

	public void setInscricaoMunicipal(String inscricaoMunicipal) {
		this.inscricaoMunicipal = inscricaoMunicipal;
	}

	public String getRazaoSocialPrestador() {
		return razaoSocialPrestador;
	}

	public void setRazaoSocialPrestador(String razaoSocialPrestador) {
		this.razaoSocialPrestador = razaoSocialPrestador;
	}

	public String getCodigoMunicipioEmissor() {
		return codigoMunicipioEmissor;
	}

	public void setCodigoMunicipioEmissor(String codigoMunicipioEmissor) {
		this.codigoMunicipioEmissor = codigoMunicipioEmissor;
	}

	public String getTelefonePrestador() {
		return telefonePrestador;
	}

	public void setTelefonePrestador(String telefonePrestador) {
		this.telefonePrestador = telefonePrestador;
	}

	public String getEmailPrestador() {
		return emailPrestador;
	}

	public void setEmailPrestador(String emailPrestador) {
		this.emailPrestador = emailPrestador;
	}

	public Integer getOpcaoSimplesNacional() {
		return opcaoSimplesNacional;
	}

	public void setOpcaoSimplesNacional(Integer opcaoSimplesNacional) {
		this.opcaoSimplesNacional = opcaoSimplesNacional;
	}

	public Integer getRegimeApuracaoSimplesNacional() {
		return regimeApuracaoSimplesNacional;
	}

	public void setRegimeApuracaoSimplesNacional(Integer regimeApuracaoSimplesNacional) {
		this.regimeApuracaoSimplesNacional = regimeApuracaoSimplesNacional;
	}

	public Integer getRegimeEspecialTributacao() {
		return regimeEspecialTributacao;
	}

	public void setRegimeEspecialTributacao(Integer regimeEspecialTributacao) {
		this.regimeEspecialTributacao = regimeEspecialTributacao;
	}

	public String getSerieDps() {
		return serieDps;
	}

	public void setSerieDps(String serieDps) {
		this.serieDps = serieDps;
	}

	public Long getUltimoNumeroDps() {
		return ultimoNumeroDps;
	}

	public void setUltimoNumeroDps(Long ultimoNumeroDps) {
		this.ultimoNumeroDps = ultimoNumeroDps;
	}

	public byte[] getCertificado() {
		return certificado;
	}

	public void setCertificado(byte[] certificado) {
		this.certificado = certificado;
	}

	public String getNomeArquivoCertificado() {
		return nomeArquivoCertificado;
	}

	public void setNomeArquivoCertificado(String nomeArquivoCertificado) {
		this.nomeArquivoCertificado = nomeArquivoCertificado;
	}

	public String getSenhaCertificado() {
		return senhaCertificado;
	}

	public void setSenhaCertificado(String senhaCertificado) {
		this.senhaCertificado = senhaCertificado;
	}

	public String getAlgoritmoAssinatura() {
		return algoritmoAssinatura;
	}

	public void setAlgoritmoAssinatura(String algoritmoAssinatura) {
		this.algoritmoAssinatura = algoritmoAssinatura;
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

	public String getCodigoNbs() {
		return codigoNbs;
	}

	public void setCodigoNbs(String codigoNbs) {
		this.codigoNbs = codigoNbs;
	}

	public String getDescricaoServicoPadrao() {
		return descricaoServicoPadrao;
	}

	public void setDescricaoServicoPadrao(String descricaoServicoPadrao) {
		this.descricaoServicoPadrao = descricaoServicoPadrao;
	}

	public String getLocalPrestacaoPadrao() {
		return localPrestacaoPadrao;
	}

	public void setLocalPrestacaoPadrao(String localPrestacaoPadrao) {
		this.localPrestacaoPadrao = localPrestacaoPadrao;
	}

	public Integer getTributacaoIssqn() {
		return tributacaoIssqn;
	}

	public void setTributacaoIssqn(Integer tributacaoIssqn) {
		this.tributacaoIssqn = tributacaoIssqn;
	}

	public Integer getRetencaoIssqnPadrao() {
		return retencaoIssqnPadrao;
	}

	public void setRetencaoIssqnPadrao(Integer retencaoIssqnPadrao) {
		this.retencaoIssqnPadrao = retencaoIssqnPadrao;
	}

	public Double getAliquotaIss() {
		return aliquotaIss;
	}

	public void setAliquotaIss(Double aliquotaIss) {
		this.aliquotaIss = aliquotaIss;
	}

	public Double getPercentualTributosSimples() {
		return percentualTributosSimples;
	}

	public void setPercentualTributosSimples(Double percentualTributosSimples) {
		this.percentualTributosSimples = percentualTributosSimples;
	}

	public String getInformacoesComplementares() {
		return informacoesComplementares;
	}

	public void setInformacoesComplementares(String informacoesComplementares) {
		this.informacoesComplementares = informacoesComplementares;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
}
