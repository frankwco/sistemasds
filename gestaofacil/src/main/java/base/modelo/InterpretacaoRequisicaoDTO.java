package base.modelo;

import java.io.Serializable;

/**
 * Corpo dos endpoints de captura por IA do app:
 * <ul>
 * <li>/app/ia/audio, /app/ia/foto — só {@code base64}+{@code mimeType}</li>
 * <li>/app/ia/texto — só {@code texto}</li>
 * <li>/app/ia/fotoTexto — {@code base64}+{@code mimeType}, com {@code texto}
 * opcional complementando a foto</li>
 * </ul>
 */
public class InterpretacaoRequisicaoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String base64;

	private String mimeType;

	// falado/digitado pelo motorista — sozinho (/ia/texto) ou complementando
	// uma foto (/ia/fotoTexto)
	private String texto;

	public String getBase64() {
		return base64;
	}

	public void setBase64(String base64) {
		this.base64 = base64;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

}
