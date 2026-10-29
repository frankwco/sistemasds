package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.Anexo;
import dao.GenericDAO;
import util.Transacional;

public class AnexoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<Anexo> dao;

	@Transacional
	public void inserirAlterar(Anexo anexo) {
		if (anexo.getId() == null) {
			dao.inserir(anexo);
		} else {
			dao.alterar(anexo);
		}
	}

}
