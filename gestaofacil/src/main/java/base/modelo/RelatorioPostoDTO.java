package base.modelo;

import java.io.Serializable;

/**
 * Rendimento (km/l) e preço médio do litro por posto, com a bandeira do
 * fornecedor (RF21/RF22). O km/l é calculado pela diferença de km entre um
 * abastecimento e o anterior do mesmo veículo, dividida pelos litros do
 * abastecimento atual — não é o km/l do veículo todo, é a "nota" daquele
 * posto especificamente.
 */
public class RelatorioPostoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nome;
	private String bandeira;
	private Integer quantidadeAbastecimentos;
	private Double kmPorLitroMedio;
	private Double precoMedioLitro;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getBandeira() {
		return bandeira;
	}

	public void setBandeira(String bandeira) {
		this.bandeira = bandeira;
	}

	public Integer getQuantidadeAbastecimentos() {
		return quantidadeAbastecimentos;
	}

	public void setQuantidadeAbastecimentos(Integer quantidadeAbastecimentos) {
		this.quantidadeAbastecimentos = quantidadeAbastecimentos;
	}

	public Double getKmPorLitroMedio() {
		return kmPorLitroMedio;
	}

	public void setKmPorLitroMedio(Double kmPorLitroMedio) {
		this.kmPorLitroMedio = kmPorLitroMedio;
	}

	public Double getPrecoMedioLitro() {
		return precoMedioLitro;
	}

	public void setPrecoMedioLitro(Double precoMedioLitro) {
		this.precoMedioLitro = precoMedioLitro;
	}

}
