package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.CategoriaIndicador;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppAssinatura;
import base.modelo.Tipo;
import dao.GenericDAO;
import util.Transacional;

public class ColetaAppAssinaturaService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<ColetaAppAssinatura> dao;

	@Transacional
	public void removerAssinaturaPorColeta(Long idColeta) {
		for (ColetaAppAssinatura c : dao.listar(ColetaAppAssinatura.class, "coletaApp.id=" + idColeta)) {
			dao.remover(ColetaAppAssinatura.class, c.getId());
		}

	}

	@Transacional
	public void inserirAlterar(ColetaAppAssinatura tipo) {
		if (tipo.getId() == null) {
			dao.inserir(tipo);
		} else {
			dao.alterar(tipo);
		}
	}

}
