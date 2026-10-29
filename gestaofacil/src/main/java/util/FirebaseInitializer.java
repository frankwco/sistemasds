package util;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.ServletContext;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Inicialização centralizada do Firebase Admin SDK.
 *
 * Antes, a inicialização só acontecia se um administrador abrisse a tela
 * "Importar Firebase" (ImportarFirebaseMB), o que deixava qualquer outro
 * consumidor do Firestore (como o serviço automático de sincronização)
 * sujeito a falhar se ninguém tivesse visitado aquela página ainda.
 *
 * Usa FirebaseApp.getApps() (API pública) em vez de uma flag própria,
 * para que múltiplos pontos de entrada possam chamar inicializar() com
 * segurança sem lançar "FirebaseApp name [DEFAULT] already exists".
 */
public class FirebaseInitializer {

	private FirebaseInitializer() {
	}

	public static synchronized void inicializar(ServletContext contexto) {
		if (!FirebaseApp.getApps().isEmpty()) {
			return;
		}

		try (InputStream serviceAccount = new FileInputStream(
				contexto.getRealPath("/WEB-INF/config/gestaofacil-a6251-bb79e3546d2f.json"))) {

			GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
			FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials)
					.setDatabaseUrl("https://gestaofacil-a6251.firebaseio.com/")
					.setStorageBucket("gestaofacil-a6251.appspot.com").build();

			FirebaseApp.initializeApp(options);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
