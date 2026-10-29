package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.DocumentoVeiculo;
import dao.GenericDAO;
import util.Transacional;

public class DocumentoVeiculoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<DocumentoVeiculo> dao;

	@Transacional
	public void inserirAlterar(DocumentoVeiculo documento) {
		if (documento.getId() == null) {
			dao.inserir(documento);
		} else {
			dao.alterar(documento);
		}
	}

}
