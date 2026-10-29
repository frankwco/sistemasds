package util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Agenda, dentro do próprio servidor, a chamada periódica ao endpoint
 * /app/consumirFirebase — que reconcilia o que o app gravou no Firestore
 * (inserções e remoções de coletas) com o banco relacional, para os casos em
 * que o envio direto via REST do app não tenha chegado.
 *
 * Sobe junto com a aplicação (contextInitialized dispara em todo start/
 * redeploy) e não depende de nenhum agendador externo ao servidor (cron,
 * Agendador de Tarefas do Windows, etc.) — então funciona igual em qualquer
 * ambiente onde o WAR for implantado, sem precisar configurar nada de novo
 * a cada deploy/servidor.
 *
 * A chamada é feita via HTTP local (loopback) para o próprio endpoint REST
 * em vez de chamar o serviço diretamente em Java, porque o EntityManager
 * desta aplicação é @RequestScoped (CDI/Weld) — só existe dentro de uma
 * requisição HTTP de verdade. Fazendo o loopback, a requisição entra pelo
 * pipeline normal do Jersey e o CDI ativa o escopo corretamente, sem
 * nenhuma mágica extra.
 *
 * Configurável via propriedades de sistema, sem precisar recompilar nem
 * editar o web.xml (ex.: -DfirebaseSync.porta=8081 na inicialização do
 * servidor, só quando a porta padrão do Tomcat não for a 8080):
 *   firebaseSync.habilitado        (padrão "true")
 *   firebaseSync.porta             (padrão "8080")
 *   firebaseSync.intervaloSegundos (padrão "14400" = 4 horas — mantido
 *                                    longo de propósito para não consumir
 *                                    muita leitura do Firestore, já que
 *                                    hoje isso é só uma malha de segurança
 *                                    em paralelo ao envio direto via REST)
 */
public class FirebaseSyncSchedulerListener implements ServletContextListener {

	private static final int ATRASO_INICIAL_SEGUNDOS = 60;

	private ScheduledExecutorService executor;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// Inicializa o Firebase Admin SDK aqui, onde temos um ServletContext de
		// verdade garantido. Não fazer isso dentro da requisição JAX-RS: com
		// Jersey 2.22 (jersey-cdi1x), quando o CDI cria a instância do bean em
		// vez do próprio Jersey, @Context ServletContext às vezes não é
		// preenchido — causava NullPointerException em consumirFirebase().
		FirebaseInitializer.inicializar(sce.getServletContext());

		boolean habilitado = Boolean.parseBoolean(System.getProperty("firebaseSync.habilitado", "true"));
		if (!habilitado) {
			System.out.println("[FirebaseSync] Desabilitado via firebaseSync.habilitado=false");
			return;
		}

		String porta = System.getProperty("firebaseSync.porta", "8080");
		int intervaloSegundos = lerInteiro("firebaseSync.intervaloSegundos", 4 * 60 * 60);
		String contextPath = sce.getServletContext().getContextPath();
		// /app (não /service): consumirFirebase agora exige a chave de API
		// (ver services.ExigeChaveApp/ChaveApiFilter/ServicoApp).
		String url = "http://localhost:" + porta + contextPath + "/rest/app/consumirFirebase";

		ThreadFactory threadFactory = r -> {
			Thread t = new Thread(r, "firebase-sync-scheduler");
			t.setDaemon(true);
			return t;
		};
		executor = Executors.newSingleThreadScheduledExecutor(threadFactory);

		// scheduleWithFixedDelay (não fixedRate): se uma execução demorar mais que
		// o intervalo, a próxima só começa depois que a anterior terminar.
		executor.scheduleWithFixedDelay(() -> chamar(url), ATRASO_INICIAL_SEGUNDOS, intervaloSegundos,
				TimeUnit.SECONDS);

		System.out.println("[FirebaseSync] Agendado: " + url + " a cada " + intervaloSegundos + "s (primeira "
				+ "chamada em " + ATRASO_INICIAL_SEGUNDOS + "s)");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	private void chamar(String urlStr) {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			// consumirFirebase agora exige a chave de API (ExigeChaveApp/ChaveApiFilter).
			conn.setRequestProperty("X-App-Key",
					System.getProperty("appApiKey", "gf-app-7f3a9c2e-51b6-4d8a-9c0f-2e6a8b1d4f77"));
			conn.setConnectTimeout(10000);
			// Os 150s antigos só contavam o tempo de consulta ao Firestore (JANELA_DIAS
			// x 15s = 75s + margem), mas cada candidato a inserção/remoção também roda
			// a lógica de negócio completa de cadastroColeta (e-mail, financeiro etc.)
			// dentro da mesma requisição, sem timeout próprio — com o volume atual
			// (~600 documentos/ciclo) isso passa fácil de 150s mesmo sem nenhum erro.
			// scheduleWithFixedDelay garante que não há execução sobreposta, então uma
			// margem maior aqui é segura.
			conn.setReadTimeout(600000);

			int status = conn.getResponseCode();
			if (status != 200) {
				System.err.println("[FirebaseSync] consumirFirebase retornou status " + status);
			}
		} catch (Exception e) {
			// Não deixa uma falha pontual (ex.: servidor ainda subindo, Firebase fora do
			// ar) derrubar o agendamento — a próxima tentativa acontece normalmente.
			System.err.println("[FirebaseSync] Falha ao chamar consumirFirebase: " + e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private int lerInteiro(String chave, int padrao) {
		try {
			return Integer.parseInt(System.getProperty(chave, String.valueOf(padrao)));
		} catch (NumberFormatException e) {
			return padrao;
		}
	}
}
