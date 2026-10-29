package base.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;


import base.modelo.Cliente;
import base.modelo.Usuario;
import dao.GenericDAO;
import util.Transacional;

public class ClienteService implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<Cliente> daoTipo;
	
	
	@Transacional
	public void inserirAlterar(Cliente tipo){
		if(tipo.getId()==null){
			daoTipo.inserir(tipo);
		}else{
			daoTipo.alterar(tipo);
		}
	}
	
	@Transacional
	public void updateSenhaCliente(String senha, String email){
		daoTipo.updateSenhaCliente(senha, email);
	}
	
	@Transacional
	public void updateSenhaClienteEmail2(String senha, String email){
		daoTipo.updateSenhaClienteEmail2(senha, email);
	}

	@Transacional
	public void definirSenhaAdmin(Long idCliente, String senhaHash, Date validade){
		daoTipo.updateSenhaAdminCliente(senhaHash, validade, idCliente);
	}

	@Transacional
	public void revogarSenhaAdmin(Long idCliente){
		daoTipo.revogarSenhaAdminCliente(idCliente);
	}

}
