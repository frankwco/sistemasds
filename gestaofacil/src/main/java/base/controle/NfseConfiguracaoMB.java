package base.controle;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import base.modelo.NfseConfiguracao;
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
	/** Há senha gravada no banco (não conta a digitada e ainda não salva). */
	private boolean senhaSalva;

	@PostConstruct
	public void init() {
		configuracao = nfseService.carregarConfiguracao();
		senhaSalva = temTexto(configuracao.getSenhaCertificado());
		atualizarInfoCertificado();
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
			infoCertificado = nfseEmissaoService.testarCertificado(configuracao);
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
