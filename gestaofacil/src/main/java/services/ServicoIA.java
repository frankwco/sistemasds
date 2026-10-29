package services;

import java.util.Base64;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import base.modelo.InterpretacaoIADTO;
import base.modelo.InterpretacaoRequisicaoDTO;
import util.GeminiClient;

/**
 * Captura assistida por IA (RF11-15) — endpoints isolados de
 * {@link ServicoApp} porque cada chamada aqui custa dinheiro (API do
 * Gemini) e pode demorar alguns segundos; separar deixa mais fácil de achar
 * na hora de medir uso/latência. Nunca persiste nada — só devolve a
 * interpretação pro app mostrar a tela de confirmação (RF14); quem grava o
 * {@code LancamentoVeiculo} de fato é o motorista confirmando, via
 * POST /app/lancamentos de sempre.
 */
@Path("/app/ia")
@ExigeChaveApp
public class ServicoIA {

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/audio")
	public Response interpretarAudio(InterpretacaoRequisicaoDTO requisicao) {
		return interpretar(requisicao, true);
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/foto")
	public Response interpretarFoto(InterpretacaoRequisicaoDTO requisicao) {
		return interpretar(requisicao, false);
	}

	/**
	 * Só texto — falado (via reconhecimento de voz nativo, se o app tiver) ou
	 * digitado direto. Mesmo mecanismo da tela administrativa: sem mídia
	 * nenhuma, a resposta do Gemini sai bem mais rápido.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/texto")
	public Response interpretarTexto(InterpretacaoRequisicaoDTO requisicao) {
		if (requisicao == null || requisicao.getTexto() == null || requisicao.getTexto().trim().isEmpty()) {
			return Response.status(400).build();
		}
		try {
			InterpretacaoIADTO resultado = GeminiClient.interpretarTexto(requisicao.getTexto().trim());
			return Response.status(200).entity(resultado).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(502).build();
		}
	}

	/**
	 * Foto (ou PDF) complementada, opcionalmente, por um texto — permite o
	 * app mandar os dois juntos numa chamada só, igual a tela administrativa
	 * faz no chat da Captura por IA.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/fotoTexto")
	public Response interpretarFotoTexto(InterpretacaoRequisicaoDTO requisicao) {
		if (requisicao == null || requisicao.getBase64() == null || requisicao.getMimeType() == null) {
			return Response.status(400).build();
		}
		try {
			byte[] midia = Base64.getDecoder().decode(requisicao.getBase64());
			InterpretacaoIADTO resultado = GeminiClient.interpretarFotoComTexto(midia, requisicao.getMimeType(),
					requisicao.getTexto());
			return Response.status(200).entity(resultado).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(502).build();
		}
	}

	private Response interpretar(InterpretacaoRequisicaoDTO requisicao, boolean isAudio) {
		if (requisicao == null || requisicao.getBase64() == null || requisicao.getMimeType() == null) {
			return Response.status(400).build();
		}

		try {
			byte[] midia = Base64.getDecoder().decode(requisicao.getBase64());
			InterpretacaoIADTO resultado = isAudio ? GeminiClient.interpretarAudio(midia, requisicao.getMimeType())
					: GeminiClient.interpretarFoto(midia, requisicao.getMimeType());
			return Response.status(200).entity(resultado).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(502).build();
		}
	}

}
