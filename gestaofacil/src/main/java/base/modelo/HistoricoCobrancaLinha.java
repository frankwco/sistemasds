package base.modelo;

import java.io.Serializable;

/** Uma linha do histórico de cobrança de um cliente, já separada em canal + texto. */
public class HistoricoCobrancaLinha implements Serializable {

	private static final long serialVersionUID = 1L;

	private String canal;
	private String texto;

	// Construtor sem argumentos e setters exigidos pelo JAXB/MOXy (provedor JSON do Jersey)
	// pra poder serializar esta classe — mesmo só escrevendo, nunca lendo de volta: sem os
	// dois, o MOXy ou lança erro na hora de montar o contexto, ou simplesmente ignora as
	// propriedades e manda um objeto vazio.
	public HistoricoCobrancaLinha() {
	}

	public HistoricoCobrancaLinha(String canal, String texto) {
		this.canal = canal;
		this.texto = texto;
	}

	public String getCanal() {
		return canal;
	}

	public void setCanal(String canal) {
		this.canal = canal;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public boolean isWhatsapp() {
		return "WHATSAPP".equals(canal);
	}

}
