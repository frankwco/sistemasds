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
 * Lembrete avulso (RF31) — alvo por data OU por km (ex.: "trocar óleo em
 * 5.000 km", "verificar extintor em 30 dias"). Motorista é opcional.
 * {@link #concluido} marca RF33; a geração automática do serviço/despesa ao
 * concluir fica em {@code LembreteService.concluir}, não aqui.
 */
@Entity
@Table(name = "lembrete")
public class Lembrete implements Serializable {

	public Lembrete() {
		super();
	}

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String descricao;

	@ManyToOne
	@JoinColumn(name = "veiculo_id")
	private Veiculo veiculo;

	@ManyToOne
	@JoinColumn(name = "motorista_id")
	private Usuario motorista;

	// "Data" ou "Km"
	private String alvoTipo;

	@Temporal(TemporalType.DATE)
	private Date dataAlvo;

	private Long kmAlvo;

	private Boolean concluido;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataConclusao;

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

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
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

	public String getAlvoTipo() {
		return alvoTipo;
	}

	public void setAlvoTipo(String alvoTipo) {
		this.alvoTipo = alvoTipo;
	}

	public Date getDataAlvo() {
		return dataAlvo;
	}

	public void setDataAlvo(Date dataAlvo) {
		this.dataAlvo = dataAlvo;
	}

	public Long getKmAlvo() {
		return kmAlvo;
	}

	public void setKmAlvo(Long kmAlvo) {
		this.kmAlvo = kmAlvo;
	}

	public Boolean getConcluido() {
		if (concluido == null) {
			concluido = false;
		}
		return concluido;
	}

	public void setConcluido(Boolean concluido) {
		this.concluido = concluido;
	}

	public Date getDataConclusao() {
		return dataConclusao;
	}

	public void setDataConclusao(Date dataConclusao) {
		this.dataConclusao = dataConclusao;
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
		Lembrete other = (Lembrete) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
