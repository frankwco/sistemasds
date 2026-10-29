package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.DocumentoMotorista;
import dao.GenericDAO;
import util.Transacional;

public class DocumentoMotoristaService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<DocumentoMotorista> dao;

	@Transacional
	public void inserirAlterar(DocumentoMotorista documento) {
		if (documento.getId() == null) {
			dao.inserir(documento);
		} else {
			dao.alterar(documento);
		}
	}

}
