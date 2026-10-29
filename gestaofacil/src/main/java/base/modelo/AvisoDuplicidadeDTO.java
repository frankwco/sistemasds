package base.modelo;

import java.io.Serializable;

/** Resposta de GET /app/verificarDuplicidade — {@code aviso} vem null quando não achou nada suspeito. */
public class AvisoDuplicidadeDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String aviso;

	public AvisoDuplicidadeDTO() {
	}

	public AvisoDuplicidadeDTO(String aviso) {
		this.aviso = aviso;
	}

	public String getAviso() {
		return aviso;
	}

	public void setAviso(String aviso) {
		this.aviso = aviso;
	}

}
