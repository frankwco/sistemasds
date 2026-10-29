package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Leitura de odômetro do veículo (RF29). Pode ser avulsa (origem "Manual")
 * ou gerada automaticamente a partir de um {@link LancamentoVeiculo} de
 * Abastecimento/Serviço que já veio com km preenchido — nesse caso
 * {@link #lancamento} aponta para o lançamento que a originou.
 */
@Entity
@Table(name = "leitura_odometro")
public class LeituraOdometro implements Serializable {

	public LeituraOdometro() {
		super();
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "veiculo_id")
	private Veiculo veiculo;

	private Long km;

	@Temporal(TemporalType.DATE)
	private Date data;

	// "Manual", "Abastecimento" ou "Serviço"
	private String origem;

	@ManyToOne
	@JoinColumn(name = "lancamento_id")
	private LancamentoVeiculo lancamento;

	private String criadoPor;

	@Temporal(TemporalType.TIMESTAMP)
	private Date criadoEm;

	private Boolean status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Veiculo getVeiculo() {
		return veiculo;
	}

	public void setVeiculo(Veiculo veiculo) {
		this.veiculo = veiculo;
	}

	public Long getKm() {
		return km;
	}

	public void setKm(Long km) {
		this.km = km;
	}

	public Date getData() {
		return data;
	}

	public void setData(Date data) {
		this.data = data;
	}

	public String getOrigem() {
		return origem;
	}

	public void setOrigem(String origem) {
		this.origem = origem;
	}

	public LancamentoVeiculo getLancamento() {
		return lancamento;
	}

	public void setLancamento(LancamentoVeiculo lancamento) {
		this.lancamento = lancamento;
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

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
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
		LeituraOdometro other = (LeituraOdometro) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
