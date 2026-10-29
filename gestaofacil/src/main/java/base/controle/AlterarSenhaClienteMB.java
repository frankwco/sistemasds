package base.controle;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import base.modelo.Cliente;
import base.service.ClienteService;
import dao.GenericDAO;
import util.CriptografiaSenha;
import util.ExibirMensagem;

@ViewScoped
@Named("alterarSenhaClienteMB")
public class AlterarSenhaClienteMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private String senhaAtual;
	private String novaSenha;
	private String confirmarNovaSenha;

	@Inject
	private GenericDAO<Cliente> daoCliente;

	@Inject
	private ClienteService clienteService;

	@PostConstruct
	public void inicializar() {
		senhaAtual = "";
		novaSenha = "";
		confirmarNovaSenha = "";
	}

	private String nomeUsuarioLogado() {
		String nomeUsuario = "";
		Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null) {
			Object obj = authentication.getPrincipal();
			if (obj instanceof UserDetails) {
				nomeUsuario = ((UserDetails) obj).getUsername();
			} else {
				nomeUsuario = obj.toString();
			}
		}
		return nomeUsuario;
	}

	public void alterarSenha() {
		String nomeUsuario = nomeUsuarioLogado();

		List<Cliente> lc = daoCliente.listar(Cliente.class,
				"(email='" + nomeUsuario + "' or email2='" + nomeUsuario + "' or email3='" + nomeUsuario + "')");
		if (lc.isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Não foi possível identificar seu cadastro. Faça login novamente.");
			return;
		}
		Cliente cliente = lc.get(0);

		// email/senha e email2|email3/senha2 são os pares usados na autenticação
		// (ver applicationContext.xml) — login por email2 ou email3 compartilha senha2.
		boolean viaSenha2 = nomeUsuario.equalsIgnoreCase(cliente.getEmail2())
				|| nomeUsuario.equalsIgnoreCase(cliente.getEmail3());
		String hashAtual = viaSenha2 ? cliente.getSenha2() : cliente.getSenha();

		if (senhaAtual == null || senhaAtual.isEmpty() || !CriptografiaSenha.decript(senhaAtual, hashAtual)) {
			ExibirMensagem.exibirMensagemErro("Senha atual incorreta!");
			return;
		}
		if (novaSenha == null || novaSenha.length() < 6) {
			ExibirMensagem.exibirMensagemErro("A nova senha deve ter pelo menos 6 caracteres!");
			return;
		}
		if (!novaSenha.equals(confirmarNovaSenha)) {
			ExibirMensagem.exibirMensagemErro("A confirmação não confere com a nova senha!");
			return;
		}

		String hashNovo = CriptografiaSenha.criptografar(novaSenha);
		if (viaSenha2) {
			cliente.setSenha2(hashNovo);
		} else {
			cliente.setSenha(hashNovo);
		}
		clienteService.inserirAlterar(cliente);

		senhaAtual = "";
		novaSenha = "";
		confirmarNovaSenha = "";
		ExibirMensagem.exibirMensagem("Senha alterada com sucesso!");
	}

	public String getSenhaAtual() {
		return senhaAtual;
	}

	public void setSenhaAtual(String senhaAtual) {
		this.senhaAtual = senhaAtual;
	}

	public String getNovaSenha() {
		return novaSenha;
	}

	public void setNovaSenha(String novaSenha) {
		this.novaSenha = novaSenha;
	}

	public String getConfirmarNovaSenha() {
		return confirmarNovaSenha;
	}

	public void setConfirmarNovaSenha(String confirmarNovaSenha) {
		this.confirmarNovaSenha = confirmarNovaSenha;
	}

}
