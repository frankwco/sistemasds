package base.service;

import java.io.Serializable;

import javax.inject.Inject;


import base.modelo.ConfiguracaoAjustes;
import dao.GenericDAO;
import util.Transacional;

public class ConfiguracaoAjusteService implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<ConfiguracaoAjustes> daoTipo;
	
	@Transacional
	public void inserirAlterar(ConfiguracaoAjustes tipo){
		if(tipo.getId()==null){
			daoTipo.inserir(tipo);
		}else{
			daoTipo.alterar(tipo);
		}
	}

}
