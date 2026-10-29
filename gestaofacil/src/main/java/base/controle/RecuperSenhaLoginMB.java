package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.persistence.sessions.DatasourceLogin;

import util.CriptografiaSenha;
import util.EnviarEmail;
import util.ExibirMensagem;
import util.GeradorSenhas;
import util.Mensagem;
import base.modelo.Cliente;
import base.modelo.Usuario;
import base.service.ClienteService;
import base.service.UsuarioService;
import dao.GenericDAO;

@ViewScoped
@Named("recuperSenhaLoginMB")
public class RecuperSenhaLoginMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private String email;

	private List<Usuario> lista;
	private String mensagem;

	@Inject
	private GenericDAO<Usuario> daoPessoa;

	@Inject
	private GenericDAO<Cliente> daoCliente;

	@Inject
	private UsuarioService pessoaService;

	@Inject
	private ClienteService clienteService;

	@PostConstruct
	public void inicializa() {
		lista = new ArrayList<>();
		mensagem = "";
		email = "";
	}
	
//AQUI SÓ ESTA VERIFICANDO NO EMAIL 1, PRECISA DOS OUTROS DOIS
	public Boolean buscarEmail() {
		lista = daoPessoa.listaComStatus(Usuario.class);
		List<Cliente> lc = daoCliente.listaComStatus(Cliente.class);
		if (lista.stream().anyMatch(p -> email.equals(p.getEmail()))) {
			return true;
		} else if (lc.stream().anyMatch(p -> email.equals(p.getEmail()))) {
			return true;
		}else if (lc.stream().anyMatch(p -> email.equals(p.getEmail2()))) {
			return true;
		}else if (lc.stream().anyMatch(p -> email.equals(p.getEmail3()))) {
			return true;
		}
		return false;
	}

	public void recuperarSenhaLogin() {
		//System.out.println("Recuperando a senha");
		String senha;
		String novaSenha;
		if (buscarEmail()) {
			senha = GeradorSenhas.gerarSenha();
			novaSenha = senha.charAt(0) + "" + senha.charAt(1) + "" + senha.charAt(3) + "" + senha.charAt(5) + ""
					+ senha.charAt(6);
			pessoaService.updateSenha(CriptografiaSenha.criptografar(novaSenha), email);
			clienteService.updateSenhaCliente(CriptografiaSenha.criptografar(novaSenha), email);
			if (EnviarEmail.enviarEmail(email, "DS Resíduos - Recuperação de senha",
					"Olá, fizemos a recuperação da sua senha no sistema de gestão da DS Resíduos " + "<br/><br/>"
							+ "Sua nova senha: " + novaSenha
							+ "<br/><br/>Muito obrigado por utilizar o sistema, estamos trabalhando para facilitar a gestão da sua empresa.")) {
				mensagem = "E-mail enviado";
			} else {
				
				mensagem = "Erros ao enviar e-mail";
			}
		} else {
			mensagem = "E-mail não encontrado";
		}
		
		ExibirMensagem.exibirMensagem(mensagem);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMensagem() {
		return mensagem;
	}

	public void setMensagem(String mensagem) {
		this.mensagem = mensagem;
	}
}
