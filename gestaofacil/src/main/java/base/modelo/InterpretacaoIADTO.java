package base.modelo;

import java.io.Serializable;

/**
 * Resultado da captura assistida por IA (RF11-15) — extração feita pelo
 * Gemini a partir de um áudio (transcrição) ou foto de nota fiscal (OCR).
 * Não é persistido direto: o app mostra pro motorista revisar/confirmar
 * (RF14) antes de virar um {@link LancamentoVeiculo} de verdade.
 */
public class InterpretacaoIADTO implements Serializable {

	private static final long serialVersionUID = 1L;

	// "Abastecimento", "Despesa" ou "Serviço"
	private String tipo;

	// placa (ou parte dela), ou apelido do veículo mencionado — o app tenta
	// casar com o cadastro existente, igual faz com fornecedor
	private String veiculo;

	// nome do motorista mencionado — mesma lógica de casamento do veículo
	private String motorista;

	// nome da categoria de despesa sugerida (ex.: "Combustível", "Multa")
	private String categoria;

	private Double valor;

	private Double litros;

	private Double valorLitro;

	private Long km;

	private String combustivel;

	// nome do fornecedor/posto/oficina mencionado — o app tenta casar com o
	// cadastro existente; se não achar, mantém como texto solto
	private String fornecedor;

	private String observacao;

	// número do cupom fiscal, nota fiscal ou nota de serviço, se visível no
	// documento — usado pra detectar lançamento duplicado (o mesmo comprovante
	// sendo importado de novo)
	private String numeroDocumento;

	// 0 a 1 — confiança que a IA teve na extração (RF15)
	private Double confianca;

	// transcrição do áudio ou texto lido na imagem, sem edição (RF15)
	private String textoBruto;

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(String veiculo) {
		this.veiculo = veiculo;
	}

	public String getMotorista() {
		return motorista;
	}

	public void setMotorista(String motorista) {
		this.motorista = motorista;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Double getLitros() {
		return litros;
	}

	public void setLitros(Double litros) {
		this.litros = litros;
	}

	public Double getValorLitro() {
		return valorLitro;
	}

	public void setValorLitro(Double valorLitro) {
		this.valorLitro = valorLitro;
	}

	public Long getKm() {
		return km;
	}

	public void setKm(Long km) {
		this.km = km;
	}

	public String getCombustivel() {
		return combustivel;
	}

	public void setCombustivel(String combustivel) {
		this.combustivel = combustivel;
	}

	public String getFornecedor() {
		return fornecedor;
	}

	public void setFornecedor(String fornecedor) {
		this.fornecedor = fornecedor;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public String getNumeroDocumento() {
		return numeroDocumento;
	}

	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}

	public Double getConfianca() {
		return confianca;
	}

	public void setConfianca(Double confianca) {
		this.confianca = confianca;
	}

	public String getTextoBruto() {
		return textoBruto;
	}

	public void setTextoBruto(String textoBruto) {
		this.textoBruto = textoBruto;
	}

}
