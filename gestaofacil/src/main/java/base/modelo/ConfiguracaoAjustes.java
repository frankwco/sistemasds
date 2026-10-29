package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "configuracao_ajuste")
public class ConfiguracaoAjustes implements Serializable {

	public ConfiguracaoAjustes() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String descricao;

	private Double porcentagemValorFixo = 0.;

	private Double porcentagemValorQuilo = 0.;

	private Double porcentagemExcedente = 0.;

	private Double porcentagemQuiloGrupoParte = 0.;

	@Temporal(TemporalType.DATE)
	private Date dataCadastro = new Date();
	
	@Temporal(TemporalType.DATE)
	private Date dataInativacao;

	private String situacao = "Ativo";// ativo ou inativo

	private Boolean status;
	
	

	public Date getDataInativacao() {
		return dataInativacao;
	}

	public void setDataInativacao(Date dataInativacao) {
		this.dataInativacao = dataInativacao;
	}

	public String getSituacao() {
		return situacao;
	}

	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}

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

	public Double getPorcentagemValorFixo() {
		return porcentagemValorFixo;
	}

	public void setPorcentagemValorFixo(Double porcentagemValorFixo) {
		this.porcentagemValorFixo = porcentagemValorFixo;
	}

	public Double getPorcentagemValorQuilo() {
		return porcentagemValorQuilo;
	}

	public void setPorcentagemValorQuilo(Double porcentagemValorQuilo) {
		this.porcentagemValorQuilo = porcentagemValorQuilo;
	}

	public Double getPorcentagemExcedente() {
		return porcentagemExcedente;
	}

	public void setPorcentagemExcedente(Double porcentagemExcedente) {
		this.porcentagemExcedente = porcentagemExcedente;
	}

	public Double getPorcentagemQuiloGrupoParte() {
		return porcentagemQuiloGrupoParte;
	}

	public void setPorcentagemQuiloGrupoParte(Double porcentagemQuiloGrupoParte) {
		this.porcentagemQuiloGrupoParte = porcentagemQuiloGrupoParte;
	}

	public Date getDataCadastro() {
		return dataCadastro;
	}

	public void setDataCadastro(Date dataCadastro) {
		this.dataCadastro = dataCadastro;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

}
