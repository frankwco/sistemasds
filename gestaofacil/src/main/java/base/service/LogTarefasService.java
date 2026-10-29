package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.CategoriaIndicador;
import base.modelo.LogTarefas;
import base.modelo.Tipo;
import dao.GenericDAO;
import util.Transacional;

public class LogTarefasService implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<LogTarefas> dao;
	
	@Transacional
	public void inserirAlterar(LogTarefas tipo){
		if(tipo.getId()==null){
			dao.inserir(tipo);
		}else{
			dao.alterar(tipo);
		}
	}


}
