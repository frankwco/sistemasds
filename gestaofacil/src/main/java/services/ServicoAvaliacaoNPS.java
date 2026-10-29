package services;

import java.net.URI;
import java.util.Date;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import base.modelo.AvaliacaoNPS;
import base.modelo.ColetaApp;
import base.service.AvaliacaoNPSService;
import dao.GenericDAO;

@RequestScoped
@Path("/avaliacaoNPS")
public class ServicoAvaliacaoNPS {

	private static final String SITE_URL  = "https://dsresiduos.com.br/avaliacao.php";
	//private static final String SITE_URL  = "http://localhost/dsresiduos_2026_claude/avaliacao.php";

	@Inject
	private AvaliacaoNPSService avaliacaoNPSService;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp;

	/**
	 * Chamado quando o cliente clica em uma nota no e-mail de coleta.
	 * Salva a avaliação e redireciona para a página pública de avaliação.
	 *
	 * Exemplo: GET /rest/avaliacaoNPS/registrar?nota=9&coletaId=42
	 */
	@GET
	@Path("/registrar")
	@Produces("application/json;charset=UTF-8")
	public Response registrar(@QueryParam("nota") Integer nota,
	                          @QueryParam("coletaId") Long coletaId) {

		if (nota == null || nota < 0 || nota > 10) {
			return Response.seeOther(URI.create(SITE_URL)).build();
		}

		// Verifica se já existe avaliação deste ColetaApp hoje — se sim, atualiza a nota
		if (coletaId != null && coletaId > 0) {
			AvaliacaoNPS existente = avaliacaoNPSService.buscarAvaliacaoHoje(coletaId);
			if (existente != null) {
				existente.setNota(nota);
				existente.setDataAvaliacao(new Date());
				avaliacaoNPSService.inserirAlterar(existente);
				String redirect = SITE_URL + "?nota=" + nota + "&id=" + existente.getId();
				return Response.seeOther(URI.create(redirect)).build();
			}
		}

		// Nova avaliação
		AvaliacaoNPS av = new AvaliacaoNPS();
		av.setNota(nota);
		av.setDataAvaliacao(new Date());
		av.setStatus(true);

		if (coletaId != null && coletaId > 0) {
			ColetaApp co = daoColetaApp.buscarPorId(ColetaApp.class, coletaId);
			av.setColetaApp(co);
		}

		try {
			avaliacaoNPSService.inserirAlterar(av);
			String redirect = SITE_URL + "?nota=" + nota + "&id=" + av.getId();
			return Response.seeOther(URI.create(redirect)).build();
		} catch (Exception e) {
			return Response.seeOther(URI.create(SITE_URL + "?nota=" + nota)).build();
		}
	}

	/**
	 * Recebe o comentário digitado pelo cliente na página dsresiduos.com.br/avaliacao.
	 *
	 * Body JSON: { "id": 123, "comentario": "Ótimo serviço!" }
	 */
	@POST
	@Path("/comentario")
	@Consumes("application/json;charset=UTF-8")
	@Produces("application/json;charset=UTF-8")
	public Response salvarComentario(String body) {
		try {
			JsonObject json = new Gson().fromJson(body, JsonObject.class);
			Long id = json.get("id").getAsLong();
			String comentario = json.get("comentario").getAsString();

			// Limita o tamanho para evitar abuso
			if (comentario != null && comentario.length() > 2000) {
				comentario = comentario.substring(0, 2000);
			}

			avaliacaoNPSService.atualizarComentario(id, comentario);

			return Response.ok("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			return Response.status(400).entity("{\"status\":\"erro\"}").build();
		}
	}

	/**
	 * Atualiza a nota de uma avaliação já registrada.
	 * Chamado quando o cliente muda a nota na página dsresiduos.com.br/avaliacao.
	 *
	 * Body JSON: { "id": 123, "nota": 8 }
	 */
	@POST
	@Path("/atualizarNota")
	@Consumes("application/json;charset=UTF-8")
	@Produces("application/json;charset=UTF-8")
	public Response atualizarNota(String body) {
		try {
			JsonObject json = new Gson().fromJson(body, JsonObject.class);
			Long id   = json.get("id").getAsLong();
			Integer nota = json.get("nota").getAsInt();

			if (nota < 0 || nota > 10) {
				return Response.status(400).entity("{\"status\":\"nota invalida\"}").build();
			}

			avaliacaoNPSService.atualizarNota(id, nota);

			return Response.ok("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			return Response.status(400).entity("{\"status\":\"erro\"}").build();
		}
	}
}
