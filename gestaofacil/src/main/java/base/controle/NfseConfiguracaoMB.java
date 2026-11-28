package base.controle;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import base.modelo.NfseAliquotaMensal;
import base.modelo.NfseConfiguracao;
import base.modelo.NfseModeloDescricao;
import base.service.NfseEmissaoService;
import base.service.NfseService;
import util.ExibirMensagem;
import util.nfse.NfseNacionalClient;
import util.nfse.NfseNacionalClient.NfseException;

@ViewScoped
@Named("nfseConfiguracaoMB")
public class NfseConfiguracaoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private NfseService nfseService;

	@Inject
	private NfseEmissaoService nfseEmissaoService;

	private NfseConfiguracao configuracao;
	/** Senha digitada na tela; vazia = mantém a senha já gravada. */
	private String novaSenha;
	private String infoCertificado;

	// Alíquotas do mês
	private Integer aliqMes;
	private Integer aliqAno;
	private Double aliqSimples;
	private java.util.List<NfseAliquotaMensal> listaAliquotas;

	// Modelos de descrição
	private NfseModeloDescricao modelo = new NfseModeloDescricao();
	private java.util.List<NfseModeloDescricao> listaModelos;
	/** Há senha gravada no banco (não conta a digitada e ainda não salva). */
	private boolean senhaSalva;

	@PostConstruct
	public void init() {
		configuracao = nfseService.carregarConfiguracao();
		senhaSalva = temTexto(configuracao.getSenhaCertificado());
		atualizarInfoCertificado();
		limparAliquota();
		listaModelos = nfseService.listarModelosDescricao();
	}

	public void novoModelo() {
		modelo = new NfseModeloDescricao();
		modelo.setOrdem(listaModelos == null ? 1 : listaModelos.size() + 1);
	}

	public void editarModelo(NfseModeloDescricao m) {
		modelo = m;
	}

	public void salvarModelo() {
		if (modelo.getNome() == null || modelo.getNome().trim().isEmpty() || modelo.getTexto() == null
				|| modelo.getTexto().trim().isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Informe o nome do botão e o texto.");
			return;
		}
		nfseService.salvarModeloDescricao(modelo);
		listaModelos = nfseService.listarModelosDescricao();
		novoModelo();
		ExibirMensagem.exibirMensagem("Modelo salvo.");
		org.primefaces.PrimeFaces.current().executeScript("PF('dlgModelo').hide();");
	}

	public void excluirModelo(NfseModeloDescricao m) {
		m.setStatus(false);
		nfseService.salvarModeloDescricao(m);
		listaModelos = nfseService.listarModelosDescricao();
		novoModelo();
	}

	public NfseModeloDescricao getModelo() {
		return modelo;
	}

	public java.util.List<NfseModeloDescricao> getListaModelos() {
		return listaModelos;
	}

	private void limparAliquota() {
		java.util.Calendar cal = java.util.Calendar.getInstance();
		aliqMes = cal.get(java.util.Calendar.MONTH) + 1;
		aliqAno = cal.get(java.util.Calendar.YEAR);
		aliqSimples = null;
		listaAliquotas = nfseService.listarAliquotas();
	}

	public void salvarAliquota() {
		if (aliqMes == null || aliqAno == null || aliqAno < 2020 || aliqAno > 2100) {
			ExibirMensagem.exibirMensagemErro("Informe o mês e o ano.");
			return;
		}
		if (aliqSimples == null || aliqSimples <= 0 || aliqSimples >= 100) {
			ExibirMensagem.exibirMensagemErro("Informe o % de tributos do Simples do mês.");
			return;
		}
		try {
			nfseService.salvarAliquota(aliqMes, aliqAno, aliqSimples);
			ExibirMensagem.exibirMensagem("Alíquotas de " + String.format("%02d/%d", aliqMes, aliqAno) + " salvas.");
			limparAliquota();
		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagemErro("Erro ao salvar as alíquotas: " + e.getMessage());
		}
	}

	public void editarAliquota(NfseAliquotaMensal a) {
		aliqMes = a.getMes();
		aliqAno = a.getAno();
		aliqSimples = a.getPercentualSimples();
	}

	public Integer getAliqMes() {
		return aliqMes;
	}

	public void setAliqMes(Integer aliqMes) {
		this.aliqMes = aliqMes;
	}

	public Integer getAliqAno() {
		return aliqAno;
	}

	public void setAliqAno(Integer aliqAno) {
		this.aliqAno = aliqAno;
	}

	public Double getAliqSimples() {
		return aliqSimples;
	}

	public void setAliqSimples(Double aliqSimples) {
		this.aliqSimples = aliqSimples;
	}

	public java.util.List<NfseAliquotaMensal> getListaAliquotas() {
		return listaAliquotas;
	}

	public void salvar() {
		try {
			aplicarSenhaDigitada();
			normalizar();
			nfseService.salvarConfiguracao(configuracao);
			configuracao = nfseService.carregarConfiguracao();
			senhaSalva = temTexto(configuracao.getSenhaCertificado());
			novaSenha = null;
			atualizarInfoCertificado();
			ExibirMensagem.exibirMensagem("Configuração da NFS-e salva.");
		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagemErro("Erro ao salvar: " + e.getMessage());
		}
	}

	public void uploadCertificado(FileUploadEvent evento) {
		UploadedFile arquivo = evento.getFile();
		if (arquivo == null || arquivo.getContents() == null || arquivo.getContents().length == 0) {
			ExibirMensagem.exibirMensagemErro("Arquivo do certificado vazio.");
			return;
		}
		configuracao.setCertificado(arquivo.getContents());
		configuracao.setNomeArquivoCertificado(arquivo.getFileName());
		infoCertificado = "Arquivo " + arquivo.getFileName()
				+ " carregado. Informe a senha e clique em Salvar.";
		ExibirMensagem.exibirMensagem("Certificado carregado. Informe a senha e salve a configuração.");
	}

	public void testarCertificado() {
		try {
			if (!configuracao.isCertificadoCarregado()) {
				throw new NfseException("Envie primeiro o arquivo do certificado (.pfx ou .p12).");
			}
			if (!temTexto(novaSenha) && !temTexto(configuracao.getSenhaCertificado())) {
				throw new NfseException("Digite a senha do certificado para testar (ainda não há senha salva).");
			}
			aplicarSenhaDigitada();
			infoCertificado = nfseEmissaoService.testarCertificado(configuracao);
			ExibirMensagem.exibirMensagem("Certificado OK: " + infoCertificado);
		} catch (NfseException e) {
			infoCertificado = e.getMessage();
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		}
	}

	private void aplicarSenhaDigitada() {
		if (novaSenha != null && !novaSenha.isEmpty()) {
			configuracao.setSenhaCertificado(novaSenha);
		}
	}

	private void atualizarInfoCertificado() {
		if (!configuracao.isCertificadoCarregado()) {
			infoCertificado = "Nenhum certificado carregado.";
			return;
		}
		try {
			infoCertificado = nfseEmissaoService.descreverCertificado(configuracao);
		} catch (NfseException e) {
			infoCertificado = "Certificado " + configuracao.getNomeArquivoCertificado() + ": " + e.getMessage();
		}
	}

	private void normalizar() {
		configuracao.setCnpjPrestador(NfseNacionalClient.somenteDigitos(configuracao.getCnpjPrestador()));
		configuracao.setCodigoMunicipioEmissor(
				NfseNacionalClient.somenteDigitos(configuracao.getCodigoMunicipioEmissor()));
		configuracao.setCodigoTributacaoNacional(
				NfseNacionalClient.somenteDigitos(configuracao.getCodigoTributacaoNacional()));
		configuracao.setCodigoTributacaoMunicipal(
				vazioParaNulo(NfseNacionalClient.somenteDigitos(configuracao.getCodigoTributacaoMunicipal())));
		configuracao.setCodigoNbs(vazioParaNulo(NfseNacionalClient.somenteDigitos(configuracao.getCodigoNbs())));
		configuracao.setSerieDps(NfseNacionalClient.somenteDigitos(configuracao.getSerieDps()));
	}

	private static String vazioParaNulo(String s) {
		return s == null || s.isEmpty() ? null : s;
	}

	public NfseConfiguracao getConfiguracao() {
		return configuracao;
	}

	public void setConfiguracao(NfseConfiguracao configuracao) {
		this.configuracao = configuracao;
	}

	public String getNovaSenha() {
		return novaSenha;
	}

	public void setNovaSenha(String novaSenha) {
		this.novaSenha = novaSenha;
	}

	public boolean isSenhaSalva() {
		return senhaSalva;
	}

	private static boolean temTexto(String s) {
		return s != null && !s.isEmpty();
	}

	public String getInfoCertificado() {
		return infoCertificado;
	}
}
