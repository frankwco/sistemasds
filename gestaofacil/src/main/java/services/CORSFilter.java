package services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    // Origens aceitas pelo endpoint de avaliação NPS (site público)
    private static final List<String> NPS_ORIGINS = Arrays.asList(
        "https://dsresiduos.com.br",
        "https://www.dsresiduos.com.br",
        "http://localhost",
        "http://127.0.0.1"
    );

    @Override
    public void filter(ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {

        String path = request.getUriInfo().getPath();

        if (path != null && path.startsWith("avaliacaoNPS")) {
            // Endpoint restrito: só aceita requisições do site de avaliação
            String origin = request.getHeaderString("Origin");
            if (origin != null && NPS_ORIGINS.contains(origin)) {
                response.getHeaders().add("Access-Control-Allow-Origin", origin);
                response.getHeaders().add("Vary", "Origin");
            }
            // Se a origem não estiver na lista, o header não é adicionado
            // e o browser bloqueia a requisição cross-origin
        } else {
            // Demais endpoints (ex: /service para o app Ionic): aceita qualquer origem
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
        }

        response.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, x-app-key");
        response.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}
