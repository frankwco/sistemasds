package base.modelo;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Alíquotas do Simples Nacional de um mês de competência. Mudam todo mês, por
 * isso não ficam na configuração fixa: assim uma nota nunca sai com o
 * percentual de outro mês sem ninguém perceber.
 */
@Entity
@Table(name = "nfse_aliquota_mensal")
public class NfseAliquotaMensal implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Integer mes;
	private Integer ano;
	/** % aproximado dos tributos do Simples (pTotTribSN). */
	private Double percentualSimples;
	private Boolean status = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getMes() {
		return mes;
	}

	public void setMes(Integer mes) {
		this.mes = mes;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public Double getPercentualSimples() {
		return percentualSimples;
	}

	public void setPercentualSimples(Double percentualSimples) {
		this.percentualSimples = percentualSimples;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public String getMesAno() {
		return String.format("%02d/%d", mes, ano);
	}
}
