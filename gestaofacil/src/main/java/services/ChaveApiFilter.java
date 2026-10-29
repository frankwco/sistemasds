package services;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Exige o header X-App-Key em todo endpoint anotado com {@link ExigeChaveApp}
 * (hoje, a classe {@link ServicoApp} inteira).
 *
 * Não é proteção contra um atacante determinado — a chave fica embutida no
 * APK do appgestaofacil e pode ser extraída por engenharia reversa. O
 * objetivo é fechar a exposição total de hoje (qualquer um podendo chamar
 * os endpoints do app sem nada) por algo equivalente a uma chave de API
 * simples, consistente com o resto da segurança deste projeto.
 *
 * Para trocar a chave: atualizar o valor aqui (ou via -DappApiKey=... na
 * JVM) E em appgestaofacil (ConstantesService.tokenApi), e publicar os dois
 * lados juntos — um sem o outro derruba a sincronização do app.
 */
@Provider
@ExigeChaveApp
@Priority(Priorities.AUTHENTICATION)
public class ChaveApiFilter implements ContainerRequestFilter {

	private static final String CABECALHO = "X-App-Key";

	private static final String CHAVE_ESPERADA = System.getProperty("appApiKey",
			"gf-app-7f3a9c2e-51b6-4d8a-9c0f-2e6a8b1d4f77");

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
			// Preflight de CORS não envia headers customizados — deixa passar,
			// senão o navegador nunca chega a enviar a requisição real.
			return;
		}

		String chave = requestContext.getHeaderString(CABECALHO);
		if (chave == null || !chave.equals(CHAVE_ESPERADA)) {
			requestContext.abortWith(
					Response.status(Response.Status.UNAUTHORIZED).entity("Chave de API ausente ou inválida").build());
		}
	}
}
