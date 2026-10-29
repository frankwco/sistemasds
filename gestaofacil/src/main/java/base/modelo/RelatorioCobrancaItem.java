package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Um título (linha) do relatório de vencidos do banco, já resolvido (ou não) para um cliente.
 *
 * statusResolucao:
 *  - CONFIRMADO_AUTOMATICO: o código de contrato do "Seu Número" bateu com um único cliente
 *  - CONFIRMADO_MANUAL: havia mais de uma possibilidade e o usuário escolheu na tela de revisão
 *  - PENDENTE_ESCOLHA: ainda não resolvido, aguardando decisão do usuário
 *  - IGNORADO: usuário decidiu não vincular esse título a nenhum cliente
 */
@Entity
@Table(name = "relatorio_cobranca_item")
public class RelatorioCobrancaItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	private RelatorioCobranca relatorioCobranca;

	private String nomeSacadoRelatorio;
	private String seuNumero;
	private String nossoNumero;

	@Temporal(TemporalType.DATE)
	private Date dataVencimento;
	private Double valor;

	private Integer codigoContratoExtraido;

	@ManyToOne
	private Cliente cliente;

	private String statusResolucao = "PENDENTE_ESCOLHA";

	/** IDs de clientes candidatos quando ambíguo, separados por vírgula (ex: "246,513"). */
	private String candidatosClienteIds;

	/** true quando o nome do sacado no relatório do banco não parece bater com o cliente resolvido pelo código. */
	private Boolean nomeDivergente = false;

	private Boolean quitado = false;
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataQuitacao;
	private String formaQuitacao;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataCobrancaEnviada;
	private String canalCobranca;

	private Boolean status = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RelatorioCobranca getRelatorioCobranca() {
		return relatorioCobranca;
	}

	public void setRelatorioCobranca(RelatorioCobranca relatorioCobranca) {
		this.relatorioCobranca = relatorioCobranca;
	}

	public String getNomeSacadoRelatorio() {
		return nomeSacadoRelatorio;
	}

	public void setNomeSacadoRelatorio(String nomeSacadoRelatorio) {
		this.nomeSacadoRelatorio = nomeSacadoRelatorio;
	}

	public String getSeuNumero() {
		return seuNumero;
	}

	public void setSeuNumero(String seuNumero) {
		this.seuNumero = seuNumero;
	}

	public String getNossoNumero() {
		return nossoNumero;
	}

	public void setNossoNumero(String nossoNumero) {
		this.nossoNumero = nossoNumero;
	}

	public Date getDataVencimento() {
		return dataVencimento;
	}

	public void setDataVencimento(Date dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Integer getCodigoContratoExtraido() {
		return codigoContratoExtraido;
	}

	public void setCodigoContratoExtraido(Integer codigoContratoExtraido) {
		this.codigoContratoExtraido = codigoContratoExtraido;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public String getStatusResolucao() {
		return statusResolucao;
	}

	public void setStatusResolucao(String statusResolucao) {
		this.statusResolucao = statusResolucao;
	}

	public String getCandidatosClienteIds() {
		return candidatosClienteIds;
	}

	public void setCandidatosClienteIds(String candidatosClienteIds) {
		this.candidatosClienteIds = candidatosClienteIds;
	}

	public Boolean getNomeDivergente() {
		if (nomeDivergente == null) {
			nomeDivergente = false;
		}
		return nomeDivergente;
	}

	public void setNomeDivergente(Boolean nomeDivergente) {
		this.nomeDivergente = nomeDivergente;
	}

	public Boolean getQuitado() {
		return quitado;
	}

	public void setQuitado(Boolean quitado) {
		this.quitado = quitado;
	}

	public Date getDataQuitacao() {
		return dataQuitacao;
	}

	public void setDataQuitacao(Date dataQuitacao) {
		this.dataQuitacao = dataQuitacao;
	}

	public String getFormaQuitacao() {
		return formaQuitacao;
	}

	public void setFormaQuitacao(String formaQuitacao) {
		this.formaQuitacao = formaQuitacao;
	}

	public Date getDataCobrancaEnviada() {
		return dataCobrancaEnviada;
	}

	public void setDataCobrancaEnviada(Date dataCobrancaEnviada) {
		this.dataCobrancaEnviada = dataCobrancaEnviada;
	}

	public String getCanalCobranca() {
		return canalCobranca;
	}

	public void setCanalCobranca(String canalCobranca) {
		this.canalCobranca = canalCobranca;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

}
