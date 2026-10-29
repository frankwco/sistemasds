package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * Lançamento financeiro de um veículo. Cobre 3 fluxos (RF06/RF07/RF08) numa
 * única tabela — "Despesa", "Abastecimento" ou "Serviço" no campo
 * {@link #tipo} — porque os relatórios (RF19/20/24/25/26/27) e o extrato
 * unificado (RF30) precisam agregar os três o tempo todo; assim viram um
 * SELECT direto em vez de UNION em três tabelas. As telas continuam
 * separadas (Abastecimento / Despesas e Manutenção).
 */
@Entity
@Table(name = "lancamento_veiculo")
public class LancamentoVeiculo implements Serializable {

	public LancamentoVeiculo() {
		super();
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	// "Despesa", "Abastecimento" ou "Serviço"
	private String tipo;

	@ManyToOne
	@JoinColumn(name = "veiculo_id")
	private Veiculo veiculo;

	@ManyToOne
	@JoinColumn(name = "motorista_id")
	private Usuario motorista;

	@ManyToOne
	@JoinColumn(name = "categoria_despesa_id")
	private CategoriaDespesa categoriaDespesa;

	@ManyToOne
	@JoinColumn(name = "fornecedor_id")
	private Fornecedor fornecedor;

	@Temporal(TemporalType.DATE)
	private Date data;

	private Double valor;

	// km do odômetro no momento do lançamento (abastecimento/serviço)
	private Long km;

	private String observacao;

	// número do cupom fiscal, nota fiscal ou nota de serviço (quando veio de
	// captura por IA e o documento anexado tinha esse número visível) — usado
	// pra detectar lançamento duplicado
	private String numeroDocumento;

	// não persiste — só um "recado" do app mobile pro backend: nome de
	// fornecedor que a IA leu no comprovante mas não bateu com nenhum já
	// sincronizado no aparelho. Quando chega assim (fornecedor null +
	// fornecedorSugerido preenchido), o backend cadastra automaticamente na
	// hora de sincronizar (ver ServicoApp#cadastrarLancamentos)
	@Transient
	private String fornecedorSugerido;

	// campos específicos de Abastecimento
	private Double litros;

	private Double valorLitro;

	private String combustivel;

	// campos específicos de Serviço
	// "Preventiva" ou "Corretiva"
	private String tipoManutencao;

	@Temporal(TemporalType.DATE)
	private Date dataEntrada;

	@Temporal(TemporalType.DATE)
	private Date dataSaida;

	private Long kmProximaRevisao;

	private Boolean status;

	private String criadoPor;

	@Temporal(TemporalType.TIMESTAMP)
	private Date criadoEm;

	private String alteradoPor;

	@Temporal(TemporalType.TIMESTAMP)
	private Date alteradoEm;

	// "Manual" (padrão), "IA_Voz" ou "IA_Foto" (RNF02/RF15)
	private String origemLancamento;

	// 0 a 1 — confiança que a IA teve na extração (RF15), só quando origemLancamento começa com "IA_"
	private Double confiancaIA;

	// transcrição do áudio ou texto lido na foto, bruto, sem edição do usuário (RF15)
	@Lob
	private String textoBrutoIA;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public Usuario getMotorista() {
		return motorista;
	}

	public void setMotorista(Usuario motorista) {
		this.motorista = motorista;
	}

	public CategoriaDespesa getCategoriaDespesa() {
		return categoriaDespesa;
	}

	public void setCategoriaDespesa(CategoriaDespesa categoriaDespesa) {
		this.categoriaDespesa = categoriaDespesa;
	}

	public Fornecedor getFornecedor() {
		return fornecedor;
	}

	public void setFornecedor(Fornecedor fornecedor) {
		this.fornecedor = fornecedor;
	}

	public Date getData() {
		return data;
	}

	public void setData(Date data) {
		this.data = data;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Long getKm() {
		return km;
	}

	public void setKm(Long km) {
		this.km = km;
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

	public String getFornecedorSugerido() {
		return fornecedorSugerido;
	}

	public void setFornecedorSugerido(String fornecedorSugerido) {
		this.fornecedorSugerido = fornecedorSugerido;
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

	public String getCombustivel() {
		return combustivel;
	}

	public void setCombustivel(String combustivel) {
		this.combustivel = combustivel;
	}

	public String getTipoManutencao() {
		return tipoManutencao;
	}

	public void setTipoManutencao(String tipoManutencao) {
		this.tipoManutencao = tipoManutencao;
	}

	public Date getDataEntrada() {
		return dataEntrada;
	}

	public void setDataEntrada(Date dataEntrada) {
		this.dataEntrada = dataEntrada;
	}

	public Date getDataSaida() {
		return dataSaida;
	}

	public void setDataSaida(Date dataSaida) {
		this.dataSaida = dataSaida;
	}

	public Long getKmProximaRevisao() {
		return kmProximaRevisao;
	}

	public void setKmProximaRevisao(Long kmProximaRevisao) {
		this.kmProximaRevisao = kmProximaRevisao;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public String getCriadoPor() {
		return criadoPor;
	}

	public void setCriadoPor(String criadoPor) {
		this.criadoPor = criadoPor;
	}

	public Date getCriadoEm() {
		return criadoEm;
	}

	public void setCriadoEm(Date criadoEm) {
		this.criadoEm = criadoEm;
	}

	public String getAlteradoPor() {
		return alteradoPor;
	}

	public void setAlteradoPor(String alteradoPor) {
		this.alteradoPor = alteradoPor;
	}

	public Date getAlteradoEm() {
		return alteradoEm;
	}

	public void setAlteradoEm(Date alteradoEm) {
		this.alteradoEm = alteradoEm;
	}

	public String getOrigemLancamento() {
		return origemLancamento;
	}

	public void setOrigemLancamento(String origemLancamento) {
		this.origemLancamento = origemLancamento;
	}

	public Double getConfiancaIA() {
		return confiancaIA;
	}

	public void setConfiancaIA(Double confiancaIA) {
		this.confiancaIA = confiancaIA;
	}

	public String getTextoBrutoIA() {
		return textoBrutoIA;
	}

	public void setTextoBrutoIA(String textoBrutoIA) {
		this.textoBrutoIA = textoBrutoIA;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LancamentoVeiculo other = (LancamentoVeiculo) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
