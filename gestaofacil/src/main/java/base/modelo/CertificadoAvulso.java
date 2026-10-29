package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "certificadoavulso")
public class CertificadoAvulso implements Serializable {

	public CertificadoAvulso() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(length = 5000)
	private String texto="";

	@Temporal(TemporalType.DATE)
	private Date dataEmissao = new Date();

	@Temporal(TemporalType.DATE)
	private Date dataInicialFechamento;

	@Temporal(TemporalType.DATE)
	private Date dataFinalFechamento;

	@ManyToOne
	private FechamentoMes fechamentoMes;

	private Boolean status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public Date getDataEmissao() {
		return dataEmissao;
	}

	public void setDataEmissao(Date dataEmissao) {
		this.dataEmissao = dataEmissao;
	}

	public Date getDataInicialFechamento() {
		return dataInicialFechamento;
	}

	public void setDataInicialFechamento(Date dataInicialFechamento) {
		this.dataInicialFechamento = dataInicialFechamento;
	}

	public Date getDataFinalFechamento() {
		return dataFinalFechamento;
	}

	public void setDataFinalFechamento(Date dataFinalFechamento) {
		this.dataFinalFechamento = dataFinalFechamento;
	}


	public FechamentoMes getFechamentoMes() {
		return fechamentoMes;
	}

	public void setFechamentoMes(FechamentoMes fechamentoMes) {
		this.fechamentoMes = fechamentoMes;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

}
