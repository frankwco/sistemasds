package base.modelo;

import java.io.Serializable;

/**
 * Corpo das requisições de confirmar/desmarcar/registrar envio de WhatsApp de cobrança,
 * feitas pelo appgestaofacil em {@code ServicoApp}.
 */
public class CobrancaWhatsappRequestDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long usuarioId;
	private Long clienteId;
	// 1 ou 2, indicando telefone1 ou telefone2 do cliente
	private Integer numeroTelefone;

	public Long getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(Long usuarioId) {
		this.usuarioId = usuarioId;
	}

	public Long getClienteId() {
		return clienteId;
	}

	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}

	public Integer getNumeroTelefone() {
		return numeroTelefone;
	}

	public void setNumeroTelefone(Integer numeroTelefone) {
		this.numeroTelefone = numeroTelefone;
	}

}
