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
import javax.persistence.Transient;

@Entity
@Table(name = "avaliacao_nps")
public class AvaliacaoNPS implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Integer nota;

	@Column(length = 2000)
	private String comentario;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataAvaliacao = new Date();

	@ManyToOne
	private ColetaApp coletaApp;

	private Boolean status = true;

	// ── Helpers (não persistidos) ─────────────────────────────────────────────

	@Transient
	public String getNotaCategoria() {
		if (nota == null) return "";
		if (nota >= 9) return "Promotor";
		if (nota >= 7) return "Passivo";
		return "Detrator";
	}

	@Transient
	public String getNotaCategoriaCor() {
		if (nota == null) return "#9e9e9e";
		if (nota >= 9) return "#2e7d32";
		if (nota >= 7) return "#f57f17";
		return "#c62828";
	}

	// ── Getters & Setters ─────────────────────────────────────────────────────

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Integer getNota() { return nota; }
	public void setNota(Integer nota) { this.nota = nota; }

	public String getComentario() { return comentario; }
	public void setComentario(String comentario) { this.comentario = comentario; }

	public Date getDataAvaliacao() { return dataAvaliacao; }
	public void setDataAvaliacao(Date dataAvaliacao) { this.dataAvaliacao = dataAvaliacao; }

	public ColetaApp getColetaApp() { return coletaApp; }
	public void setColetaApp(ColetaApp coletaApp) { this.coletaApp = coletaApp; }

	public Boolean getStatus() { return status; }
	public void setStatus(Boolean status) { this.status = status; }
}
