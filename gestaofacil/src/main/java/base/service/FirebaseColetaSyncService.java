package base.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;

import base.modelo.ColetaApp;
import base.modelo.LogTarefas;
import dao.GenericDAO;
import services.ServicoHello;

/**
 * Consome as coletas gravadas no Firestore pelo appgestaofacil e garante que
 * também existam (inserção) ou tenham sido removidas (remoção) no banco
 * relacional do gestaofacil.
 *
 * Reaproveita a MESMA lógica de negócio já usada pelo envio direto via REST
 * (ServicoHello.cadastroColeta) — em vez de duplicar as regras de e-mail,
 * atualização financeira e checagem de duplicidade, este serviço apenas
 * "reinjeta" cada documento do Firestore como se fosse uma chamada normal
 * do app àquele mesmo método.
 *
 * Fase "modo sombra" da migração combinada: roda em paralelo ao envio
 * direto via REST do app, sem substituí-lo. Ele automatiza o que hoje é
 * feito manualmente em base/importarFirebase.jsf (que só cobre inserções e
 * exige copiar/colar JSON no Postman) e cobre também remoções, que hoje não
 * têm nenhuma forma de reconciliação.
 */
public class FirebaseColetaSyncService {

	/**
	 * Quantos dias para trás verificar a cada execução — cobre coletas feitas
	 * offline e sincronizadas ao Firebase com atraso.
	 */
	private static final int JANELA_DIAS = 5;

	/** Tempo máximo de espera por cada consulta ao Firestore (1 por dia da janela). */
	private static final int TIMEOUT_FIRESTORE_SEGUNDOS = 15;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp;

	// Instance<T> em vez de @Inject direto: ServicoHello também injeta este
	// serviço (para expor /consumirFirebase), o que forma um ciclo entre dois
	// beans de escopo @Dependent — proibido pelo Weld (WELD-001443) e faz o
	// deploy inteiro falhar. Instance<T> resolve sob demanda, em tempo de
	// execução, e quebra o ciclo estático que o validador do CDI enxerga.
	@Inject
	private Instance<ServicoHello> servicoHelloInstance;

	@Inject
	private LogTarefasService logTarefasService;

	public static class Resultado {
		public int documentosVerificados = 0;
		public int candidatosInsercao = 0;
		public int inseridosConfirmados = 0;
		public int candidatosRemocao = 0;
		public int removidosConfirmados = 0;
		public int errosLeitura = 0;
	}

	/**
	 * Pressupõe que o Firebase Admin SDK já foi inicializado na subida da
	 * aplicação (ver {@link util.FirebaseSyncSchedulerListener}).
	 */
	public Resultado sincronizar() {
		Resultado resultado = new Resultado();
		Firestore db = FirestoreClient.getFirestore();

		Calendar cal = Calendar.getInstance();
		for (int i = 0; i < JANELA_DIAS; i++) {
			int dia = cal.get(Calendar.DAY_OF_MONTH);
			int mes = cal.get(Calendar.MONTH) + 1;
			int ano = cal.get(Calendar.YEAR);

			processarDia(db, dia, mes, ano, resultado);

			cal.add(Calendar.DATE, -1);
		}

		if (resultado.candidatosInsercao > 0 || resultado.candidatosRemocao > 0) {
			registrarLog(resultado);
		}

		return resultado;
	}

	private void processarDia(Firestore db, int dia, int mes, int ano, Resultado resultado) {
		try {
			CollectionReference colecao = db.collection("coleta").document("veiculo")
					.collection(dia + "-" + mes + "-" + ano);

			// Timeout explícito: sem isso, uma consulta presa (rede/firewall
			// bloqueando o acesso ao Firestore, etc.) trava o método inteiro sem
			// limite de tempo, e só aparece como "Read timed out" lá na chamada
			// HTTP do agendador — sem indicar qual foi o problema real.
			QuerySnapshot snapshot = colecao.get().get(TIMEOUT_FIRESTORE_SEGUNDOS, TimeUnit.SECONDS);

			for (QueryDocumentSnapshot doc : snapshot.getDocuments()) {
				resultado.documentosVerificados++;
				try {
					processarDocumento(doc, resultado);
				} catch (Exception e) {
					resultado.errosLeitura++;
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// Dia sem nenhuma coleta ainda dá QuerySnapshot vazio (não cai aqui) —
			// chegar neste catch é falha real de acesso ao Firestore (timeout,
			// rede, credencial), então vale deixar visível no log do servidor.
			resultado.errosLeitura++;
			System.err.println("[FirebaseSync] Falha ao consultar Firestore para " + dia + "-" + mes + "-" + ano
					+ ": " + e);
		}
	}

	private void processarDocumento(QueryDocumentSnapshot doc, Resultado resultado) {
		String identificadorApp = doc.getString("identificadorApp");
		if (identificadorApp == null || identificadorApp.trim().isEmpty()) {
			return;
		}
		identificadorApp = identificadorApp.trim();

		Boolean status = doc.getBoolean("status");
		boolean ativoNoFirebase = status != null && status;

		boolean existeNoBanco = existeNoBanco(identificadorApp);

		if (ativoNoFirebase && !existeNoBanco) {
			resultado.candidatosInsercao++;
			enviarParaCadastro(doc);
			if (existeNoBanco(identificadorApp)) {
				resultado.inseridosConfirmados++;
			}
		} else if (!ativoNoFirebase && existeNoBanco) {
			resultado.candidatosRemocao++;
			enviarParaCadastro(doc);
			if (!existeNoBanco(identificadorApp)) {
				resultado.removidosConfirmados++;
			}
		}
	}

	private boolean existeNoBanco(String identificadorApp) {
		List<ColetaApp> existentes = daoColetaApp.listar(ColetaApp.class,
				"identificadorApp='" + identificadorApp + "'");
		return !existentes.isEmpty();
	}

	/**
	 * Envia o documento do Firestore para o MESMO método que o app já usa ao
	 * sincronizar via REST — garante tratamento de negócio idêntico (e-mail,
	 * dados financeiros, checagem de duplicidade) nos dois caminhos.
	 */
	private void enviarParaCadastro(QueryDocumentSnapshot doc) {
		Gson gson = new Gson();
		String json = gson.toJson(doc.getData());
		ColetaApp coletaApp = gson.fromJson(json, ColetaApp.class);

		// cadastroColeta acessa pontoColetaSelecionado.getId() e veiculo.getId()
		// sem checagem de nulo. Documentos antigos/de teste no Firestore que não
		// têm esse formato (de antes da regra atual, ou criados manualmente no
		// console do Firebase) derrubavam a sincronização inteira com NPE a
		// cada ciclo. Melhor pular e registrar do que travar tudo.
		if (coletaApp == null || coletaApp.getPontoColetaSelecionado() == null
				|| coletaApp.getPontoColetaSelecionado().getId() == null || coletaApp.getVeiculo() == null
				|| coletaApp.getVeiculo().getId() == null) {
			System.err.println("[FirebaseSync] Documento " + doc.getId()
					+ " ignorado: faltam pontoColetaSelecionado.id ou veiculo.id (dado antigo/incompleto?)");
			return;
		}

		servicoHelloInstance.get().cadastroColeta(Collections.singletonList(coletaApp));
	}

	private void registrarLog(Resultado resultado) {
		LogTarefas log = new LogTarefas();
		log.setTipo("SYNC FIREBASE AUTOMATICO");
		log.setDataRegistro(new Date());
		log.setDescricaoCurta(resultado.inseridosConfirmados + " inserido(s), " + resultado.removidosConfirmados
				+ " removido(s)");
		log.setDescricao("Verificados: " + resultado.documentosVerificados + " | Candidatos inserção: "
				+ resultado.candidatosInsercao + " (confirmados: " + resultado.inseridosConfirmados
				+ ") | Candidatos remoção: " + resultado.candidatosRemocao + " (confirmados: "
				+ resultado.removidosConfirmados + ") | Erros de leitura: " + resultado.errosLeitura);
		log.setStatus(true);
		log.setErro((resultado.candidatosInsercao - resultado.inseridosConfirmados) > 0
				|| (resultado.candidatosRemocao - resultado.removidosConfirmados) > 0 || resultado.errosLeitura > 0);
		// logTarefasService.inserirAlterar(log);
	}
}
