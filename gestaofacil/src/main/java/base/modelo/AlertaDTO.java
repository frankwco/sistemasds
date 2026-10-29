package base.modelo;

import java.io.Serializable;
import java.util.Date;

/**
 * Item da central de alertas unificada (RF34): agrega documentos de
 * veículo/motorista a vencer (RF18), manutenção programada por km e
 * lembretes avulsos (RF32) — não é entidade, é só a resposta agregada de
 * {@code AlertaService.listarAlertas()}.
 */
public class AlertaDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	// "Documento Veículo", "Documento Motorista", "Lembrete" ou "Manutenção Programada"
	private String tipo;

	private String descricao;

	private String veiculo;

	private String motorista;

	private Date dataVencimento;

	private Long kmAlvo;

	private Long kmAtual;

	// "Vencido" ou "Próximo"
	private String urgencia;

	// ids da entidade de origem — usados só pela resolução rápida da Central de Alertas
	private Long documentoVeiculoId;
	private Long documentoMotoristaId;
	private Long lembreteId;
	private Long veiculoId;
	private Long lancamentoVeiculoId;

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
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

	public Date getDataVencimento() {
		return dataVencimento;
	}

	public void setDataVencimento(Date dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public Long getKmAlvo() {
		return kmAlvo;
	}

	public void setKmAlvo(Long kmAlvo) {
		this.kmAlvo = kmAlvo;
	}

	public Long getKmAtual() {
		return kmAtual;
	}

	public void setKmAtual(Long kmAtual) {
		this.kmAtual = kmAtual;
	}

	public String getUrgencia() {
		return urgencia;
	}

	public void setUrgencia(String urgencia) {
		this.urgencia = urgencia;
	}

	public Long getDocumentoVeiculoId() {
		return documentoVeiculoId;
	}

	public void setDocumentoVeiculoId(Long documentoVeiculoId) {
		this.documentoVeiculoId = documentoVeiculoId;
	}

	public Long getDocumentoMotoristaId() {
		return documentoMotoristaId;
	}

	public void setDocumentoMotoristaId(Long documentoMotoristaId) {
		this.documentoMotoristaId = documentoMotoristaId;
	}

	public Long getLembreteId() {
		return lembreteId;
	}

	public void setLembreteId(Long lembreteId) {
		this.lembreteId = lembreteId;
	}

	public Long getVeiculoId() {
		return veiculoId;
	}

	public void setVeiculoId(Long veiculoId) {
		this.veiculoId = veiculoId;
	}

	public Long getLancamentoVeiculoId() {
		return lancamentoVeiculoId;
	}

	public void setLancamentoVeiculoId(Long lancamentoVeiculoId) {
		this.lancamentoVeiculoId = lancamentoVeiculoId;
	}

}
