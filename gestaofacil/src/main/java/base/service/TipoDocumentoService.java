package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.TipoDocumento;
import dao.GenericDAO;
import util.Transacional;

public class TipoDocumentoService implements Serializable {

	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<TipoDocumento> dao;

	@Transacional
	public void inserirAlterar(TipoDocumento tipoDocumento) {
		if (tipoDocumento.getId() == null) {
			dao.inserir(tipoDocumento);
		} else {
			dao.alterar(tipoDocumento);
		}
	}

}
