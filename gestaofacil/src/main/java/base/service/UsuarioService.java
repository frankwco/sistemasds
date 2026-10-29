package base.service;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import base.modelo.CategoriaIndicador;
import base.modelo.Pessoa;
import base.modelo.Tipo;
import base.modelo.Usuario;
import dao.GenericDAO;
import util.CriptografiaSenha;
import util.EnviarEmail;
import util.GeradorSenhas;
import util.Transacional;

public class UsuarioService implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Inject
	private GenericDAO<base.modelo.Usuario> dao;
	
	@Transacional
	public void inserirAlterar(Usuario tipo){
		if(tipo.getId()==null){
			dao.inserir(tipo);
		}else{
			dao.alterar(tipo);
		}
	}
	
	@Transacional
	public void updateSenha(String senha, String email){
		dao.updateSenha(senha, email);
	}
	
	@Transacional
	public void updateSenhaEmail2(String senha, String email){
		dao.updateSenhaEmail2(senha, email);
	}
	
	public Boolean buscarEmail(String email) {
		List<Usuario> lista = dao.listaComStatus(Usuario.class);
		System.out.println(email);
		if (lista.stream().anyMatch(p -> p.getEmail().equals(email))) {
			return true;
		} 
		return false;
	}
	
	@Transacional
	public String recuperarSenhaLogin(String email) {
		String senha;
		String novaSenha;
		
		if (buscarEmail(email)) {
			senha = GeradorSenhas.gerarSenha();
			novaSenha = senha.charAt(0) + "" + senha.charAt(1) + "" + senha.charAt(3) + "" + senha.charAt(5) + ""
					+ senha.charAt(6);
			updateSenha(CriptografiaSenha.criptografar(novaSenha), email);
			
			if (EnviarEmail.enviarEmail(email, "DS Resíduos - Recuperação de senha",
					"Olá, fizemos a recuperação da sua senha no sistema de gestão da DS Resíduos " + "\n\n"
							+ "Sua nova senha: " + novaSenha
							+ "\n\nMuito obrigado por utilizar o sistema, estamos trabalhando para facilitar a gestão da sua empresa.")) {
				return "E-mail enviado";
			} else {
				return "Erros ao enviar e-mail";
			}
		} else {
			return "E-mail não encontrado";
		}
	}


}
