package base.modelo;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ColetaAppPontosSemColeta {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	
	public ColetaAppPontosSemColeta() {
		super();
	}
	
	
	
	private String nomeRazaoSocial;
	private String nomeFantasia;
	private String endereco;
	private String numero;
	private String bairro;
	//private String nome;
	private String sigla;
	//private Long id;
	//private Boolean vigente;
	private Date dataUltimaColeta;
	private String nomeCidade;
	private Long idCidade;
	private int diasSemColeta;
	
	

	
	public String getNomeCidade() {
		return nomeCidade;
	}
	public void setNomeCidade(String nomeCidade) {
		this.nomeCidade = nomeCidade;
	}
	public Long getIdCidade() {
		return idCidade;
	}
	public void setIdCidade(Long idCidade) {
		this.idCidade = idCidade;
	}
	public int getDiasSemColeta() {
		return diasSemColeta;
	}
	public void setDiasSemColeta(int diasSemColeta) {
		this.diasSemColeta = diasSemColeta;
	}
	public Date getDataUltimaColeta() {
		return dataUltimaColeta;
	}
	public void setDataUltimaColeta(Date dataUltimaColeta) {
		this.dataUltimaColeta = dataUltimaColeta;
	}
	public String getNomeRazaoSocial() {
		return nomeRazaoSocial;
	}
	public void setNomeRazaoSocial(String nomeRazaoSocial) {
		this.nomeRazaoSocial = nomeRazaoSocial;
	}
	public String getNomeFantasia() {
		return nomeFantasia;
	}
	public void setNomeFantasia(String nomeFantasia) {
		this.nomeFantasia = nomeFantasia;
	}
	public String getEndereco() {
		return endereco;
	}
	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}
	public String getNumero() {
		return numero;
	}
	public void setNumero(String numero) {
		this.numero = numero;
	}
	public String getBairro() {
		return bairro;
	}
	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getSigla() {
		return sigla;
	}
	public void setSigla(String sigla) {
		this.sigla = sigla;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	
	

}
