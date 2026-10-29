package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import base.modelo.Pessoa;
import base.modelo.Atividade;
import base.modelo.ColetaApp;
import base.service.AtividadeService;
import base.service.ColetaAppService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("importarFirebaseMB")
public class ImportarFirebaseMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer dia;
	private Integer mes;
	private Integer ano;
	private JsonArray coletas = new JsonArray();
	private String coletasString = "";
	int controle = 0;
	ListenerRegistration registrati=null;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp; // faz as buscas

	@Inject
	private ColetaAppService coletaAppService; // inserir no banco

	@PostConstruct
	public void inicializar() {
		System.out.println("Inicializando Classe Importar");

		FacesContext facesContext = FacesContext.getCurrentInstance();
		facesContext.responseComplete();
		ServletContext scontext = (ServletContext) facesContext.getExternalContext().getContext();

		util.FirebaseInitializer.inicializar(scontext);

		lerDados();
	}

	public void lerDados() {
		Firestore db = null;
		db = FirestoreClient.getFirestore();
		try {
		DocumentReference docRef = db.collection("teste").document("carro");
		if(registrati!=null) {
			System.out.println("Remove ");
		registrati.remove();
		}
		
	 registrati =	docRef.addSnapshotListener(
		    new EventListener<DocumentSnapshot>() {
		      @Override
		      public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirestoreException e) {
		    	  
		        if (e != null) {
		          System.err.println("Listen failed: " + e);
		          return;
		        }

		        if (snapshot != null && snapshot.exists()) {
		          System.out.println("Current data: " + snapshot.getData());
		        } else {
		          System.out.println("Current data: null");
		        }
		      }
		      
		    });
		}catch (Exception e) {
			System.out.println("no cath do try");
			
			// TODO: handle exception
		}

		
	}

	public void importar() {

		coletasString = "";

		Firestore db = null;
		db = FirestoreClient.getFirestore();

		try {
			//ApiFuture<QuerySnapshot> query = db.collection("coleta").document("veiculo")
				//	.collection(dia + "" + mes + "" + ano).get();
			
			ApiFuture<QuerySnapshot> query = db.collection("coleta").document("veiculo")
				.collection(dia + "-" + mes + "-" + ano).get();
			
			System.out.println(dia + "" + mes + "" + ano);
			// ...
			// query.get() blocks on response
			QuerySnapshot querySnapshot = query.get();
			List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
			System.out.println("Quantidade Coletado: " + documents.size() + " - " + dia + "/" + mes + "/" + ano);
			coletas = new JsonArray();
			for (QueryDocumentSnapshot document : documents) {
				List<ColetaApp> ca = daoColetaApp.listar(ColetaApp.class,
						"diaUltimaColeta=" + dia + " and mesUltimaColeta=" + mes + " and anoUltimaColeta=" + ano
								+ " and pontoColetaSelecionado.id=" + Long.parseLong(document.getId()));
				if (ca.size() == 0) {
					String encoded = new Gson().toJson(document.getData());
					//System.out.println(encoded);
					// System.out.println("");
					// coletas.add(new Gson().toJson(document.getData()));
					coletas.add(encoded.toString());
					coletasString = coletasString + ", " + encoded.toString();
				}
				//PROVISORIO
				//String encoded = new Gson().toJson(document.getData());
				//System.out.println(encoded);
				//coletas.add(encoded.toString());
				//coletasString = coletasString + ", " + encoded.toString();
			}
			ExibirMensagem.exibirMensagem("Finalizado!!");
//					System.out.println(coletas);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public JsonArray getColetas() {
		return coletas;
	}

	public void setColetas(JsonArray coletas) {
		this.coletas = coletas;
	}

	public Integer getDia() {
		return dia;
	}

	public void setDia(Integer dia) {
		this.dia = dia;
	}

	public Integer getMes() {
		return mes;
	}

	public void setMes(Integer mes) {
		this.mes = mes;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public String getColetasString() {
		return coletasString;
	}

	public void setColetasString(String coletasString) {
		this.coletasString = coletasString;
	}

}
