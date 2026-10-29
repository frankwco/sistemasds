package base.service;

import java.io.Serializable;

import javax.inject.Inject;


import base.modelo.Configuracao;
import dao.GenericDAO;
import util.Transacional;

public class ConfiguracaoService implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<Configuracao> dao;
	
	@Transacional
	public void inserirAlterar(Configuracao objeto){
		if(objeto.getId()==null){
			dao.inserir(objeto);
		}else{
			dao.alterar(objeto);
		}
	}

}
