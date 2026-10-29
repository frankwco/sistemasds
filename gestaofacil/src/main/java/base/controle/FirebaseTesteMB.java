package base.controle;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.annotations.Nullable;

import base.modelo.Pessoa;
import base.modelo.Atividade;
import base.service.AtividadeService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ApplicationScoped
@Named("firebaseTesteMB")
public class FirebaseTesteMB implements Serializable {
	
	Firestore db = null;

	@Inject
	private GenericDAO<Atividade> daoProcesso; // faz as buscas

	@Inject
	private AtividadeService atividadeService; // inserir no banco

	@PostConstruct
	public void inicializar() {

		
		//conectarFire();
	
		
		//ler();
	}
	
	public void conectarFire() {
		
			    	FirebaseOptions options;
					try {
						options = FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault())
								.setDatabaseUrl("https://gestaofacil-a6251.firebaseio.com/").build();
						FirebaseApp.initializeApp(options);
						db = FirestoreClient.getFirestore();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
			    
			
			  
			  
	}
	
	public void ler() {
		
		//ABAIXO É PARA LER COM MODIFICAÇÕES
//		DocumentReference docRef = db.collection("users").document("alovelace");
//		docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
//			
//			  @Override
//			  public void onEvent(@Nullable DocumentSnapshot snapshot,
//			                      @Nullable FirestoreException e) {
//			    if (e != null) {
//			      System.err.println("Listen failed: " + e);
//			      return;
//			    }
//
//			    if (snapshot != null && snapshot.exists()) {
//			      System.out.println("Current data: " + snapshot.getData());
//			    } else {
//			      System.out.print("Current data: null");
//			    }
//			  }
//		});
		
		//LER TODOS
		try {
			ApiFuture<QuerySnapshot> query = db.collection("coleta/veiculo/3082021").get();
			// ...
			// query.get() blocks on response
			QuerySnapshot querySnapshot = query.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			for (QueryDocumentSnapshot document : documents) {
				System.out.println(document);
//				System.out.println("User: " + document.getId());
//				System.out.println("First: " + document.getString("first"));
//				if (document.contains("middle")) {
//					System.out.println("Middle: " + document.getString("middle"));
//				}
//				System.out.println("Last: " + document.getString("last"));
//				System.out.println("Born: " + document.getLong("born"));
			}
		} catch (Exception e) {

		}
	}

	public void salvar() {
		DocumentReference docRef = db.collection("coleta").document("alovelace");
		// Add document data with id "alovelace" using a hashmap
		Map<String, Object> data = new HashMap<>();
		data.put("first", "Ada "+new Date());
		data.put("last", "Lovelace ");
		data.put("born ", 1815);
		// asynchronously write data
		ApiFuture<WriteResult> result = docRef.set(data);
		// ...
		// result.get() blocks on response
		try {
			System.out.println("Update time : " + result.get().getUpdateTime());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
