package base.modelo;

import java.io.Serializable;
import java.util.List;

/**
 * Um cliente com títulos em atraso, pronto pra tela de Cobrança do appgestaofacil — não é
 * entidade, é a resposta agregada de {@code ServicoApp}, montada a partir de um
 * {@link util.cobranca.GrupoConfirmadoCobranca} pra não expor o grafo inteiro de Cliente/Pessoa.
 */
public class CobrancaClienteDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long clienteId;
	private String nome;
	private String cidade;
	private String telefone1;
	private String telefone2;
	private Boolean telefone1Whatsapp;
	private Boolean telefone2Whatsapp;
	private String valorTotalFormatado;
	private List<String> parcelasFormatadas;
	private String nomesSacadoRelatorio;
	private boolean temNomeDivergente;
	private String mensagemWhatsapp;
	private List<HistoricoCobrancaLinha> historicoAtual;
	private List<HistoricoCobrancaLinha> historicoAnterior;

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getCidade() {
		return cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getTelefone1() {
		return telefone1;
	}

	public void setTelefone1(String telefone1) {
		this.telefone1 = telefone1;
	}

	public String getTelefone2() {
		return telefone2;
	}

	public void setTelefone2(String telefone2) {
		this.telefone2 = telefone2;
	}

	public Boolean getTelefone1Whatsapp() {
		return telefone1Whatsapp;
	}

	public void setTelefone1Whatsapp(Boolean telefone1Whatsapp) {
		this.telefone1Whatsapp = telefone1Whatsapp;
	}

	public Boolean getTelefone2Whatsapp() {
		return telefone2Whatsapp;
	}

	public void setTelefone2Whatsapp(Boolean telefone2Whatsapp) {
		this.telefone2Whatsapp = telefone2Whatsapp;
	}

	public String getValorTotalFormatado() {
		return valorTotalFormatado;
	}

	public void setValorTotalFormatado(String valorTotalFormatado) {
		this.valorTotalFormatado = valorTotalFormatado;
	}

	public List<String> getParcelasFormatadas() {
		return parcelasFormatadas;
	}

	public void setParcelasFormatadas(List<String> parcelasFormatadas) {
		this.parcelasFormatadas = parcelasFormatadas;
	}

	public String getNomesSacadoRelatorio() {
		return nomesSacadoRelatorio;
	}

	public void setNomesSacadoRelatorio(String nomesSacadoRelatorio) {
		this.nomesSacadoRelatorio = nomesSacadoRelatorio;
	}

	public boolean isTemNomeDivergente() {
		return temNomeDivergente;
	}

	public void setTemNomeDivergente(boolean temNomeDivergente) {
		this.temNomeDivergente = temNomeDivergente;
	}

	public String getMensagemWhatsapp() {
		return mensagemWhatsapp;
	}

	public void setMensagemWhatsapp(String mensagemWhatsapp) {
		this.mensagemWhatsapp = mensagemWhatsapp;
	}

	public List<HistoricoCobrancaLinha> getHistoricoAtual() {
		return historicoAtual;
	}

	public void setHistoricoAtual(List<HistoricoCobrancaLinha> historicoAtual) {
		this.historicoAtual = historicoAtual;
	}

	public List<HistoricoCobrancaLinha> getHistoricoAnterior() {
		return historicoAnterior;
	}

	public void setHistoricoAnterior(List<HistoricoCobrancaLinha> historicoAnterior) {
		this.historicoAnterior = historicoAnterior;
	}

}
