package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.AvaliacaoNPS;
import base.service.AvaliacaoNPSService;

@ViewScoped
@Named("avaliacaoNPSMB")
public class AvaliacaoNPSMB implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private AvaliacaoNPSService avaliacaoNPSService;

	private List<AvaliacaoNPS> listaAvaliacoes = new ArrayList<>();

	private int totalAvaliacoes;
	private int promotores;
	private int passivos;
	private int detratores;
	private int scoreNPS;

	@PostConstruct
	public void inicializar() {
		carregarLista();
	}

	public void carregarLista() {
		listaAvaliacoes = avaliacaoNPSService.listar();
		calcularNPS();
	}

	private void calcularNPS() {
		promotores = 0;
		passivos   = 0;
		detratores = 0;

		for (AvaliacaoNPS av : listaAvaliacoes) {
			if (av.getNota() != null) {
				if (av.getNota() >= 9) {
					promotores++;
				} else if (av.getNota() >= 7) {
					passivos++;
				} else {
					detratores++;
				}
			}
		}

		totalAvaliacoes = listaAvaliacoes.size();
		scoreNPS = totalAvaliacoes > 0
				? (int) Math.round(((double)(promotores - detratores) / totalAvaliacoes) * 100.0)
				: 0;
	}

	// ── Getters ───────────────────────────────────────────────────────────────

	public List<AvaliacaoNPS> getListaAvaliacoes() { return listaAvaliacoes; }
	public int getTotalAvaliacoes() { return totalAvaliacoes; }
	public int getPromotores()     { return promotores; }
	public int getPassivos()       { return passivos; }
	public int getDetratores()     { return detratores; }
	public int getScoreNPS()       { return scoreNPS; }
}
