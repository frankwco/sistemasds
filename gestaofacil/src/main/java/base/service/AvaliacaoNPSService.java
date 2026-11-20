package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import base.modelo.AvaliacaoNPS;
import base.modelo.ColetaApp;
import dao.GenericDAO;
import util.Transacional;

/**
 * Regra: existe no maximo UMA avaliacao NPS ativa por coleta (ColetaApp).
 * Cliques repetidos no e-mail e alteracoes feitas na pagina do site sempre
 * sobrescrevem a avaliacao dessa coleta, ficando valendo a ultima.
 */
public class AvaliacaoNPSService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<AvaliacaoNPS> dao;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp;

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

	/**
	 * Registra (ou sobrescreve) a nota da coleta informada. Se ja houver
	 * avaliacoes dessa coleta, atualiza a mais recente e desativa as demais
	 * (duplicadas antigas). Retorna o id da avaliacao que ficou valendo.
	 */
	@Transacional
	public Long registrarNota(Long coletaAppId, Integer nota) {
		AvaliacaoNPS av = null;
		if (coletaAppId != null && coletaAppId > 0) {
			List<AvaliacaoNPS> existentes = listarAtivasDaColeta(coletaAppId);
			if (!existentes.isEmpty()) {
				av = existentes.get(0);
				desativarDuplicadas(existentes, av);
			}
		}

		if (av == null) {
			av = new AvaliacaoNPS();
			av.setStatus(true);
			if (coletaAppId != null && coletaAppId > 0) {
				av.setColetaApp(daoColetaApp.buscarPorId(ColetaApp.class, coletaAppId));
			}
		}

		av.setNota(nota);
		av.setDataAvaliacao(new Date());
		if (av.getId() == null) {
			dao.inserir(av);
		} else {
			dao.alterar(av);
		}
		manager.flush();
		return av.getId();
	}

	@Transacional
	public void atualizarComentario(Long id, String comentario) {
		AvaliacaoNPS av = avaliacaoVigente(id);
		if (av != null) {
			av.setComentario(comentario);
			dao.alterar(av);
		}
	}

	@Transacional
	public void atualizarNota(Long id, Integer nota) {
		AvaliacaoNPS av = avaliacaoVigente(id);
		if (av != null) {
			av.setNota(nota);
			av.setDataAvaliacao(new Date());
			dao.alterar(av);
		}
	}

	/**
	 * Lista as avaliacoes ativas, mostrando so a mais recente de cada coleta
	 * (nao conta duas vezes a mesma coleta caso existam duplicadas antigas).
	 */
	public List<AvaliacaoNPS> listar() {
		List<AvaliacaoNPS> todas = dao.listar(AvaliacaoNPS.class,
				"status=true order by dataAvaliacao desc, id desc");
		List<AvaliacaoNPS> resultado = new ArrayList<>();
		Set<Long> coletasVistas = new HashSet<>();
		for (AvaliacaoNPS av : todas) {
			if (av.getColetaApp() == null || coletasVistas.add(av.getColetaApp().getId())) {
				resultado.add(av);
			}
		}
		return resultado;
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

	// ── Internos ─────────────────────────────────────────────────────────────

	/** Avaliacoes ativas da coleta, da mais recente para a mais antiga. */
	private List<AvaliacaoNPS> listarAtivasDaColeta(Long coletaAppId) {
		return manager
				.createQuery("from AvaliacaoNPS where coletaApp.id = :cid and status = true"
						+ " order by dataAvaliacao desc, id desc", AvaliacaoNPS.class)
				.setParameter("cid", coletaAppId)
				.getResultList();
	}

	private void desativarDuplicadas(List<AvaliacaoNPS> existentes, AvaliacaoNPS manter) {
		for (AvaliacaoNPS outra : existentes) {
			if (!outra.getId().equals(manter.getId())) {
				outra.setStatus(false);
				dao.alterar(outra);
			}
		}
	}

	/**
	 * A pagina do site guarda o id recebido no clique. Se esse id for de uma
	 * avaliacao antiga/duplicada da coleta, a alteracao vai para a avaliacao
	 * vigente (a mais recente) dessa coleta, e as outras sao desativadas.
	 */
	private AvaliacaoNPS avaliacaoVigente(Long id) {
		AvaliacaoNPS av = dao.buscarPorId(AvaliacaoNPS.class, id);
		if (av == null || av.getColetaApp() == null) {
			return av;
		}
		List<AvaliacaoNPS> existentes = listarAtivasDaColeta(av.getColetaApp().getId());
		if (existentes.isEmpty()) {
			av.setStatus(true);
			return av;
		}
		AvaliacaoNPS vigente = existentes.get(0);
		desativarDuplicadas(existentes, vigente);
		return vigente;
	}
}
