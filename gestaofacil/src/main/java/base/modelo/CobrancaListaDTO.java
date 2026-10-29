package base.modelo;

import java.io.Serializable;
import java.util.List;

/** Resposta de GET /app/cobrancas: o resumo do topo + a lista de clientes em atraso. */
public class CobrancaListaDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private CobrancaResumoDTO resumo;
	private List<CobrancaClienteDTO> clientes;

	public CobrancaResumoDTO getResumo() {
		return resumo;
	}

	public void setResumo(CobrancaResumoDTO resumo) {
		this.resumo = resumo;
	}

	public List<CobrancaClienteDTO> getClientes() {
		return clientes;
	}

	public void setClientes(List<CobrancaClienteDTO> clientes) {
		this.clientes = clientes;
	}

}
