package util;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.cloud.storage.Blob;
import com.google.firebase.cloud.StorageClient;

/**
 * Upload/leitura de anexos (RF09) no Firebase Storage, organizados por
 * ano/mês ("anexos/{ano}/{mes}/{lancamentoId}/{uuid}-{nomeArquivo}") para
 * permitir expurgo futuro por período. Só o caminho fica no MySQL
 * ({@link base.modelo.Anexo}); o binário fica só no Storage.
 */
public class FirebaseStorageUtil {

	private FirebaseStorageUtil() {
	}

	public static String montarCaminho(Long lancamentoId, String nomeArquivoOriginal) {
		Date agora = new Date();
		String ano = new SimpleDateFormat("yyyy").format(agora);
		String mes = new SimpleDateFormat("MM").format(agora);
		String nomeSanitizado = nomeArquivoOriginal == null ? "arquivo"
				: nomeArquivoOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
		return "anexos/" + ano + "/" + mes + "/" + lancamentoId + "/" + UUID.randomUUID() + "-" + nomeSanitizado;
	}

	public static String upload(byte[] conteudo, String nomeArquivoOriginal, String contentType, Long lancamentoId)
			throws Exception {
		String caminho = montarCaminho(lancamentoId, nomeArquivoOriginal);
		StorageClient.getInstance().bucket().create(caminho, new ByteArrayInputStream(conteudo), contentType);
		return caminho;
	}

	public static byte[] baixar(String caminho) {
		Blob blob = StorageClient.getInstance().bucket().get(caminho);
		return blob == null ? null : blob.getContent();
	}

	public static String gerarUrlAssinada(String caminho, int minutos) {
		Blob blob = StorageClient.getInstance().bucket().get(caminho);
		if (blob == null) {
			return null;
		}
		return blob.signUrl(minutos, TimeUnit.MINUTES).toString();
	}

	public static void excluir(String caminho) {
		Blob blob = StorageClient.getInstance().bucket().get(caminho);
		if (blob != null) {
			blob.delete();
		}
	}

}
