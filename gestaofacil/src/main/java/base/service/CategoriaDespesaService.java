package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.CategoriaDespesa;
import dao.GenericDAO;
import util.Transacional;

public class CategoriaDespesaService implements Serializable {

	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<CategoriaDespesa> dao;

	@Transacional
	public void inserirAlterar(CategoriaDespesa categoriaDespesa) {
		if (categoriaDespesa.getId() == null) {
			dao.inserir(categoriaDespesa);
		} else {
			dao.alterar(categoriaDespesa);
		}
	}

}
