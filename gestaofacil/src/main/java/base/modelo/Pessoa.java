package base.modelo;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "pessoa")
@Inheritance(strategy = InheritanceType.JOINED)
public class Pessoa implements Serializable {

	public Pessoa() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String observacao;

	private String nomeRazaoSocial;
	private String nomeFantasia;
	private String cpfCnpj;
	private String rgInscricaoEstadual;
	private Integer diaVencimentoFatura = 10;
	private String cep;
	private String tipo; // o tipo pode ser cliente, ponto de coleta ou usuario
							// colaborador.
	private String permissao = "CLIENTE";
	private String endereco;
	private String numero;
	private String bairro;
	@ManyToOne
	private Cidade cidade;
	private String email = "";
	private String senha = "kkskssddf199";
	private String senha2 = "kkskssddf199";
	private Boolean status;
	private String responsavel;
	@Column(length = 50)
	private String telefone1Antigo;
	@Column(length = 50)
	private String telefone2Antigo;
	@Column(length = 50)
	private String telefone1;
	@Column(length = 50)
	private String telefone2;
	private Boolean telefone1Whatsapp;
	private Boolean telefone2Whatsapp;
	@Column(length = 50)
	private String tipoPessoa = "Física"; // pode ser pessoa física ou jurídica

	private String edificio;
	@Column(length = 50)
	private String sala;
	@Column(length = 50)
	private String andar;
	private String complemento;
	private String email2;
	private String email3;
	private String emailAntigo = "";
	private String email2Antigo;
	private String ramoAtividade;

	private String cpfResponsavel;
	private String rgResponsavel;
	private String orgaoEmissorRgResponsavel;
	private String enderecoResponsavel;
	@ManyToOne
	private Cidade cidadeResponsavel;

	private String apelido;

	private String mostrarTextoResponsavel = "Sim";

	private Boolean emDebito = false;
	@Column(length = 4000)
	private String historicoCobranca;

	public Boolean getEmDebito() {
		if (emDebito == null) {
			emDebito = false;
		}
		return emDebito;
	}

	public void setEmDebito(Boolean emDebito) {
		this.emDebito = emDebito;
	}

	public String getHistoricoCobranca() {
		if (historicoCobranca == null) {
			historicoCobranca = "";
		}
		return historicoCobranca;
	}

	public void setHistoricoCobranca(String historicoCobranca) {
		this.historicoCobranca = historicoCobranca;
	}

	/**
	 * Acrescenta mais uma linha ao histórico de notificações de cobrança enviadas pra este
	 * cliente. O canal ("EMAIL" ou "WHATSAPP") fica marcado no início da linha, pra
	 * getHistoricoCobrancaLinhas() separar depois e a tela mostrar o ícone certo.
	 */
	public void registrarEnvioCobranca(Date data, String canal) {
		String linha = canal + " - " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(data);
		historicoCobranca = getHistoricoCobranca().isEmpty() ? linha : getHistoricoCobranca() + "\n" + linha;
	}

	/**
	 * Remove a última linha do histórico de cobrança — usado quando o usuário desfaz uma
	 * confirmação de WhatsApp feita por engano, pra tirar do histórico o registro que a
	 * confirmação tinha acabado de criar.
	 */
	public void removerUltimoRegistroCobranca() {
		if (getHistoricoCobranca().isEmpty()) {
			return;
		}
		int ultimaQuebra = historicoCobranca.lastIndexOf('\n');
		historicoCobranca = ultimaQuebra == -1 ? "" : historicoCobranca.substring(0, ultimaQuebra);
	}

	private static final String MARCADOR_LOTE_PREFIXO = "LOTE - ";

	/**
	 * Marca no histórico o início de uma nova rodada de cobrança (nova importação do relatório
	 * de vencidos). Não apaga nada do histórico anterior — só serve de fronteira pra
	 * getHistoricoCobrancaLinhasAtuais()/getHistoricoCobrancaLinhasAnteriores() separarem o que
	 * é desta rodada do que é de rodadas passadas.
	 */
	public void marcarNovoLoteCobranca(Date data) {
		String linha = MARCADOR_LOTE_PREFIXO + new SimpleDateFormat("dd/MM/yyyy").format(data);
		historicoCobranca = getHistoricoCobranca().isEmpty() ? linha : getHistoricoCobranca() + "\n" + linha;
	}

	private List<String> getLinhasBrutasHistorico() {
		if (getHistoricoCobranca().isEmpty()) {
			return new ArrayList<>();
		}
		return Arrays.asList(historicoCobranca.split("\n"));
	}

	/** Posição, nas linhas brutas, logo depois do último marcador de novo lote — sem marcador, tudo é da rodada atual. */
	private int getInicioLoteAtual(List<String> linhasBrutas) {
		for (int i = linhasBrutas.size() - 1; i >= 0; i--) {
			if (linhasBrutas.get(i).startsWith(MARCADOR_LOTE_PREFIXO)) {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * Histórico já separado em canal + texto, pra tela iterar e mostrar o ícone certo
	 * (envelope pra email, whatsapp pra whatsapp) na frente de cada linha. Linhas antigas
	 * (de antes de existir o canal WhatsApp aqui) não têm o prefixo — tratadas como EMAIL,
	 * que era o único canal na época.
	 */
	private List<HistoricoCobrancaLinha> converterHistorico(List<String> linhasBrutas) {
		List<HistoricoCobrancaLinha> linhas = new ArrayList<>();
		for (String linha : linhasBrutas) {
			if (linha.startsWith("WHATSAPP - ")) {
				linhas.add(new HistoricoCobrancaLinha("WHATSAPP", linha.substring("WHATSAPP - ".length())));
			} else if (linha.startsWith("EMAIL - ")) {
				linhas.add(new HistoricoCobrancaLinha("EMAIL", linha.substring("EMAIL - ".length())));
			} else {
				linhas.add(new HistoricoCobrancaLinha("EMAIL", linha));
			}
		}
		return linhas;
	}

	/** Notificações da rodada de cobrança atual (depois do último marcarNovoLoteCobranca). */
	public List<HistoricoCobrancaLinha> getHistoricoCobrancaLinhasAtuais() {
		List<String> brutas = getLinhasBrutasHistorico();
		int inicio = getInicioLoteAtual(brutas);
		return converterHistorico(brutas.subList(inicio, brutas.size()));
	}

	/** Notificações de rodadas de cobrança anteriores — ficam escondidas por padrão na tela. */
	public List<HistoricoCobrancaLinha> getHistoricoCobrancaLinhasAnteriores() {
		List<String> brutas = getLinhasBrutasHistorico();
		int inicio = getInicioLoteAtual(brutas);
		List<String> anteriores = new ArrayList<>();
		for (int i = 0; i < inicio; i++) {
			if (!brutas.get(i).startsWith(MARCADOR_LOTE_PREFIXO)) {
				anteriores.add(brutas.get(i));
			}
		}
		return converterHistorico(anteriores);
	}

	private String senhaAdmin;
	@Temporal(TemporalType.TIMESTAMP)
	private Date senhaAdminValidade;

	public String getSenhaAdmin() {
		return senhaAdmin;
	}

	public void setSenhaAdmin(String senhaAdmin) {
		this.senhaAdmin = senhaAdmin;
	}

	public Date getSenhaAdminValidade() {
		return senhaAdminValidade;
	}

	public void setSenhaAdminValidade(Date senhaAdminValidade) {
		this.senhaAdminValidade = senhaAdminValidade;
	}

	// senha alternativa de acesso administrativo à área do cliente; ver o authentication-provider de senhaAdmin em applicationContext.xml
	public boolean isSenhaAdminAtiva() {
		return senhaAdminValidade != null && senhaAdminValidade.after(new Date());
	}

	public String getSenhaAdminStatus() {
		if (senhaAdminValidade == null) {
			return "Nenhuma senha administrativa definida.";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		if (isSenhaAdminAtiva()) {
			return "Ativa até " + sdf.format(senhaAdminValidade) + ".";
		}
		return "Expirou em " + sdf.format(senhaAdminValidade) + ".";
	}

	public String getEmail3() {
		return email3;
	}

	public void setEmail3(String email3) {
		this.email3 = email3;
	}

	public String getSenha2() {
		return senha2;
	}

	public void setSenha2(String senha2) {
		this.senha2 = senha2;
	}

	public String getTelefone2Antigo() {
		return telefone2Antigo;
	}

	public void setTelefone2Antigo(String telefone2Antigo) {
		this.telefone2Antigo = telefone2Antigo;
	}

	public String getTelefone1Antigo() {
		return telefone1Antigo;
	}

	public void setTelefone1Antigo(String telefone1Antigo) {
		this.telefone1Antigo = telefone1Antigo;
	}

	public String getEmailAntigo() {
		return emailAntigo;
	}

	public void setEmailAntigo(String emailAntigo) {
		this.emailAntigo = emailAntigo;
	}

	public String getEmail2Antigo() {
		return email2Antigo;
	}

	public void setEmail2Antigo(String email2Antigo) {
		this.email2Antigo = email2Antigo;
	}

	public String getMostrarTextoResponsavel() {
		if (mostrarTextoResponsavel == null) {
			mostrarTextoResponsavel = "Sim";
		}
		return mostrarTextoResponsavel;
	}

	public void setMostrarTextoResponsavel(String mostrarTextoResponsavel) {
		this.mostrarTextoResponsavel = mostrarTextoResponsavel;
	}

	public String getRamoAtividade() {
		if (ramoAtividade == null) {
			ramoAtividade = "";
		}
		return ramoAtividade;
	}

	public void setRamoAtividade(String ramoAtividade) {
		if (ramoAtividade == null) {
			ramoAtividade = "";
		}
		this.ramoAtividade = ramoAtividade;
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

	public String getPermissao() {
		return permissao;
	}

	public void setPermissao(String permissao) {
		this.permissao = permissao;
	}

	public String getCpfResponsavel() {
		if (cpfResponsavel == null) {
			cpfResponsavel = "";
		}
		return cpfResponsavel;
	}

	public void setCpfResponsavel(String cpfResponsavel) {
		this.cpfResponsavel = cpfResponsavel;
	}

	public String getRgResponsavel() {
		if (rgResponsavel == null) {
			rgResponsavel = "";
		}
		return rgResponsavel;
	}

	public void setRgResponsavel(String rgResponsavel) {
		this.rgResponsavel = rgResponsavel;
	}

	public String getOrgaoEmissorRgResponsavel() {
		return orgaoEmissorRgResponsavel;
	}

	public void setOrgaoEmissorRgResponsavel(String orgaoEmissorRgResponsavel) {
		this.orgaoEmissorRgResponsavel = orgaoEmissorRgResponsavel;
	}

	public String getEnderecoResponsavel() {
		return enderecoResponsavel;
	}

	public void setEnderecoResponsavel(String enderecoResponsavel) {
		this.enderecoResponsavel = enderecoResponsavel;
	}

	public Cidade getCidadeResponsavel() {
		return cidadeResponsavel;
	}

	public void setCidadeResponsavel(Cidade cidadeResponsavel) {
		this.cidadeResponsavel = cidadeResponsavel;
	}

	public String getBairro() {
		if (bairro == null) {
			bairro = "";
		}
		return bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getAndar() {
		if (andar == null) {
			andar = "";
		}
		return andar;
	}

	public void setAndar(String andar) {
		this.andar = andar;
	}

	public String getEdificio() {
		if (edificio == null) {
			edificio = "";
		}
		return edificio;
	}

	public void setEdificio(String edificio) {
		this.edificio = edificio;
	}

	public String getSala() {
		if (sala == null) {
			sala = "";
		}
		return sala;
	}

	public void setSala(String sala) {
		this.sala = sala;
	}

	public String getComplemento() {
		if (complemento == null) {
			complemento = "";
		}
		return complemento;
	}

	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}

	public String getEmail2() {
		if(email2==null) {
			email2="";
		}
		return email2;
	}

	public void setEmail2(String email2) {
		this.email2 = email2;
	}

	public String getTipoPessoa() {
		return tipoPessoa;
	}

	public void setTipoPessoa(String tipoPessoa) {
		this.tipoPessoa = tipoPessoa;
	}

	public String getNomeFantasia() {
		if (nomeFantasia == null) {
			nomeFantasia = "";
		}
		return nomeFantasia;
	}

	public void setNomeFantasia(String nomeFantasia) {
		this.nomeFantasia = nomeFantasia;
	}

	public String getTelefone1() {
		if(telefone1==null) {
			telefone1="";
		}
		return telefone1;
	}

	public void setTelefone1(String telefone1) {
		this.telefone1 = telefone1;
	}

	public String getTelefone2() {
		if(telefone2==null) {
			telefone2="";
		}
		return telefone2;
	}

	public void setTelefone2(String telefone2) {
		this.telefone2 = telefone2;
	}

	public Boolean getTelefone1Whatsapp() {
		return telefone1Whatsapp != null && telefone1Whatsapp;
	}

	public void setTelefone1Whatsapp(Boolean telefone1Whatsapp) {
		this.telefone1Whatsapp = telefone1Whatsapp;
	}

	public Boolean getTelefone2Whatsapp() {
		return telefone2Whatsapp != null && telefone2Whatsapp;
	}

	public void setTelefone2Whatsapp(Boolean telefone2Whatsapp) {
		this.telefone2Whatsapp = telefone2Whatsapp;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public String getNomeRazaoSocial() {
		if (nomeRazaoSocial == null) {
			nomeRazaoSocial = "";
		}
		return nomeRazaoSocial;
	}

	public String getNomeRazaoSocialCpfCnpj() {
		String retorno = "";
		if (nomeRazaoSocial != null) {
			retorno = "Nome/Razão: " + nomeRazaoSocial;
		}
		if (cpfCnpj != null) {
			retorno += " CPF/CNPJ: " + cpfCnpj;
		}
		return retorno;
	}

	public void setNomeRazaoSocial(String nomeRazaoSocial) {
		this.nomeRazaoSocial = nomeRazaoSocial;
	}

	public String getCpfCnpj() {
		return cpfCnpj;
	}

	public String getCpfCnpjSemMascara() {
		return cpfCnpj.replace(".", "").replace("-", "").replace("-", "").replace("/", "");
	}

	public void setCpfCnpj(String cpfCnpj) {
		this.cpfCnpj = cpfCnpj;
	}

	public String getRgInscricaoEstadual() {
		return rgInscricaoEstadual;
	}

	public void setRgInscricaoEstadual(String rgInscricaoEstadual) {
		this.rgInscricaoEstadual = rgInscricaoEstadual;
	}

	public Integer getDiaVencimentoFatura() {
		return diaVencimentoFatura;
	}

	public void setDiaVencimentoFatura(Integer diaVencimentoFatura) {
		this.diaVencimentoFatura = diaVencimentoFatura;
	}

	public String getCep() {
		if (cep == null) {
			cep = "";
		}
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getEndereco() {
		if (endereco == null) {
			endereco = "";
		}
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getNumero() {
		if (numero == null) {
			numero = "";
		}
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public Cidade getCidade() {
		return cidade;
	}

	public void setCidade(Cidade cidade) {
		this.cidade = cidade;
	}

	public String getEmail() {
		if (email == null) {
			email = "";
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public String getResponsavel() {
		return responsavel;
	}

	public void setResponsavel(String responsavel) {
		this.responsavel = responsavel;
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
		Pessoa other = (Pessoa) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String getNomeRazaoSocialNomeFantasia() {
		if (!getNomeRazaoSocial().equals("") && !getNomeFantasia().equals("")) {
			return getNomeRazaoSocial() + " - " + getNomeFantasia() + " (" + getApelido() + ")";
		} else {
			return getNomeRazaoSocial() + " (" + getApelido() + ")";
		}
	}

	public String getNomeRazaoSocialNomeFantasiaEndereco() {
		String retorno = "";
		if (!getNomeRazaoSocial().equals("") && !getNomeFantasia().equals("")) {
			retorno = getNomeRazaoSocial() + " - " + getNomeFantasia();
		} else {
			retorno = getNomeRazaoSocial();
		}

		if (!getApelido().equals("")) {
			retorno += " (" + getApelido() + ")";
		}

		if (getEndereco() != null) {
			retorno += ". " + getEndereco();
			if (getNumero() != null && !getNumero().trim().equals("")) {
				retorno += ", " + getNumero();
			}
			if (getCidade() != null) {
				retorno += " - " + getCidade().getNome() + "-" + getCidade().getEstado().getSigla();
			}
		}
		return retorno;
	}

	public String getEnderecoCompleto() {
		String retorno = "";

		if (getEndereco() != null) {
			retorno += getEndereco();
			if (getNumero() != null && !getNumero().trim().equals("")) {
				retorno += ", " + getNumero();
			}
			
			if(!getEdificio().trim().equals("")) {
				retorno+=". Edifício "+getEdificio();				
			}
			if(!getAndar().trim().equals("")) {
				retorno+=" - andar "+getAndar();				
			}
			if(!getSala().trim().equals("")) {
				retorno+=" - sala "+getSala()+".";				
			}
			if(getBairro()!=null & !getBairro().trim().equals("")) {
				retorno += " - "+getBairro();
			}
			if (getCidade() != null) {
				retorno += " - " + getCidade().getNome() + "-" + getCidade().getEstado().getSigla();
			}
		}
		return retorno;
	}

	public String getEnderecoCompletoSemCidade() {
		String retorno = "";

		if (getEndereco() != null) {
			retorno += getEndereco();
			if (getNumero() != null && !getNumero().trim().equals("")) {
				retorno += ", " + getNumero();
			}

		}
		return retorno;
	}

}
