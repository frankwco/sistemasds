package testes;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.BasicConfigurator;
import org.hibernate.persister.collection.QueryableCollection;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import base.modelo.ColetaApp;

//import org.tensorflow.Graph;
//import org.tensorflow.Session;
//import org.tensorflow.Tensor;
//import org.tensorflow.TensorFlow;

public class HelloTF {

	public static void main(String[] args)  {
		//BasicConfigurator.configure();
		//System.out.println("AAAA");
		Firestore db = null;
		FirebaseOptions options;
		try {
			options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
					.setDatabaseUrl("https://gestaofacil-a6251.firebaseio.com/").build();
			FirebaseApp.initializeApp(options);
			db = FirestoreClient.getFirestore();
			//System.out.println("A");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("A333");
			e.printStackTrace();
		}
		
		try {
			ApiFuture<QuerySnapshot> query = db.collection("coleta").document("veiculo").collection("192021").get();
			// ...
			// query.get() blocks on response
			QuerySnapshot querySnapshot = query.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			System.out.println("Quantidade Coletado: "+documents.size());
			for (QueryDocumentSnapshot document : documents) {
				//System.out.println(document.getId());
				if(Double.parseDouble(document.getId())==11130000
){
					System.out.println("A: "+document.getDouble("a"));
					System.out.println("E: "+document.getDouble("e"));
					System.out.println("B: "+document.getDouble("b"));
					System.out.println("A2: "+document.getDouble("a2"));
					System.out.println("A3: "+document.getDouble("a3"));
					System.out.println(document.getString("pontoColetaSelecionado.nomeRazaoSocial"));
				}
				//System.out.println(document.toObject(ColetaApp.class));
			}
//				System.out.println("User: " + document.getId());
//				System.out.println("First: " + document.getString("first"));
//				if (document.contains("middle")) {
//					System.out.println("Middle: " + document.getString("middle"));
//				}
//				System.out.println("Last: " + document.getString("last"));
//				System.out.println("Born: " + document.getLong("born"));
//			}
			//System.out.println("As");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		DocumentReference docRef = db.collection("users").document("alovelace");
//		// Add document data with id "alovelace" using a hashmap
//		Map<String, Object> data = new HashMap<>();
//		data.put("first", "Ada");
//		data.put("last", "Lovelace");
//		data.put("born", 1815);
//		// asynchronously write data
//		ApiFuture<WriteResult> result = docRef.set(data);
//		// ...
//		// result.get() blocks on response
//		try {
//			System.out.println("Update time : " + result.get().getUpdateTime());
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

	}




	private void salvar() {
//		DocumentReference docRef = db.collection("users").document("alovelace");
//		// Add document data with id "alovelace" using a hashmap
//		Map<String, Object> data = new HashMap<>();
//		data.put("first", "Ada");
//		data.put("last", "Lovelace");
//		data.put("born", 1815);
//		// asynchronously write data
//		ApiFuture<WriteResult> result = docRef.set(data);
//		// ...
//		// result.get() blocks on response
//		try {
//			System.out.println("Update time : " + result.get().getUpdateTime());
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ExecutionException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
}