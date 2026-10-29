package base.modelo;

import java.util.Date;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;

public class PontoColetaRetorno {

	public PontoColetaRetorno() {
		super();
	}

	private Long id;

	private Boolean acessoSistema = false;
	private String apelido = "";
	private Boolean status;
	private String razaoSocialCliente = "";
	private String nomeRazaoSocial;
	private String nomeFantasia;
	private String cep;
	private String endereco;
	private Long idCidade;
	private String nomeCidade;
	private String email = "";
	private String nomeRota;
	private String gruposColetados = "";
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataUltimaColeta;
	private Integer diaUltimaColeta = 0;
	private Integer mesUltimaColeta = 0;
	private Integer anoUltimaColeta = 0;
	private Integer horaUltimaColeta = 0;
	private Integer minutoUltimaColeta = 0;
	private Integer segundoUltimaColeta = 0;
	private Integer quantidadeColetaMes = 0;
	private Integer quantidadeColetadaMes = 0;
	private String nomeRazaoSocialNomeFantasiaEndereco = "";
	private String periodicidadeColeta = "";
	private String aviso = "";
	private Boolean situacaoAviso = false;
	private String telefone1;
	private String telefone2;
	private String telefone1Cliente;
	private String telefone2Cliente;
	private String enderecoCompleto="";
	private Boolean primeiraColeta=true;
	
	
	

	public Boolean getPrimeiraColeta() {
		return primeiraColeta;
	}

	public void setPrimeiraColeta(Boolean primeiraColeta) {
		this.primeiraColeta = primeiraColeta;
	}

	public String getEnderecoCompleto() {
		return enderecoCompleto;
	}

	public void setEnderecoCompleto(String enderecoCompleto) {
		this.enderecoCompleto = enderecoCompleto;
	}

	public String getTelefone1Cliente() {
		return telefone1Cliente;
	}

	public void setTelefone1Cliente(String telefone1Cliente) {
		this.telefone1Cliente = telefone1Cliente;
	}

	public String getTelefone2Cliente() {
		return telefone2Cliente;
	}

	public void setTelefone2Cliente(String telefone2Cliente) {
		this.telefone2Cliente = telefone2Cliente;
	}

	public String getTelefone1() {		
		return telefone1;
	}

	public void setTelefone1(String telefone1) {
		this.telefone1 = telefone1;
	}

	public String getTelefone2() {	
		return telefone2;
	}

	public void setTelefone2(String telefone2) {
		this.telefone2 = telefone2;
	}

	public Boolean getSituacaoAviso() {
		if (situacaoAviso == null) {
			situacaoAviso = false;
		}
		return situacaoAviso;
	}

	public void setSituacaoAviso(Boolean situacaoAviso) {
		this.situacaoAviso = situacaoAviso;
	}

	public String getAviso() {
		return aviso;
	}

	public void setAviso(String aviso) {
		this.aviso = aviso;
	}

	public String getPeriodicidadeColeta() {
		return periodicidadeColeta;
	}

	public void setPeriodicidadeColeta(String periodicidadeColeta) {
		this.periodicidadeColeta = periodicidadeColeta;
	}

	public String getNomeRazaoSocialNomeFantasiaEndereco() {
		return nomeRazaoSocialNomeFantasiaEndereco;
	}

	public void setNomeRazaoSocialNomeFantasiaEndereco(String nomeRazaoSocialNomeFantasiaEndereco) {
		this.nomeRazaoSocialNomeFantasiaEndereco = nomeRazaoSocialNomeFantasiaEndereco;
	}

	public Integer getQuantidadeColetaMes() {
		return quantidadeColetaMes;
	}

	public void setQuantidadeColetaMes(Integer quantidadeColetaMes) {
		this.quantidadeColetaMes = quantidadeColetaMes;
	}

	public Integer getQuantidadeColetadaMes() {
		return quantidadeColetadaMes;
	}

	public void setQuantidadeColetadaMes(Integer quantidadeColetadaMes) {
		this.quantidadeColetadaMes = quantidadeColetadaMes;
	}

	public String getApelido() {
		if (apelido == null) {
			apelido = "";
		}
		return apelido;
	}

	public void setApelido(String apelido) {
		this.apelido = apelido;
	}

	public Integer getHoraUltimaColeta() {
		if (horaUltimaColeta == null) {
			horaUltimaColeta = 0;
		}
		return horaUltimaColeta;
	}

	public void setHoraUltimaColeta(Integer horaUltimaColeta) {
		this.horaUltimaColeta = horaUltimaColeta;
	}

	public Integer getMinutoUltimaColeta() {
		if (minutoUltimaColeta == null) {
			minutoUltimaColeta = 0;
		}
		return minutoUltimaColeta;
	}

	public void setMinutoUltimaColeta(Integer minutoUltimaColeta) {
		this.minutoUltimaColeta = minutoUltimaColeta;
	}

	public Integer getSegundoUltimaColeta() {
		if (segundoUltimaColeta == null) {
			segundoUltimaColeta = 0;
		}
		return segundoUltimaColeta;
	}

	public void setSegundoUltimaColeta(Integer segundoUltimaColeta) {
		this.segundoUltimaColeta = segundoUltimaColeta;
	}

	public Integer getDiaUltimaColeta() {
		if (diaUltimaColeta == null) {
			diaUltimaColeta = 0;
		}
		return diaUltimaColeta;
	}

	public void setDiaUltimaColeta(Integer diaUltimaColeta) {
		this.diaUltimaColeta = diaUltimaColeta;
	}

	public Integer getMesUltimaColeta() {
		if (mesUltimaColeta == null) {
			mesUltimaColeta = 0;
		}
		return mesUltimaColeta;
	}

	public void setMesUltimaColeta(Integer mesUltimaColeta) {
		this.mesUltimaColeta = mesUltimaColeta;
	}

	public Integer getAnoUltimaColeta() {
		if (anoUltimaColeta == null) {
			anoUltimaColeta = 0;
		}
		return anoUltimaColeta;
	}

	public void setAnoUltimaColeta(Integer anoUltimaColeta) {
		this.anoUltimaColeta = anoUltimaColeta;
	}

	public Date getDataUltimaColeta() {
		return dataUltimaColeta;
	}

	public void setDataUltimaColeta(Date dataUltimaColeta) {
		this.dataUltimaColeta = dataUltimaColeta;
	}

	public String getGruposColetados() {
		if (gruposColetados == null) {
			gruposColetados = "";
		}
		return gruposColetados;
	}

	public void setGruposColetados(String gruposColetados) {
		this.gruposColetados = gruposColetados;
	}

	public String getNomeRota() {
		return nomeRota;
	}

	public void setNomeRota(String nomeRota) {
		this.nomeRota = nomeRota;
	}

	public String getRazaoSocialCliente() {
		return razaoSocialCliente;
	}

	public void setRazaoSocialCliente(String razaoSocialCliente) {
		this.razaoSocialCliente = razaoSocialCliente;
	}

	public Boolean getAcessoSistema() {
		return acessoSistema;
	}

	public void setAcessoSistema(Boolean acessoSistema) {
		this.acessoSistema = acessoSistema;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public Long getIdCidade() {
		return idCidade;
	}

	public void setIdCidade(Long idCidade) {
		this.idCidade = idCidade;
	}

	public String getNomeCidade() {
		return nomeCidade;
	}

	public void setNomeCidade(String nomeCidade) {
		this.nomeCidade = nomeCidade;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
