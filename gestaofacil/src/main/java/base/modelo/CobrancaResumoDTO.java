package base.modelo;

import java.io.Serializable;

/**
 * Resumo compacto pro topo da tela de Cobrança do appgestaofacil — mesma ideia do card
 * "Resumo da importação" da tela web (base/cobranca.xhtml), só que enxuto pro celular.
 */
public class CobrancaResumoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nomeArquivo;
	private String dataArquivoFormatada;
	private Integer totalClientesEmAberto;
	private String valorTotalEmAbertoFormatado;

	public String getNomeArquivo() {
		return nomeArquivo;
	}

	public void setNomeArquivo(String nomeArquivo) {
		this.nomeArquivo = nomeArquivo;
	}

	public String getDataArquivoFormatada() {
		return dataArquivoFormatada;
	}

	public void setDataArquivoFormatada(String dataArquivoFormatada) {
		this.dataArquivoFormatada = dataArquivoFormatada;
	}

	public Integer getTotalClientesEmAberto() {
		return totalClientesEmAberto;
	}

	public void setTotalClientesEmAberto(Integer totalClientesEmAberto) {
		this.totalClientesEmAberto = totalClientesEmAberto;
	}

	public String getValorTotalEmAbertoFormatado() {
		return valorTotalEmAbertoFormatado;
	}

	public void setValorTotalEmAbertoFormatado(String valorTotalEmAbertoFormatado) {
		this.valorTotalEmAbertoFormatado = valorTotalEmAbertoFormatado;
	}

}
