package base.controle;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import base.modelo.Cliente;
import base.modelo.FechamentoMes;
import base.modelo.NfseConfiguracao;
import base.modelo.NotaFiscalServico;
import base.service.NfseEmissaoService;
import base.service.NfseService;
import dao.GenericDAO;
import util.ExibirMensagem;
import util.nfse.NfseNacionalClient.NfseException;
import util.nfse.NfseNacionalClient.NfseFalhaComunicacaoException;

/** Tela de emissão e consulta de NFS-e (Sistema Nacional NFS-e). */
@ViewScoped
@Named("nfseMB")
public class NfseMB implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private NfseService nfseService;

	@Inject
	private NfseEmissaoService nfseEmissaoService;

	@Inject
	private GenericDAO<Cliente> daoCliente;

	@Inject
	private GenericDAO<FechamentoMes> daoFechamento;

	private NfseConfiguracao configuracao;
	private NotaFiscalServico nota;
	private NotaFiscalServico notaSelecionada;
	private List<NotaFiscalServico> listaNotas = new ArrayList<>();
	private Integer mesFiltro;
	private Integer anoFiltro;
	/** Id do fechamento vindo da tela de Fechamento do Mês (?fechamento=ID). */
	private Long fechamentoId;
	private boolean abrirDialogAoCarregar;
	private NotaFiscalServico notaCancelar;
	private Integer motivoCancelamento = 1;
	private String justificativaCancelamento;

	@PostConstruct
	public void init() {
		configuracao = nfseService.carregarConfiguracao();
		Calendar cal = Calendar.getInstance();
		mesFiltro = cal.get(Calendar.MONTH) + 1;
		anoFiltro = cal.get(Calendar.YEAR);
		buscar();
		novaNota();
		carregarFechamento();
	}

	/** Tela aberta a partir do Fechamento do Mês (nfse.jsf?fechamento=ID). */
	private void carregarFechamento() {
		try {
			String param = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
					.get("fechamento");
			if (param == null || !param.matches("\\d+")) {
				return;
			}
			fechamentoId = Long.valueOf(param);
			FechamentoMes f = daoFechamento.buscarPorId(FechamentoMes.class, fechamentoId);
			if (f == null || f.getInformacaoFinanceira() == null || f.getMes() == null || f.getAno() == null) {
				return;
			}
			nota = nfseEmissaoService.novaNota(f.getInformacaoFinanceira().getCliente(), f.getValorFinal(), f.getMes(),
					f.getAno(), nfseService.buscarVencimento(f.getId()));
			nota.setFechamentoMes(f);
			abrirDialogAoCarregar = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isAbrirDialogAoCarregar() {
		return abrirDialogAoCarregar;
	}

	public void buscar() {
		StringBuilder cond = new StringBuilder("1=1");
		if (mesFiltro != null && mesFiltro > 0) {
			cond.append(" and month(dataEmissao) = ").append(mesFiltro);
		}
		if (anoFiltro != null && anoFiltro > 0) {
			cond.append(" and year(dataEmissao) = ").append(anoFiltro);
		}
		listaNotas = nfseService.listarNotas(cond.toString());
	}

	public void novaNota() {
		nota = nfseEmissaoService.novaNota(null, null);
	}

	public void aoMudarReferencia() {
		nfseEmissaoService.atualizarDescricao(nota);
	}

	/** A competência (data da nota) define as alíquotas do mês. */
	public void aoMudarCompetencia() {
		nfseEmissaoService.aplicarAliquotasDoMes(nota);
	}

	public String getAvisoAliquota() {
		if (nota == null || nota.getCompetencia() == null || nota.getPercentualTributosSimples() != null
				|| configuracao.getOpcaoSimplesNacional() == null || configuracao.getOpcaoSimplesNacional() != 3) {
			return null;
		}
		return "Não há alíquotas cadastradas para " + new java.text.SimpleDateFormat("MM/yyyy").format(nota.getCompetencia())
				+ ". Cadastre em Configuração NFS-e > Alíquotas do mês, ou digite os valores abaixo.";
	}

	public void aoMudarClienteOuCompetencia() {
		nfseEmissaoService.atualizarPadroesCliente(nota);
	}

	public List<Cliente> completarCliente(String str) {
		List<Cliente> selecionados = new ArrayList<>();
		String q = str == null ? "" : str.toLowerCase();
		for (Cliente c : daoCliente.listaComStatus(Cliente.class, "nomeRazaoSocial")) {
			String nome = c.getNomeRazaoSocial() == null ? "" : c.getNomeRazaoSocial().toLowerCase();
			String fantasia = c.getNomeFantasia() == null ? "" : c.getNomeFantasia().toLowerCase();
			String doc = c.getCpfCnpj() == null ? "" : c.getCpfCnpj();
			if (nome.contains(q) || fantasia.contains(q) || doc.contains(q)) {
				selecionados.add(c);
			}
		}
		return selecionados;
	}

	/** Pendências de cadastro do cliente que impedem ou empobrecem a nota. */
	public String getAvisoCliente() {
		if (nota == null || nota.getCliente() == null) {
			return null;
		}
		Cliente c = nota.getCliente();
		List<String> faltando = new ArrayList<>();
		String doc = c.getCpfCnpj() == null ? "" : c.getCpfCnpj().replaceAll("\\D", "");
		if (doc.length() != 11 && doc.length() != 14) {
			faltando.add("CPF/CNPJ");
		}
		if (c.getCidade() == null || c.getCidade().getCodigoIbge() == null
				|| c.getCidade().getCodigoIbge().isEmpty()) {
			faltando.add("código IBGE da cidade (Cadastros > Cidades)");
		}
		if (c.getCep() == null || c.getCep().replaceAll("\\D", "").length() != 8) {
			faltando.add("CEP");
		}
		if (faltando.isEmpty()) {
			return null;
		}
		return "Cadastro do cliente incompleto: " + String.join(", ", faltando)
				+ (doc.length() == 11 || doc.length() == 14 ? ". A nota sai sem o endereço do tomador." : ".");
	}

	/** Avisa se já existe nota emitida/pendente para o mesmo cliente e competência. */
	public String getAvisoDuplicidade() {
		if (nota == null || nota.getCliente() == null || nota.getCliente().getId() == null
				|| nota.getMesReferencia() == null || nota.getAnoReferencia() == null) {
			return null;
		}
		List<NotaFiscalServico> existentes = nfseService.listarNotas("cliente.id = " + nota.getCliente().getId()
				+ " and situacao in ('" + NotaFiscalServico.SITUACAO_EMITIDA + "','"
				+ NotaFiscalServico.SITUACAO_PENDENTE + "') and mesReferencia = " + nota.getMesReferencia() + " and anoReferencia = "
				+ nota.getAnoReferencia());
		if (existentes.isEmpty()) {
			return null;
		}
		NotaFiscalServico n = existentes.get(0);
		return "Atenção: já existe NFS-e " + n.getSituacao().toLowerCase()
				+ (n.getNumeroNfse() != null ? " nº " + n.getNumeroNfse() : "") + " para este cliente neste mês de referência"
				+ " (R$ " + String.format("%.2f", n.getValor()) + ").";
	}

	public void emitir() {
		if (nota.getCliente() == null) {
			ExibirMensagem.exibirMensagemErro("Selecione o cliente.");
			return;
		}
		try {
			nota.setUsuarioEmissao(usuarioLogado());
			nota = nfseEmissaoService.emitir(nota);
			if (nota.isEmitida()) {
				ExibirMensagem.exibirMensagem("NFS-e " + (nota.getNumeroNfse() == null ? "" : "nº " + nota.getNumeroNfse() + " ")
						+ "emitida com sucesso!");
				PrimeFaces.current().executeScript("PF('dlgEmitirNfse').hide();");
				novaNota();
			} else {
				ExibirMensagem.exibirMensagemErro("Nota rejeitada pela Sefin Nacional: " + nota.getMensagemRetorno());
				// número da DPS já foi consumido: prepara nova tentativa com os mesmos dados
				nota = copiarParaNovaTentativa(nota);
			}
		} catch (NfseFalhaComunicacaoException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage()
					+ " A nota ficou PENDENTE: use o botão Consultar na lista antes de emitir de novo.");
			PrimeFaces.current().executeScript("PF('dlgEmitirNfse').hide();");
			novaNota();
		} catch (NfseException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagemErro("Erro inesperado ao emitir: " + e.getMessage());
		}
		buscar();
	}

	// ---------------- Cancelamento ----------------

	public void prepararCancelamento(NotaFiscalServico n) {
		notaCancelar = n;
		motivoCancelamento = 1;
		justificativaCancelamento = "";
	}

	public void cancelar() {
		if (notaCancelar == null) {
			return;
		}
		String just = justificativaCancelamento == null ? "" : justificativaCancelamento.trim();
		if (just.length() < 15) {
			ExibirMensagem.exibirMensagemErro("Escreva a justificativa com pelo menos 15 caracteres.");
			return;
		}
		try {
			NotaFiscalServico n = nfseEmissaoService.cancelar(notaCancelar, motivoCancelamento, just,
					usuarioLogado());
			ExibirMensagem.exibirMensagem("NFS-e " + (n.getNumeroNfse() == null ? "" : "nº " + n.getNumeroNfse() + " ")
					+ "cancelada com sucesso.");
			PrimeFaces.current().executeScript("PF('dlgCancelarNfse').hide();");
			notaCancelar = null;
		} catch (NfseException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagemErro("Erro inesperado ao cancelar: " + e.getMessage());
		}
		buscar();
	}

	public StreamedContent baixarXmlCancelamento(NotaFiscalServico n) {
		if (n.getXmlEventoCancelamento() == null) {
			ExibirMensagem.exibirMensagemErro("Nota sem XML de cancelamento.");
			return null;
		}
		return new DefaultStreamedContent(
				new ByteArrayInputStream(n.getXmlEventoCancelamento().getBytes(StandardCharsets.UTF_8)),
				"application/xml", "Cancelamento-NFSe-" + n.getChaveAcesso() + ".xml");
	}

	public NotaFiscalServico getNotaCancelar() {
		return notaCancelar;
	}

	public Integer getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(Integer motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public String getJustificativaCancelamento() {
		return justificativaCancelamento;
	}

	public void setJustificativaCancelamento(String justificativaCancelamento) {
		this.justificativaCancelamento = justificativaCancelamento;
	}

	public void consultar(NotaFiscalServico n) {
		try {
			NotaFiscalServico atualizada = nfseEmissaoService.consultarSituacao(n);
			ExibirMensagem.exibirMensagem("Situação: " + atualizada.getSituacao());
		} catch (NfseException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagemErro("Erro ao consultar: " + e.getMessage());
		}
		buscar();
	}

	/** Reaproveita os dados de uma nota rejeitada para corrigir e reenviar. */
	public void reemitir(NotaFiscalServico n) {
		nota = copiarParaNovaTentativa(n);
	}

	public StreamedContent baixarXml(NotaFiscalServico n) {
		String xml = n.getXmlNfse() != null ? n.getXmlNfse() : n.getXmlDps();
		if (xml == null) {
			ExibirMensagem.exibirMensagemErro("Nota sem XML.");
			return null;
		}
		String nome = n.getXmlNfse() != null ? "NFSe-" + n.getChaveAcesso() + ".xml" : n.getIdDps() + ".xml";
		return new DefaultStreamedContent(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
				"application/xml", nome);
	}

	public StreamedContent baixarPdf(NotaFiscalServico n) {
		try {
			byte[] pdf = nfseEmissaoService.baixarDanfse(n);
			return new DefaultStreamedContent(new ByteArrayInputStream(pdf), "application/pdf",
					"NFSe-" + (n.getNumeroNfse() != null ? n.getNumeroNfse() : n.getChaveAcesso()) + ".pdf");
		} catch (NfseException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
			return null;
		}
	}

	public double getTotalEmitido() {
		double total = 0;
		for (NotaFiscalServico n : listaNotas) {
			if (n.isEmitida() && n.getValor() != null) {
				total += n.getValor();
			}
		}
		return total;
	}

	public int getQuantidadeEmitidas() {
		int q = 0;
		for (NotaFiscalServico n : listaNotas) {
			if (n.isEmitida()) {
				q++;
			}
		}
		return q;
	}

	public boolean isConfiguracaoIncompleta() {
		return configuracao == null || !configuracao.isCertificadoCarregado()
				|| configuracao.getCnpjPrestador() == null || configuracao.getCnpjPrestador().isEmpty()
				|| configuracao.getCodigoMunicipioEmissor() == null
				|| configuracao.getCodigoMunicipioEmissor().isEmpty();
	}

	private NotaFiscalServico copiarParaNovaTentativa(NotaFiscalServico origem) {
		NotaFiscalServico n = new NotaFiscalServico();
		n.setCliente(origem.getCliente());
		n.setFechamentoMes(origem.getFechamentoMes());
		n.setCompetencia(origem.getCompetencia());
		n.setMesReferencia(origem.getMesReferencia());
		n.setAnoReferencia(origem.getAnoReferencia());
		n.setDataBoleto(origem.getDataBoleto());
		n.setValor(origem.getValor());
		n.setDescricaoServico(origem.getDescricaoServico());
		n.setCodigoTributacaoNacional(origem.getCodigoTributacaoNacional());
		n.setCodigoTributacaoMunicipal(origem.getCodigoTributacaoMunicipal());
		n.setCodigoMunicipioPrestacao(origem.getCodigoMunicipioPrestacao());
		n.setRetencaoIssqn(origem.getRetencaoIssqn());
		n.setAliquotaIss(origem.getAliquotaIss());
		n.setPercentualTributosSimples(origem.getPercentualTributosSimples());
		return n;
	}

	private static String usuarioLogado() {
		try {
			Authentication a = SecurityContextHolder.getContext().getAuthentication();
			return a == null ? null : a.getName();
		} catch (Exception e) {
			return null;
		}
	}

	public Date getHoje() {
		return new Date();
	}

	public NfseConfiguracao getConfiguracao() {
		return configuracao;
	}

	public NotaFiscalServico getNota() {
		return nota;
	}

	public void setNota(NotaFiscalServico nota) {
		this.nota = nota;
	}

	public NotaFiscalServico getNotaSelecionada() {
		return notaSelecionada;
	}

	public void setNotaSelecionada(NotaFiscalServico notaSelecionada) {
		this.notaSelecionada = notaSelecionada;
	}

	public List<NotaFiscalServico> getListaNotas() {
		return listaNotas;
	}

	public Integer getMesFiltro() {
		return mesFiltro;
	}

	public void setMesFiltro(Integer mesFiltro) {
		this.mesFiltro = mesFiltro;
	}

	public Integer getAnoFiltro() {
		return anoFiltro;
	}

	public void setAnoFiltro(Integer anoFiltro) {
		this.anoFiltro = anoFiltro;
	}

	public Long getFechamentoId() {
		return fechamentoId;
	}

	public void setFechamentoId(Long fechamentoId) {
		this.fechamentoId = fechamentoId;
	}
}
