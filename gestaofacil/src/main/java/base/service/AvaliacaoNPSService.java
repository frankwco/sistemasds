package base.service;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;

import base.modelo.AvaliacaoNPS;
import dao.GenericDAO;
import util.Transacional;

public class AvaliacaoNPSService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<AvaliacaoNPS> dao;

	@Inject
	private EntityManager manager;

	@Transacional
	public void inserirAlterar(AvaliacaoNPS av) {
		if (av.getId() == null) {
			dao.inserir(av);
		} else {
			dao.alterar(av);
		}
	}

	@Transacional
	public void atualizarComentario(Long id, String comentario) {
		AvaliacaoNPS av = dao.buscarPorId(AvaliacaoNPS.class, id);
		if (av != null) {
			av.setComentario(comentario);
			dao.alterar(av);
		}
	}

	@Transacional
	public void atualizarNota(Long id, Integer nota) {
		AvaliacaoNPS av = dao.buscarPorId(AvaliacaoNPS.class, id);
		if (av != null) {
			av.setNota(nota);
			dao.alterar(av);
		}
	}

	public List<AvaliacaoNPS> listar() {
		return dao.listar(AvaliacaoNPS.class, "status=true order by dataAvaliacao desc");
	}

	/**
	 * Desvincula (sem apagar) as avaliações NPS de uma ColetaApp que está
	 * prestes a ser removida. Nunca apaga a avaliação em si — é feedback real
	 * de cliente — só solta a referência pra não travar o DELETE por FK.
	 */
	@Transacional
	public void desvincularColetaApp(Long idColeta) {
		for (AvaliacaoNPS av : dao.listar(AvaliacaoNPS.class, "coletaApp.id=" + idColeta)) {
			av.setColetaApp(null);
			dao.alterar(av);
		}
	}

	/**
	 * Retorna a avaliação já existente do ColetaApp informado para o dia de hoje,
	 * ou null se não houver nenhuma.
	 */
	public AvaliacaoNPS buscarAvaliacaoHoje(Long coletaAppId) {
		try {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date inicio = cal.getTime();
			cal.add(Calendar.DAY_OF_MONTH, 1);
			Date fim = cal.getTime();

			List<AvaliacaoNPS> lista = manager
					.createQuery("from AvaliacaoNPS where coletaApp.id = :cid"
							+ " and dataAvaliacao >= :inicio and dataAvaliacao < :fim"
							+ " and status = true", AvaliacaoNPS.class)
					.setParameter("cid", coletaAppId)
					.setParameter("inicio", inicio, TemporalType.TIMESTAMP)
					.setParameter("fim",    fim,    TemporalType.TIMESTAMP)
					.setMaxResults(1)
					.getResultList();

			return lista.isEmpty() ? null : lista.get(0);
		} catch (Exception e) {
			return null;
		}
	}
}
