package services;

import java.net.URI;

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

import base.service.AvaliacaoNPSService;

@RequestScoped
@Path("/avaliacaoNPS")
public class ServicoAvaliacaoNPS {

	private static final String SITE_URL  = "https://dsresiduos.com.br/avaliacao.php";
	//private static final String SITE_URL  = "http://localhost/dsresiduos_2026_claude/avaliacao.php";

	/** Serializa as gravações de NPS (volume baixo; evita duplicar por cliques simultâneos). */
	private static final Object LOCK_REGISTRO = new Object();

	@Inject
	private AvaliacaoNPSService avaliacaoNPSService;

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

		// Uma única avaliação por coleta: cliques repetidos sobrescrevem a nota.
		// A trava evita que cliques simultâneos (duplo clique, ou o antivírus do
		// e-mail abrindo os links) criem várias avaliações para a mesma coleta —
		// o commit acontece ao sair do método do service, ainda dentro da trava.
		try {
			Long id;
			synchronized (LOCK_REGISTRO) {
				id = avaliacaoNPSService.registrarNota(coletaId, nota);
			}
			String redirect = SITE_URL + "?nota=" + nota + "&id=" + id;
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

			synchronized (LOCK_REGISTRO) {
				avaliacaoNPSService.atualizarComentario(id, comentario);
			}

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

			synchronized (LOCK_REGISTRO) {
				avaliacaoNPSService.atualizarNota(id, nota);
			}

			return Response.ok("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			return Response.status(400).entity("{\"status\":\"erro\"}").build();
		}
	}
}
