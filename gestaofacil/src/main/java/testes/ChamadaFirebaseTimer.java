package testes;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;

import base.modelo.ColetaApp;
import base.modelo.ColetaAppString;

public class ChamadaFirebaseTimer {

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		int delay = 5000; // delay de 5 seg.
		int interval = 600 * 1000; // intervalo de 1 seg.
		Timer timer = new Timer();
		System.out.println("Iniciando...");
		
		System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
		FirebaseOptions options;
		try {
			options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
					.setDatabaseUrl("https://gestaofacil-a6251.firebaseio.com/").build();
			FirebaseApp.initializeApp(options);
			
			// System.out.println("A");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
		
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Firestore db = null;
				db = FirestoreClient.getFirestore();

				try {
					ApiFuture<QuerySnapshot> query = db.collection("teste")
							.get();
					// ...
					// query.get() blocks on response
					QuerySnapshot querySnapshot = query.get();
					List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
					System.out.println("Quantidade Coletado: " + documents.size()+" - "+new Date());
					for (QueryDocumentSnapshot document : documents) {
						//System.out.println(document.getId());
					
							String encoded = new Gson().toJson(document.getData());
							System.out.println(encoded);
						
						// System.out.println(document.toObject(ColetaApp.class));
					}
			

				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		}, delay, interval);
		
	}

}
