package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import base.modelo.Pessoa;
import base.modelo.Atividade;
import base.modelo.MensagemSite;
import base.service.AtividadeService;
import base.service.MensagemSiteService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import util.validadores.ValidaCNPJ;
import util.validadores.ValidaCPF;
import util.validadores.ValidacaoCNPJ;
import dao.GenericDAO;

@ViewScoped
@Named("atualizacaoEmailMB")
public class AtualizacaoEmailMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private MensagemSite mensagemSite;
	private String mensagemCerto="";

	@Inject
	private MensagemSiteService mensagemSiteService; // inserir no banco

	@PostConstruct
	public void inicializar() {
		mensagemSite = new MensagemSite();
	}
	
	public static boolean isValidEmailAddress(String email) {
	    boolean result = true;
	    if(email.trim().equals("")) {
	    	return true;
	    }
	    try {
	        InternetAddress emailAddr = new InternetAddress(email);
	        emailAddr.validate();
	    } catch (AddressException ex) {
	        result = false;
	    }
	    return result;
	}

	public void salvar() {
		
		
		String cpfCnpj = mensagemSite.getCpfCnpj();
		
		cpfCnpj = cpfCnpj.replace(".", "");
		cpfCnpj = cpfCnpj.replace("-", "");
		cpfCnpj = cpfCnpj.replace("/", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.trim();
		
		if(ValidaCNPJ.isCNPJ(cpfCnpj) || ValidaCPF.isCPF(cpfCnpj)) {
		
		try {
			String erro = "";
			if (mensagemSite.getNomeRazaoSocial().trim().equals("")) {
				erro += "Nome Obrigatorio. ";
			}
			if (mensagemSite.getCpfCnpj().trim().equals("")) {
				erro += "CPF/CNPJ Obrigatorio. ";
			}
			if (mensagemSite.getEmail().trim().equals("")) {
				erro += "E-mail Obrigatorio. ";
			}
			
			if(!isValidEmailAddress(mensagemSite.getEmail())) {
				erro+="E-mail 1 inválido. ";
			}
			if(!isValidEmailAddress(mensagemSite.getEmail2())) {
				erro+="E-mail 2 inválido. ";
			}

			if (!erro.equals("")) {
				ExibirMensagem.exibirMensagemErro(erro);
			} else {

				mensagemSite.setTipo("atualizarDados");
				mensagemSite.setStatus(true);
				mensagemSiteService.inserirAlterar(mensagemSite);
				criarNovoObjeto();
				//ExibirMensagem.exibirMensagem(
				mensagemCerto =	("Recebemos os seus dados, logo faremos a atualização de cadastro. Assim que isso acontecer você receberá um e-mail com as informações de acesso da nossa plataforma, lá você poderá ver os pesos das coletas, romaneios, certificados, etc. Aguarde o e-mail!");
			
			}

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
		}else {
			ExibirMensagem.exibirMensagemErro(
					"CPF ou CNPJ está incorreto. Informe um CPF/CNPJ válido e que corresponde ao contrato!!");
		}
	}

	public void criarNovoObjeto() {
		mensagemSite = new MensagemSite();
	}

	public MensagemSite getMensagemSite() {
		return mensagemSite;
	}

	public void setMensagemSite(MensagemSite mensagemSite) {
		this.mensagemSite = mensagemSite;
	}

	public String getMensagemCerto() {
		return mensagemCerto;
	}

	public void setMensagemCerto(String mensagemCerto) {
		this.mensagemCerto = mensagemCerto;
	}
	
	

}
