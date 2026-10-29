package base.controle;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;

import base.modelo.Pessoa;
import base.modelo.PontoColeta;
import base.modelo.Usuario;
import base.modelo.Cliente;
import base.modelo.Coleta;
import base.modelo.ContasReceber;
import base.modelo.ContasReceberParcelas;
import base.modelo.FechamentoMes;
import base.modelo.FechamentoMesCertificadoPeriodo;
import base.modelo.FechamentoMesItensPontoColeta;
import base.modelo.GrupoResiduo;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensColeta;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.service.ColetaService;
import base.service.ContasReceberParcelasService;
import base.service.ContasReceberService;
import base.service.FechamentoMesItensPontoColetaService;
import base.service.FechamentoMesService;
import base.service.GrupoResiduoService;
import base.service.InformacoesFinanceirasService;
import base.service.ItensColetaService;
import base.service.ItensInformacaoFinanceiraGrupoResiduoService;
import base.service.ItensInformacaoFinanceiraPontoColetaService;
import base.service.PontoColetaService;
import util.ChamarRelatorio;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import util.RetornarDatas;
import dao.GenericDAO;
import javax.persistence.EntityManager;
import javax.swing.JOptionPane;

@SessionScoped
@Named("fechamentoMesMB")
public class FechamentoMesMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private FechamentoMes fechamentoMes;
	private FechamentoMes fechamentoMesSelecionado;
	private Cliente clienteSelecionado;
	private List<PontoColeta> listaPontosColeta;
	private List<FechamentoMes> listaFechamentoMes;
	private List<FechamentoMes> listaFechamentoMesExcedente;
	private List<FechamentoMes> listaTodosFechamentosCliente;
	private List<FechamentoMes> listaFechamentoMesFaturadoSemObservacao;
	private List<InformacaoFinanceira> listInformacoesFinanceiras;
	private Double totalAberto = 0.;
	private Double totalGeralAbertoSistema;
	private Double totalGeralAbertoAtivos;
	private Double totalGeralAbertoRenovados;
	private Double totalDesconto = 0.;
	private FechamentoMes fechamentoMesFaturamento;
	private FechamentoMesCertificadoPeriodo certificadoPeriodo = new FechamentoMesCertificadoPeriodo();
	private String contratosFechados = "não fechar";

	private Long idFechamentoAutenticidade;

	private ContasReceberParcelas parcelaFechamento = new ContasReceberParcelas();

	private List<InformacaoFinanceira> informacaoesFinanceirasSelecionadas = new ArrayList<>();

	private boolean expandirFiltroContratos = false;

	@Inject
	private GenericDAO<FechamentoMes> daoFechamentoMes; // faz as buscas

	@Inject
	private GenericDAO<ContasReceberParcelas> daoContasReceberParcelas; // faz as buscas

	@Inject
	private GenericDAO<ContasReceber> daoContasReceber;

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta;

	@Inject
	private GenericDAO<ItensColeta> daoItensColeta; // faz as buscas

	@Inject
	private FechamentoMesService fechamentoMesService; // inserir no banco

	@Inject
	private GenericDAO<FechamentoMesItensPontoColeta> daoFechamentoMesItensPontoColeta; // faz
																						// as
																						// buscas
	@Inject
	private EntityManager manager;

	@Inject
	private GenericDAO<Coleta> daoColeta;

	@Inject
	private GenericDAO<InformacaoFinanceira> daoInformacoesFinanceiras;

	@Inject
	private FechamentoMesItensPontoColetaService fechamentoMesItensPontoColetaService; // inserir
																						// no
																						// banco

	@Inject
	private ColetaService coletaService;

	@Inject
	private GenericDAO<InformacaoFinanceira> daoInformacaoFinanceira; // faz as
																		// buscas

	@Inject
	private GenericDAO<Cliente> daoCliente; // faz as
											// buscas

	@Inject
	private InformacoesFinanceirasService informacaoFinanceiraService; // inserir
																		// no
																		// banco

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraGrupo> daoItensInformacaoFinanceiraGrupo; // faz
																							// as
																							// buscas

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraPontoColeta> daoItensInformacaoFinanceiraPontoColeta; // faz
																										// as
	@Inject
	private ContasReceberService contasReceberService; // buscas

	@Inject
	private ContasReceberParcelasService contasReceberParcelaService;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		Calendar cal = Calendar.getInstance();
		int day = cal.get(Calendar.DATE);
		int month = cal.get(Calendar.MONTH) + 1;
		int year = cal.get(Calendar.YEAR);
		int dow = cal.get(Calendar.DAY_OF_WEEK);
		int dom = cal.get(Calendar.DAY_OF_MONTH);
		int doy = cal.get(Calendar.DAY_OF_YEAR);

		fechamentoMes.setAno(year);
		fechamentoMes.setMes((month - 1));
		if (fechamentoMes.getMes() == 0) {
			fechamentoMes.setMes(12);
			fechamentoMes.setAno(fechamentoMes.getAno() - 1);
		}

		// buscarContratos();
	}

	public void carregarFechamento(FechamentoMes fm) {
		fechamentoMesSelecionado = fm;
	}

	public void buscarContratos(String situacao) {
		listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
				"situacaoContrato = '" + situacao + "' order by dataFinal asc");
		expandirFiltroContratos = true;
	}

	public boolean isExpandirFiltroContratos() {
		return expandirFiltroContratos;
	}

	public void setExpandirFiltroContratos(boolean expandirFiltroContratos) {
		this.expandirFiltroContratos = expandirFiltroContratos;
	}

	public void criarNovoObjeto() {
		fechamentoMes = new FechamentoMes();

		listaFechamentoMes = new ArrayList<>();
	}

	public void buscarParcelaFechamento(FechamentoMes f) {
		parcelaFechamento = contasReceberParcelaService.buscarParcelaFechamentoMes(f.getId());
		if (parcelaFechamento != null) {
			parcelaFechamento.setBoletoEmitido(f.getFaturado());
			fechamentoMesSelecionado = f;
		} else {

		}

	}

	public void calcularDescontoTotalAberto() {

		totalAberto = 0.;
		totalDesconto = 0.;
		if (listaFechamentoMesExcedente != null) {
			for (FechamentoMes fs : listaFechamentoMesExcedente) {
				totalAberto += fs.getValorFaturar();
				fs.setValorPagoFaturamento(fs.getValorFaturar());
				totalDesconto += fs.getValorDesconto();
			}
		}
		/// System.out.println(totalDesconto);
	}

	public void buscarFechamentoAberto(FechamentoMes f) {
		fechamentoMesFaturamento = f;

		listaFechamentoMesExcedente = daoFechamentoMes.listar(FechamentoMes.class,
				"idInformacaoFinanceiraUnico=" + f.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico()
						+ " and (faturado is false or faturado is null) and valorFaturar > 0");
		calcularDescontoTotalAberto();
	}

	public void buscarTodosFechamentoCliente(FechamentoMes f) {
		fechamentoMesFaturamento = f;

		listaTodosFechamentosCliente = daoFechamentoMes.listar(FechamentoMes.class,
				"idInformacaoFinanceiraUnico=" + f.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico());
		// calcularDescontoTotalAberto();
		totalAberto = 0.;
		totalDesconto = 0.;
	}

	public void mostrarObservacao(String ob) {
		System.out.println(ob);
	}

	public void salvarParcelaFechamento() {
		int controle = 0, controle2 = 0;
		if (listaFechamentoMesExcedente.size() > 0) {
			for (FechamentoMes fm : listaFechamentoMesExcedente) {
				String ob = fm.getObservacao();
				if (fm.getFaturado().equals(true)) {
					ContasReceberParcelas parcela = contasReceberParcelaService.buscarParcelaFechamentoMes(fm.getId());
					// System.out.println(parcela);
					if (parcela != null) {

						if (fm.getId().equals(fechamentoMesFaturamento.getId())) {
							controle = 1;
							// fm = fechamentoMesFaturamento;
						}
						controle2 = 1;
						parcela.setBoletoEmitido(true);
						parcela.setNossoNumero(parcelaFechamento.getNossoNumero());
						parcela.setDataVencimento(parcelaFechamento.getDataVencimento());
//						System.out.println("Observação: " + fm.getMes() + " - " + fm.getObservacao());
						parcela.setObservacao(fm.getObservacao());
//						System.out.println("Observação 2: " + fm.getMes() + " - " + fm.getObservacao());
//System.out.println("OBSERVAOC: "+ob);
						if (parcela.getValorParcela().equals(fm.getValorDesconto())) {
							// parcela.setValorPago(0.);
							// parcela.setDesconto(fm.getValorDesconto());
							// parcela.setDataPagamento(new Date());
						}
						contasReceberParcelaService.inserirAlterar(parcela);
						// FechamentoMes
						// fmm=daoFechamentoMes.buscarPorId(FechamentoMes.class,fm.getId());
						fm.setFechamentoAnteriorAberto(false);
						fm.setFaturado(true);

						fechamentoMesService.inserirAlterar(fm);
						// System.out.println(fm.getFaturado());
						fechamentoMesSelecionado = null;
					}
				}
			}

			if (controle == 0 && controle2 == 1) {
				fechamentoMesFaturamento.setFechamentoAnteriorAberto(false);
				fechamentoMesService.inserirAlterar(fechamentoMesFaturamento);
			}

			// buscarFechamentoMes("");
			ExibirMensagem.exibirMensagem("Faturamento Alterado com Sucesso!");
		}
	}

	public void cancelarParcelaFechamento(FechamentoMes fm) {
		// System.out.println("No cancelar");
		ContasReceberParcelas parcela = contasReceberParcelaService.buscarParcelaFechamentoMes(fm.getId());
		if (parcela != null) {
			// System.out.println("no if do cancelar");
			parcela.setBoletoEmitido(false);
			contasReceberParcelaService.inserirAlterar(parcela);
			fm.setFechamentoAnteriorAberto(true);
			fm.setFaturado(false);
			fechamentoMesService.inserirAlterar(fm);
			ExibirMensagem.exibirMensagem("Faturamento Cancelado com Sucesso!");
		} else {
			ExibirMensagem.exibirMensagemErro(
					"Para contrato Anual/Boleto e sem excedente não é possível cancelar o faturamento!");
		}
	}

	public void salvarFechamento() {

		if (fechamentoMesSelecionado != null && fechamentoMesSelecionado.getId() != null) {
			fechamentoMesService.inserirAlterar(fechamentoMesSelecionado);
			ExibirMensagem.exibirMensagem("Faturamento Cancelado com Sucesso!");
			fechamentoMesSelecionado = null;
		}
	}

	public void buscarFechamentoFaturadoSemObservacoes() {
		listaFechamentoMesFaturadoSemObservacao = daoFechamentoMes.listar(FechamentoMes.class,
				"faturado is true and valorFaturar>0 and observacao like '' order by mes, ano asc");
	}

	public void buscarFechamentoMes(String parametro) {
		String extraCondicao = "";
		if (parametro.equals("excedente")) {
			extraCondicao = " AND (fm.valorExcedenteGruposContrato > 0 OR fm.valorExcedenteGruposCobradosParte > 0)";
		}
		if (fechamentoMes.getMes() != null && fechamentoMes.getAno() != null) {
			listaFechamentoMes = fechamentoMesService.listarFechamentoMesComJoinFetch(
					fechamentoMes.getMes(), fechamentoMes.getAno(), extraCondicao);
		}
		fechamentoMesService.preencherFechamentosAbertosEmLote(listaFechamentoMes);
	}

	public void buscarFechamentoMesClienteSelecionado() {
		listaFechamentoMes = new ArrayList<>();
		if (clienteSelecionado != null) {
			listaFechamentoMes = fechamentoMesService.listarFechamentoMesPorClienteComJoinFetch(
					clienteSelecionado.getId());
		}
		fechamentoMesService.preencherFechamentosAbertosEmLote(listaFechamentoMes);
	}

	public void buscarFechamentoAutenticidade() {
		if (idFechamentoAutenticidade != null && idFechamentoAutenticidade > 0) {
			listaFechamentoMes = daoFechamentoMes.listar(FechamentoMes.class, "id=" + idFechamentoAutenticidade);
			if (listaFechamentoMes.size() < 1) {
				ExibirMensagem.exibirMensagem("Nada encontrado!!");
			}
		}
	}

	public void buscarFechamentoMesCliente() {
		String nomeUsuario = "";
		Cliente cliente = null;
		Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null) {
			// System.out.println("Aquii usuario sessão");
			Object obj = authentication.getPrincipal();
			if (obj instanceof UserDetails) {
				nomeUsuario = ((UserDetails) obj).getUsername();
			} else {
				nomeUsuario = obj.toString();
			}
		}

		List<Cliente> usu = daoCliente.listar(Cliente.class,
				"(email='" + nomeUsuario + "' or email2='" + nomeUsuario + "')");
		if (usu.size() > 0) {
			cliente = usu.get(0);
		}
		//System.out.println(usu);

		if (fechamentoMes.getMes() != null && fechamentoMes.getAno() != null && cliente != null) {

			listaFechamentoMes = daoFechamentoMes.listar(FechamentoMes.class,
					" informacaoFinanceira.cliente.email like '%" + cliente.getEmail() + "%' order by ano desc, mes desc");
		}
	}

	public void faturarFechamentoMes(FechamentoMes fec, Boolean faturamento) {

		fec.setFaturado(faturamento);
		fechamentoMesService.inserirAlterar(fec);

		if (faturamento == true) {
			ExibirMensagem.exibirMensagem("Faturamento Realizado com Sucesso!");
		} else {
			ExibirMensagem.exibirMensagem("Faturamento Cancelado!");
		}

	}

	public void inativarFechamentoMes(FechamentoMes fec) {
		if (fechamentoMes.getMes() != null && fechamentoMes.getAno() != null && fechamentoMes.getMes() != 0
				&& fechamentoMes.getAno() != 0) {
			String consulta = "";
			if (fec != null) {
				consulta = "and id=" + fec.getId();
			}

			listaFechamentoMes = daoFechamentoMes.listar(FechamentoMes.class,
					"mes=" + fechamentoMes.getMes() + " and ano=" + fechamentoMes.getAno() + " " + consulta);
			if (fec != null) {
				listaFechamentoMes = new ArrayList<>();
				listaFechamentoMes.add(fec);
			}

			for (FechamentoMes fm : listaFechamentoMes) {
				List<FechamentoMesItensPontoColeta> lfmipc = daoFechamentoMesItensPontoColeta
						.listar(FechamentoMesItensPontoColeta.class, "fechamentoMes.id=" + fm.getId());
				for (FechamentoMesItensPontoColeta fmipc : lfmipc) {
					fmipc.setStatus(false);
					fechamentoMesItensPontoColetaService.inserirAlterar(fmipc);
				}

//				List<Coleta> lc = daoColeta
//						.listar(Coleta.class, "fechamentoMes.id=" + fm.getId());
//				for (Coleta fmipc : lc) {
//					fmipc.setFechamentoMes(null);
//					coletaService.inserirAlterar(fmipc);
//				}

				fm.setStatus(false);
				fechamentoMesService.inserirAlterar(fm);

				List<ContasReceber> lcrl = daoContasReceber.listar(ContasReceber.class,
						"fechamentoMes.id=" + fm.getId());
				if (lcrl.size() > 0) {
					lcrl.get(0).setStatus(false);
					contasReceberService.inserirAlterar(lcrl.get(0));

					List<ContasReceberParcelas> lcrlp = daoContasReceberParcelas.listar(ContasReceberParcelas.class,
							"contasReceber.id=" + lcrl.get(0).getId());
					if (lcrlp.size() > 0) {
						lcrlp.get(0).setStatus(false);
						contasReceberParcelaService.inserirAlterar(lcrlp.get(0));
					}

				}
			}
			listaFechamentoMes = new ArrayList<>();
			ExibirMensagem.exibirMensagem("Finalizado!");
		}

	}

	public void gerarRomaneio(FechamentoMes fec) {

		HashMap parametro = new HashMap<>();
		parametro.put("ID_FECHAMENTO_MES", fec.getId());
		ChamarRelatorio ch = new ChamarRelatorio("romaneio.jasper", parametro, "romaneio_" + fec.getId());
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void buscarPontosColetaCliente(Long idCliente) {
		listaPontosColeta = daoPontoColeta.listar(PontoColeta.class, "cliente.id=" + idCliente);
	}

	public void gerarCertificadoPeriodo(PontoColeta pc) {

		String dados = "<b><br/>" + pc.getNomeRazaoSocial().replace("&", "&amp;") + " - "
				+ pc.getNomeFantasia().replace("&", "&amp;") + " <br/>CPF/CNPJ: " + pc.getCpfCnpj() + " <br/>Endereço: "
				+ pc.getEndereco() + ", " + pc.getNumero() + " -  " + pc.getCidade().getNome() + "-"
				+ pc.getCidade().getEstado().getSigla() + "</b>";
//System.out.println(certificadoPeriodo.getAnoInicial());
//System.out.println(certificadoPeriodo.getMesInicial());
		HashMap parametro = new HashMap<>();
		parametro.put("DADOS", dados);
		parametro.put("DATA_INICIAL",
				(certificadoPeriodo.getAnoInicial() + "-" + certificadoPeriodo.getMesInicial() + "-01"));
		parametro.put("DATA_FINAL",
				(certificadoPeriodo.getAnoFinal() + "-" + certificadoPeriodo.getMesFinal() + "-30"));
		parametro.put("PONTO_COLETA_ID", pc.getId());
		ChamarRelatorio ch = new ChamarRelatorio("certificado_periodo.jasper", parametro,
				"certificado_periodo_" + pc.getId());
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void gerarCertificado(FechamentoMes fec) {

		String dados = "<b><br/>"
				+ fec.getInformacaoFinanceira().getCliente().getNomeRazaoSocial().replace("&", "&amp;") + " - "
				+ fec.getInformacaoFinanceira().getCliente().getNomeFantasia().replace("&", "&amp;")
				+ " <br/>CPF/CNPJ: " + fec.getInformacaoFinanceira().getCliente().getCpfCnpj() + " <br/>Endereço: "
				+ fec.getInformacaoFinanceira().getCliente().getEndereco() + ", "
				+ fec.getInformacaoFinanceira().getCliente().getNumero() + " -  "
				+ fec.getInformacaoFinanceira().getCliente().getCidade().getNome() + "-"
				+ fec.getInformacaoFinanceira().getCliente().getCidade().getEstado().getSigla() + "</b>";

		String periodo = "";
		if (fec.getInformacaoFinanceira().getFechamentoDataDiferente() == true) {
			periodo = "período de <b>"
					+ new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(fec.getDataInicialFechamento())
					+ " a " + new SimpleDateFormat("dd/MM/yyyy").format(fec.getDataFinalFechamento()) + "</b>.";
		} else {
			periodo = "mês de <b>" + new DateFormatSymbols(new Locale("pt", "BR")).getMonths()[(fec.getMes() - 1)]
					+ " de " + fec.getAno() + "</b>.";
		}

		if (fec.getInformacaoFinanceira().getCobradoPorQuilo() == true) {
			// f
			// COLOCAR AQUI PARA ALTERAR O RELATÓRIO POR QUILO
		}

		HashMap parametro = new HashMap<>();
		parametro.put("ID_FECHAMENTO_MES", fec.getId());
		parametro.put("DADOS", dados);
		parametro.put("PERIODO", periodo);
		ChamarRelatorio ch = new ChamarRelatorio("certificado.jasper", parametro, "certificado_" + fec.getId());
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void removerFechamentoNovaEspernca() {
		List<FechamentoMes> lf = daoFechamentoMes.listaComStatus(FechamentoMes.class);
		for (FechamentoMes fechamento : lf) {
			if (fechamento.getInformacaoFinanceira().getId() == 756) {
				// System.out.println("aquii");
				fechamento.setStatus(false);
				fechamentoMesService.inserirAlterar(fechamento);
			}

		}
		ExibirMensagem.exibirMensagem("Finalizado");
	}

	public void atualizarDataInicialFinalFechamento() {

		List<FechamentoMes> lf = daoFechamentoMes.listaComStatus(FechamentoMes.class);
		for (FechamentoMes fechamento : lf) {
			if (fechamento.getDataInicialFechamento() == null) {
				if (fechamento.getInformacaoFinanceira().getFechamentoDataDiferente().equals(true)) {

					Calendar calDataInicial = Calendar.getInstance();
					calDataInicial.setTime(new Date());
					calDataInicial.set(Calendar.DAY_OF_MONTH,
							(fechamento.getInformacaoFinanceira().getDiaFechamento() + 1));
					calDataInicial.set(Calendar.MONTH, (fechamento.getMes() - 2));
					calDataInicial.set(Calendar.YEAR, fechamento.getAno());
					fechamento.setDataInicialFechamento(calDataInicial.getTime());

					// System.out.println(fechamento.getDataInicialFechamento());

					Calendar calDataFinal = Calendar.getInstance();
					calDataFinal.setTime(new Date());
					calDataFinal.set(Calendar.DAY_OF_MONTH, fechamento.getInformacaoFinanceira().getDiaFechamento());
					calDataFinal.set(Calendar.MONTH, (fechamento.getMes() - 1));
					calDataFinal.set(Calendar.YEAR, fechamento.getAno());
					fechamento.setDataFinalFechamento(calDataFinal.getTime());

					fechamentoMesService.inserirAlterar(fechamento);
					// System.out.println(fechamento.getDataFinalFechamento());

				} else {

					Calendar calDataInicial = Calendar.getInstance();
//			calDataInicial.setTime(new Date());
					calDataInicial.set(Calendar.MONTH, (fechamentoMes.getMes() - 1));
					calDataInicial.set(Calendar.YEAR, fechamentoMes.getAno());
					calDataInicial.set(Calendar.DATE, 1);
					fechamento.setDataInicialFechamento(calDataInicial.getTime());

					Calendar calDataFinal = Calendar.getInstance();
//			calDataFinal.setTime(new Date());
					calDataFinal.set(Calendar.MONTH, (fechamentoMes.getMes() - 1));
					calDataFinal.set(Calendar.YEAR, fechamentoMes.getAno());
					calDataFinal.set(Calendar.DATE, calDataFinal.getActualMaximum(Calendar.DATE));
					fechamento.setDataFinalFechamento(calDataFinal.getTime());
					fechamentoMesService.inserirAlterar(fechamento);
				}
				// System.out.println(fechamento.getMes() + " - " + fechamento.getAno());
			}
		}
		ExibirMensagem.exibirMensagem("Finalizado");
	}

	public void corrigirFechamentosDuplicados() {

		List<InformacaoFinanceira> li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
				"situacaoContrato='Ativo' and dataInicial between '2000-01-01' and '2022-06-31'");
		int quantidade=0;
		int quantidadeMaiorDois=0;
		for (InformacaoFinanceira i : li) {
			List<FechamentoMes> lfm = daoFechamentoMes.listar(FechamentoMes.class,
					"mes=6 and ano=2022 and informacaoFinanceira.id="+i.getId());
			if(lfm.size()>1) {
				if(lfm.size()>2) {
					quantidadeMaiorDois++;
				}
				quantidade++;
				System.out.println("Tamanho: "+lfm.size());
				for(FechamentoMes f:lfm) {
					System.out.println(f.getId() +" - "+ f.getDataFechamento()+"");
					if(f.getDataFinalFechamento()==null)System.out.println(f.getId()+" - NULO");
					
					
				}
				System.out.println("Excluir: "+lfm.get(1).getId());
				this.inativarFechamentoMes(lfm.get(1));
				System.out.println("-----------------------------------------------\n");
			}
			
		}
		System.out.println("QUantidade 2: "+quantidade);
		System.out.println("QUantidade Maior que Dois: "+quantidadeMaiorDois);
	}
	
	public void corrigirFechamentosSemItensColeta() {

		
		
			List<FechamentoMes> lfm = daoFechamentoMes.listar(FechamentoMes.class,
					"mes=6 and ano=2022");
			int quantidade=0;
			for(FechamentoMes fm:lfm) {
				List<Coleta> listaColetas = daoColeta.listar(Coleta.class, "fechamentoMes.id="+fm.getId());		
				if(listaColetas.size()==0) {
					quantidade++;
					List<FechamentoMesItensPontoColeta> listaPontosColeta = daoFechamentoMesItensPontoColeta.listar(FechamentoMesItensPontoColeta.class, "fechamentoMes.id="+fm.getId());
					for(FechamentoMesItensPontoColeta fmipc:listaPontosColeta) {
						System.out.println(fmipc.getItensPontoColeta().getPontoColeta().getNomeRazaoSocialNomeFantasia());
							List<Coleta> coletas = daoColeta.listar(Coleta.class, "pontoColeta.id="+fmipc.getItensPontoColeta().getPontoColeta().getId()+" and dataColeta BETWEEN '2022-06-01' AND '2022-06-30'");
							for(Coleta c:coletas) {
								//System.out.println("AQ");
							c.setFechamentoMes(fm);
							coletaService.inserirAlterar(c);
							}
						
					
					}
					//System.out.println(fm.getInformacaoFinanceira().getCliente().getNomeRazaoSocialNomeFantasia());
				}
			}
			System.out.println(quantidade);
	}

	public void fazerFechamentoMes(String qualFechamento, FechamentoMes fechamentoAtualizar) {
		// System.out.println("AA inicio: "+fechamentoMes.getAno());
		if (fechamentoMes.getMes() != null && fechamentoMes.getAno() != null && fechamentoMes.getMes() != 0
				&& fechamentoMes.getAno() != 0) {
			// inativarFechamentoMes();s
//			String consulta = "";
//			for (InformacaoFinanceira c : informacaoesFinanceirasSelecionadas) {
//				consulta += " and id=" + c.getId();
//			}
			listaFechamentoMes = daoFechamentoMes.listar(FechamentoMes.class,
					"mes=" + fechamentoMes.getMes() + " and ano=" + fechamentoMes.getAno());

			String dataUltimoDiaMes = RetornarDatas.ultimoDiaMes(fechamentoMes.getMes(), fechamentoMes.getAno());

			List<InformacaoFinanceira> li = new ArrayList<>();
			if (qualFechamento.equals("selecionados")) {
				li = informacaoesFinanceirasSelecionadas;
			} else if (qualFechamento.equals("todos")) {
				// COMENTEI ESSAS LINHAS DAS DATAS PARA PERMITIR O FECHAMENTO ESPORÁDICO
				// DAQUELES CONTRATOS DE JANEIRO DE 2019
				// QUE TINHA COMO DATA DE INICIO SUPERIOR A DATA DE JANEIRO

				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
						"situacaoContrato='Ativo' and dataInicial between '2000-01-01' and '" + dataUltimoDiaMes + "'");

//				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
//						"situacaoContrato='Ativo'");

			} else if (qualFechamento.equals("ativoRenovado")) {
				// COMENTEI ESSAS LINHAS DAS DATAS PARA PERMITIR O FECHAMENTO ESPORÁDICO
				// DAQUELES CONTRATOS DE JANEIRO DE 2019
				// QUE TINHA COMO DATA DE INICIO SUPERIOR A DATA DE JANEIRO

				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
						"(situacaoContrato='Ativo' or situacaoContrato='Renovado') and dataInicial between '2000-01-01' and '"
								+ dataUltimoDiaMes + "'");

//				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
//						"(situacaoContrato='Ativo' or situacaoContrato='Renovado')");

			} else if (qualFechamento.equals("ativoInativoRenovado")) {
				// COMENTEI ESSAS LINHAS DAS DATAS PARA PERMITIR O FECHAMENTO ESPORÁDICO
				// DAQUELES CONTRATOS DE JANEIRO DE 2019
				// QUE TINHA COMO DATA DE INICIO SUPERIOR A DATA DE JANEIRO

				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
						"(situacaoContrato='Ativo' or situacaoContrato='Renovado' or situacaoContrato='Inativo') and dataInicial between '2000-01-01' and '"
								+ dataUltimoDiaMes + "'");

//				li = daoInformacaoFinanceira.listar(InformacaoFinanceira.class,
//						"(situacaoContrato='Ativo' or situacaoContrato='Renovado' or situacaoContrato='Inativo')");

			} else if (qualFechamento.equals("atualizar") && fechamentoAtualizar != null) {
				li = new ArrayList<>();
				li.add(fechamentoAtualizar.getInformacaoFinanceira());
			} else if (qualFechamento.equals("atualizar") && fechamentoAtualizar == null) {
				li = new ArrayList<>();
				for (FechamentoMes f : listaFechamentoMes) {
					li.add(f.getInformacaoFinanceira());
				}
			}

			try {

				// laço principal
				for (InformacaoFinanceira informacao : li) {
					int fechamentoFeito = 0;
					FechamentoMes fechamento = new FechamentoMes();

					// VERIFICA SE JÁ TEM FECHAMENTO PARA O MÊS
					for (FechamentoMes f : listaFechamentoMes) {
//						System.out.println("Contrato1 : "+informacao.getCliente().getNomeRazaoSocialNomeFantasiaEndereco());
//						System.out.println("Contrato: "+informacao.getId());
//System.out.println("Nulo: "+f.getInformacaoFinanceira().getId());
//						 System.out.println(f.getInformacaoFinanceira().getId()+ " Null: "+f.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico());
						if (f.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico()
								.equals(informacao.getIdInformacaoFinanceiraUnico())) {
							fechamentoFeito = 1;
							if (contratosFechados.equals("fechar")) {
								// FechamentoMes ff = daoFechamentoMes.buscarPorId(FechamentoMes.class,
								// f.getInformacaoFinanceira().getId());
								fechamento = f;
								fechamento.setDiferencaGerarContasPagar(0.);
								fechamento.setPesoColetadoGrupoParte(0.);
								fechamento.setPesoColetadoGruposContrato(0.);
								fechamento.setPesoExcedenteCobradoParte(0.);
								fechamento.setPesoExcedenteGruposContrato(0.);
								fechamento.setPesoExcedenteTodosPontos(0.);
								fechamento.setQuantidadePontos(0);
								fechamento.setValorExcedenteGruposCobradosParte(0.);
								fechamento.setValorExcedenteGruposContrato(0.);
								fechamento.setValorExcedenteTodosPontos(0.);
								fechamento.setValorFinal(0.);
								fechamento.setValorFixoMensalCobradoParte(0.);
								fechamento.setValorFixoMensalContratoPonto(0.);
								fechamento.setValorFixoTodosPontos(0.);
								fechamento.setValorTotalPorQuilo(0.);
								fechamento.setValorFaturar(0.);
								fechamento.setFechamentoAnteriorAberto(false);

								List<Coleta> lca = daoColeta.listarCadastrosSemStatus(Coleta.class,
										"fechamentoMes.id=" + fechamento.getId());
								for (Coleta c : lca) {
									c.setFechamentoMes(null);
									coletaService.inserirAlterar(c);
								}
//							JOptionPane.showMessageDialog(null, "Excluído????");
							}
							break;
						}
					}
					//
					if (fechamentoFeito == 0 || (fechamentoFeito == 1 && contratosFechados.equals("fechar"))) {
						fechamento.setAno(fechamentoMes.getAno());
						// System.out.println("ANO: "+fechamento.getAno());
						fechamento.setMes(fechamentoMes.getMes());
						fechamento.setDataFechamento(new Date());
						fechamento.setInformacaoFinanceira(informacao);
						fechamento.setIdInformacaoFinanceiraUnico(informacao.getIdInformacaoFinanceiraUnico());
						fechamentoMesService.inserirAlterar(fechamento);

						List<FechamentoMesItensPontoColeta> listaFechamentoItens = new ArrayList<>();

						List<ItensInformacaoFinanceiraPontoColeta> listaItensPontos = daoItensInformacaoFinanceiraPontoColeta
								.listar(ItensInformacaoFinanceiraPontoColeta.class,
										"informacoesFinanceiras.id=" + informacao.getId());
						List<ItensInformacaoFinanceiraGrupo> listaItensGrupos = daoItensInformacaoFinanceiraGrupo
								.listar(ItensInformacaoFinanceiraGrupo.class,
										"informacoesFinanceiras.id=" + informacao.getId());
//					System.out.println("LISTA ITENs pontos: " + listaItensPontos.size());
						// System.out.println(informacao.getCliente().getNomeRazaoSocialNomeFantasiaEndereco());
						for (ItensInformacaoFinanceiraPontoColeta itensPontos : listaItensPontos) {

							// CONSULTA DE FECHAMENTO PARCIAL DEVE SER FEITA NESTA DE BAIXO...
							String consulta = "coleta.pontoColeta.id=" + itensPontos.getPontoColeta().getId()
									+ " and dataColeta BETWEEN '" + fechamentoMes.getAno() + "-"
									+ fechamentoMes.getMes() + "-01' and '" + fechamentoMes.getAno() + "-"
									+ fechamentoMes.getMes() + "-31'";
							if (fechamento.getInformacaoFinanceira().getFechamentoDataDiferente().equals(true)) {

								Calendar calDataInicial = Calendar.getInstance();
								calDataInicial.setTime(new Date());
								calDataInicial.set(Calendar.DAY_OF_MONTH,
										(fechamento.getInformacaoFinanceira().getDiaFechamento() + 1));
								calDataInicial.set(Calendar.MONTH, (fechamentoMes.getMes() - 2));
								calDataInicial.set(Calendar.YEAR, fechamentoMes.getAno());
								fechamento.setDataInicialFechamento(calDataInicial.getTime());

								// System.out.println(fechamento.getDataInicialFechamento());

								Calendar calDataFinal = Calendar.getInstance();
								calDataFinal.setTime(new Date());
								calDataFinal.set(Calendar.DAY_OF_MONTH,
										fechamento.getInformacaoFinanceira().getDiaFechamento());
								calDataFinal.set(Calendar.MONTH, (fechamentoMes.getMes() - 1));
								calDataFinal.set(Calendar.YEAR, fechamentoMes.getAno());
								fechamento.setDataFinalFechamento(calDataFinal.getTime());

								// System.out.println(fechamento.getDataFinalFechamento());

							} else {

								Calendar calDataInicial = Calendar.getInstance();
//							calDataInicial.setTime(new Date());
								calDataInicial.set(Calendar.MONTH, (fechamentoMes.getMes() - 1));
								calDataInicial.set(Calendar.YEAR, fechamentoMes.getAno());
								calDataInicial.set(Calendar.DATE, 1);
								fechamento.setDataInicialFechamento(calDataInicial.getTime());

								Calendar calDataFinal = Calendar.getInstance();
//							calDataFinal.setTime(new Date());
								calDataFinal.set(Calendar.MONTH, (fechamentoMes.getMes() - 1));
								calDataFinal.set(Calendar.YEAR, fechamentoMes.getAno());
								calDataFinal.set(Calendar.DATE, calDataFinal.getActualMaximum(Calendar.DATE));
								fechamento.setDataFinalFechamento(calDataFinal.getTime());
							}

							Calendar dataPesquisaInicial = Calendar.getInstance();
							dataPesquisaInicial.setTime(fechamento.getDataInicialFechamento());

							Calendar dataPesquisaFinal = Calendar.getInstance();
							dataPesquisaFinal.setTime(fechamento.getDataFinalFechamento());

							consulta = "coleta.pontoColeta.id=" + itensPontos.getPontoColeta().getId()
									+ " and coleta.dataColeta BETWEEN '" + dataPesquisaInicial.get(Calendar.YEAR) + "-"
									+ (dataPesquisaInicial.get(Calendar.MONTH) + 1) + "-"
									+ dataPesquisaInicial.get(Calendar.DAY_OF_MONTH) + "' and '"
									+ dataPesquisaFinal.get(Calendar.YEAR) + "-"
									+ (dataPesquisaFinal.get(Calendar.MONTH) + 1) + "-"
									+ dataPesquisaFinal.get(Calendar.DAY_OF_MONTH) + "'";

//						System.out.println(consulta);

							List<ItensColeta> listaItensColeta = daoItensColeta.listarCadastros(ItensColeta.class,
									consulta);
//						System.out.println("Tamanho da LISTA>>: " + listaItensColeta.size());

							FechamentoMesItensPontoColeta fechamentoItens = new FechamentoMesItensPontoColeta();
							fechamentoItens.setFechamentoMes(fechamento);
							fechamentoItens.setItensPontoColeta(itensPontos);

							// BUSCA OS FechamentoMesItensPontoColeta POIS JÁ FOI FEITO O FECHAMENTO PARA O
							// MÊS
//						f
							if (fechamentoFeito == 1 && contratosFechados.equals("fechar")) {
								List<FechamentoMesItensPontoColeta> ffcm = daoFechamentoMesItensPontoColeta.listar(
										FechamentoMesItensPontoColeta.class, "fechamentoMes.id=" + fechamento.getId()
												+ " and itensPontoColeta.id=" + itensPontos.getId());
								if (ffcm.size() > 0) {
									fechamentoItens = ffcm.get(0);
									fechamentoItens.setGruposCobradosParte("");
									fechamentoItens.setGruposFazemParteContrato("");
									fechamentoItens.setPesoColetadoGrupoParte(0.);
									fechamentoItens.setPesoColetadoGruposContrato(0.);
									fechamentoItens.setPesoExcedenteCobradoParte(0.);
									fechamentoItens.setPesoExcedenteGruposContrato(0.);
									fechamentoItens.setValorExcedenteGruposCobradosParte(0.);
									fechamentoItens.setValorExcedenteGruposContrato(0.);
									fechamentoItens.setValorFixoMensalCobradoParte(0.);
									fechamentoItens.setValorFixoMensalContratoPonto(0.);

									fechamentoItens.setValorTotal(0.);

								} else {
									 //System.out.println("A lista de FechamentoMesItensPontoColeta não tem									 elementos");
								}

								 //System.out.println(consulta);

							}

							//////

							if (itensPontos.getInformacoesFinanceiras().getExisteValorFixoMensal()
									.equals("Mensal por Ponto")) {
								fechamentoItens.setValorFixoMensalContratoPonto(
										itensPontos.getInformacoesFinanceiras().getValorFixoMensalPonto());
							}

							for (ItensInformacaoFinanceiraGrupo itemGrupo : listaItensGrupos) {
								Double tolerancia = itemGrupo.getPesoValorFixo();
								Double valorPorQuiloExcedente = itemGrupo.getValorPorQuiloExcedente();

								String grupo = itemGrupo.getGrupoResiduos().getDescricao();
								Double pesoColetado = 0.;

								Long controle = 0L;
								// System.out.println("No FOR!! 1");
								for (ItensColeta coleta : listaItensColeta) {
									// System.out.println("No FOR!! 22");
									if (coleta.getGrupoResiduo().getId().equals(itemGrupo.getGrupoResiduos().getId())) {
										pesoColetado += coleta.getQuantidadeColetada();
										// System.out.println("No IFF!! 1");
										if (!coleta.getColeta().getId().equals(controle)) {

											controle = coleta.getColeta().getId();
											coleta.getColeta().setFechamentoMes(fechamento);
											// System.out.println("FEchamento Mês!!!!!");
											coletaService.inserirAlterar(coleta.getColeta());
										}

									}
								}
								// System.out.println("Item Grupo: " +
								// itemGrupo.getGrupoResiduos().getDescricao() + " - "
								// + itemGrupo.isCobraValorParte());
								if (itemGrupo.isCobraValorParte()) {
									fechamentoItens.setGruposCobradosParte(fechamentoItens.getGruposCobradosParte()
											+ " " + itemGrupo.getGrupoResiduos().getDescricao());
									fechamentoItens.setValorFixoMensalCobradoParte(
											fechamentoItens.getValorFixoMensalCobradoParte()
													+ itemGrupo.getValorFixo());
									fechamentoItens.setPesoColetadoGrupoParte(
											fechamentoItens.getPesoColetadoGrupoParte() + pesoColetado);
									if (pesoColetado > itemGrupo.getPesoValorFixo()) {
										Double pesoCobrar = pesoColetado - itemGrupo.getPesoValorFixo();
										fechamentoItens.setValorExcedenteGruposCobradosParte(
												fechamentoItens.getValorExcedenteGruposCobradosParte()
														+ (pesoCobrar * itemGrupo.getValorPorQuiloExcedente()));
										fechamentoItens.setPesoExcedenteCobradoParte(
												fechamentoItens.getPesoExcedenteCobradoParte() + pesoCobrar);
									}

									// tem os pesos de tolerância, que pode ser
									// diferentes para os grupos.
									// TEM UM PROBLEMA, QUANDO UM GRUPO TEM TOLERANCIA
									// DE PESO.... ACHO QUE VAI TER
									// QUE FAZER UMA ASSOCIATIVA COM AQUELA TABELA DOS
									// ITENS DO GRUPO
								} else {
									// NÃO COBRADO A PARTE
									fechamentoItens
											.setGruposFazemParteContrato(fechamentoItens.getGruposFazemParteContrato()
													+ " " + itemGrupo.getGrupoResiduos().getDescricao());
									fechamentoItens.setPesoColetadoGruposContrato(
											fechamentoItens.getPesoColetadoGruposContrato() + pesoColetado);
									// fechamentoItens.set
									pesoColetado = fechamentoItens.getPesoColetadoGruposContrato();
									if (itensPontos.getInformacoesFinanceiras().getCobraExcedente().trim()
											.equals("Excedente por Ponto")) {
										if (pesoColetado > itensPontos.getInformacoesFinanceiras().getPesoMensal()) {
											Double pesoCobrar = pesoColetado
													- itensPontos.getInformacoesFinanceiras().getPesoMensal();
											fechamentoItens.setValorExcedenteGruposContrato((pesoCobrar
													* itensPontos.getInformacoesFinanceiras().getValorExcedente()));
											fechamentoItens.setPesoExcedenteGruposContrato(pesoCobrar);

										}

									}

								}

							}
							fechamentoItens.setValorTotal(fechamentoItens.getValorExcedenteGruposCobradosParte()
									+ fechamentoItens.getValorExcedenteGruposContrato()
									+ fechamentoItens.getValorFixoMensalCobradoParte()
									+ fechamentoItens.getValorFixoMensalContratoPonto());
							fechamentoMesItensPontoColetaService.inserirAlterar(fechamentoItens);
							listaFechamentoItens.add(fechamentoItens);

						}
						Double pesoTotalCobrar = 0.;
						for (FechamentoMesItensPontoColeta fm : listaFechamentoItens) {
							fechamento.setValorFinal(fechamento.getValorFinal() + fm.getValorTotal());
							pesoTotalCobrar += fm.getPesoColetadoGruposContrato();

							fechamento.setValorFixoMensalContratoPonto(fechamento.getValorFixoMensalContratoPonto()
									+ fm.getValorFixoMensalContratoPonto());
							fechamento.setValorFixoMensalCobradoParte(
									fechamento.getValorFixoMensalCobradoParte() + fm.getValorFixoMensalCobradoParte());
							fechamento.setValorExcedenteGruposContrato(fechamento.getValorExcedenteGruposContrato()
									+ fm.getValorExcedenteGruposContrato());
							fechamento.setValorExcedenteGruposCobradosParte(
									fechamento.getValorExcedenteGruposCobradosParte()
											+ fm.getValorExcedenteGruposCobradosParte());
							fechamento.setPesoExcedenteGruposContrato(
									fechamento.getPesoExcedenteGruposContrato() + fm.getPesoExcedenteGruposContrato());
							fechamento.setPesoExcedenteCobradoParte(
									fechamento.getPesoExcedenteCobradoParte() + fm.getPesoExcedenteCobradoParte());
							fechamento.setPesoColetadoGruposContrato(
									fechamento.getPesoColetadoGruposContrato() + fm.getPesoColetadoGruposContrato());
							fechamento.setPesoColetadoGrupoParte(
									fechamento.getPesoColetadoGrupoParte() + fm.getPesoColetadoGrupoParte());

						}

						if (fechamento.getInformacaoFinanceira().getCobradoPorQuilo()) {
							fechamento.setValorTotalPorQuilo(
									pesoTotalCobrar * fechamento.getInformacaoFinanceira().getValorPorQuilo());
						}

						if (fechamento.getInformacaoFinanceira().getExisteValorFixoMensal()
								.equals("Mensal para todos Pontos")) {
							fechamento.setValorFixoTodosPontos(
									fechamento.getInformacaoFinanceira().getValorFixoMensalTotal());
						}

						if (fechamento.getInformacaoFinanceira().getCobraExcedente()
								.equals("Excedente para todos os Pontos")) {

							Double peso = fechamento.getPesoColetadoGruposContrato();

							if (peso > fechamento.getInformacaoFinanceira().getPesoMensal()) {
								Double pesoCobrar = peso - fechamento.getInformacaoFinanceira().getPesoMensal();
								fechamento.setValorExcedenteTodosPontos(
										((pesoCobrar * fechamento.getInformacaoFinanceira().getValorExcedente())));
//							System.out.println("PESO: "+peso);
//							System.out.println("PESO COBRAR: "+pesoCobrar);
//							System.out.println("Valor Excedente: "+fechamento.getInformacaoFinanceira().getValorExcedente());
//							System.out.println("CAlculo: "+pesoCobrar * fechamento.getInformacaoFinanceira().getValorExcedente());
								fechamento.setPesoExcedenteTodosPontos(pesoCobrar);
							}
						}

						fechamento.setValorFinal(fechamento.getValorFinal() + fechamento.getValorFixoTodosPontos()
								+ fechamento.getValorTotalPorQuilo() + fechamento.getValorExcedenteTodosPontos());

						List<ContasReceberParcelas> lcr = daoContasReceberParcelas.listar(ContasReceberParcelas.class,
								"referenteMes=" + fechamentoMes.getMes() + " and referenteAno=" + fechamentoMes.getAno()
										+ " and contasReceber.informacaoFinanceira.id="
										+ fechamento.getInformacaoFinanceira().getId());

						Double valoresParcelas = 0.;
						for (ContasReceberParcelas cr : lcr) {
							valoresParcelas += cr.getValorParcela();
						}

						// ESSE VALOR ESTÁ COM PROBLEMAS PQ, QUANDO ELE GERA UMA CONTAS A PAGAR E DEPOIS
						// ATUALIZA
						// O FECHAMENTO DE MÊS ELE BUSCA TODAS AS PARCELAS DO CONTAS A RECEBER....

						// COMENTEI ESSAS LINHAS ABAIXO, ATÉ O ELSE, PARA PERMITIR QUE SEJA FEITO O
						// FECHAMENTO
						// SEM QUE GERE VALOR A RECEBER - PARA FECHAMENTOS ANTERIORES...

						fechamento.setDiferencaGerarContasPagar(fechamento.getValorFinal() - valoresParcelas);

						if (fechamento.getInformacaoFinanceira().getFormaCobranca().equals("Anual")) {
							fechamento.setValorFaturar(fechamento.getValorExcedenteGruposContrato()
									+ fechamento.getValorExcedenteGruposCobradosParte()
									+ fechamento.getValorExcedenteTodosPontos());
						} else {
//						System.out.println("DEntro do Valor faturar: "+fechamento.getValorFinal());
							fechamento.setValorFaturar(fechamento.getValorFinal());
						}

						if (fechamento.getValorFaturar().equals(0.)) {
							fechamento.setFaturado(true);
						}

						List<FechamentoMes> fmm = daoFechamentoMes.listar(FechamentoMes.class,
								"idInformacaoFinanceiraUnico="
										+ fechamento.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico()
										+ " and (faturado is false or faturado is null) and valorFaturar>0");
						if (fmm.size() > 0) {
							fechamento.setFechamentoAnteriorAberto(true);
						}
						// fechamento.setValorFaturar(0.);

						fechamentoMesService.inserirAlterar(fechamento);

						// O QUE NÃO ULTRAPASSOU NAO PODERIA CADASTRAR, NO CASO O SINDICATO DE NOVA
						// ESPERANÇA
						// GERAR O CONTAS A RECEBER AQUI. PARA EXCEDENTE DE CONTRATOS ANUAIS,
						// PEGAR O VALOR FINAL E TIRAR O VALOR MENSAL.....
						ContasReceberParcelas parcela = new ContasReceberParcelas();
						ContasReceber contaReceber = new ContasReceber();

						// Caso de fechamento já realizado...
						if (fechamentoFeito == 1) {
							List<ContasReceber> lcrl = daoContasReceber.listar(ContasReceber.class,
									"fechamentoMes.id=" + fechamento.getId());
							if (lcrl.size() > 0) {
								contaReceber = lcrl.get(0);

								List<ContasReceberParcelas> lcrlp = daoContasReceberParcelas.listar(
										ContasReceberParcelas.class, "contasReceber.id=" + contaReceber.getId());
								if (lcrlp.size() > 0) {
									parcela = lcrlp.get(0);
								}
							} else {
								contaReceber.setDataCadastro(new Date());
							}
						} else {
							contaReceber.setDataCadastro(new Date());
						}

						contaReceber.setFechamentoMes(fechamento);
						contaReceber.setInformacaoFinanceira(fechamento.getInformacaoFinanceira());
						contaReceber.setQuantidadeParcelas(1);

						if (fechamento.getValorFinal() == null
								|| fechamento.getInformacaoFinanceira().getValorFixoMensalPonto() == null) {
							System.out.println(fechamento.getInformacaoFinanceira().getCliente()
									.getNomeRazaoSocialNomeFantasiaEndereco());
						}

						if (fechamento.getInformacaoFinanceira().getFormaCobranca().equals("Anual")) {
							contaReceber.setValorParcelas(fechamento.getValorFinal()
									- fechamento.getInformacaoFinanceira().getValorFixoMensalPonto());
						} else {
							contaReceber.setValorParcelas(fechamento.getValorFinal());
						}
						contaReceber.setValorTotal(contaReceber.getValorParcelas());
						if (contaReceber.getValorTotal() > 0.0
								|| !fechamento.getInformacaoFinanceira().getFormaCobranca().equals("Anual")) {
							contasReceberService.inserirAlterar(contaReceber);
//O BOLETO EMITIDO ESTAVA SETADO COMO TRUE POR PADRÃO VERIFICAR COM O JOAO SE ELE BAIXOU ALGUM FECHAMENTO DE MÊS E 
//SE NÃO JOGAR TODOS COM FECHAMENTO ANUAL COMO FALSE
							parcela.setDataVencimento(gerarDataVencimento(fechamento));
							parcela.setFechamentoMes(fechamento);
							parcela.setParcela(1);
							parcela.setQuantidadeParcelas(1);
							parcela.setValorParcela(contaReceber.getValorParcelas());
							parcela.setReferenteMes(fechamento.getMes());
							parcela.setReferenteAno(fechamento.getAno());

							if (fechamento.getFaturado().equals(true)) {
								// parcela.setDataPagamento(new Date());
								// parcela.setValorPago(parcela.getValorParcela());
								parcela.setBoletoEmitido(true);
							}

							if (fechamento.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico() == null) {
								parcela.setIdInformacaoFinanceiraUnico(fechamento.getInformacaoFinanceira().getId());
							} else {
								parcela.setIdInformacaoFinanceiraUnico(
										fechamento.getInformacaoFinanceira().getIdInformacaoFinanceiraUnico());
							}

							if (contaReceber.getValorParcelas() <= 0.) {
								contaReceber.setStatus(false);
								parcela.setStatus(false);
								contasReceberService.inserirAlterar(contaReceber);
							}

							List<ContasReceberParcelas> lcrp = new ArrayList<>();
							lcrp.add(parcela);
							contasReceberService.salvar(contaReceber, lcrp);

						}

						// System.out.println("DATA VE: "+parcela.getDataVencimento());
					}

				}

				buscarFechamentoMes("");
				ExibirMensagem.exibirMensagem("Fechamento de mês realizado com sucesso!");
			} catch (Exception e) {
				ExibirMensagem.exibirMensagem("Ocorreu um ERRO ao realizar o fechamento, recomendamos...");
				e.printStackTrace();
			}
		}

	}

	public Date gerarDataVencimento(FechamentoMes fm) {

		Calendar c = Calendar.getInstance();
		// c.setTime(contasReceber.getDataPrimeiroVencimento());
		// int mes = c.get(Calendar.MONTH);
		if (fm.getInformacaoFinanceira().getDiaVencimentoFatura() != null) {
			c.set(Calendar.DAY_OF_MONTH, fm.getInformacaoFinanceira().getDiaVencimentoFatura());
		} else {
			c.set(Calendar.DAY_OF_MONTH, 10);
		}
		c.set(Calendar.MONTH, fechamentoMes.getMes());
		c.set(Calendar.YEAR, fechamentoMes.getAno());
//System.out.println(fechamentoMes.getMes()+" "+fechamentoMes.getAno()+" "+fm.getInformacaoFinanceira().getDiaVencimentoFatura()+" "+c.getTime());
		return c.getTime();

	}

	public void inativar(FechamentoMes t) {
		t.setStatus(false);
		fechamentoMesService.inserirAlterar(t);
		// informacoesFinanceirasService.update(" InformacaoFinanceira set
		// status =
		// false where id = " +
		// t.getId());

		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);

	}

	public void salvar() {

		try {

			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogInformacoesFinanceiras();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}

	}

	public void mudarStatusFechamentoMes(FechamentoMes f) {
		f.setFaturado(true);
	}

	public FechamentoMes getFechamentoMes() {
		return fechamentoMes;
	}

	public void setFechamentoMes(FechamentoMes fechamentoMes) {
		this.fechamentoMes = fechamentoMes;
	}

	public List<FechamentoMes> getListaFechamentoMes() {
		return listaFechamentoMes;
	}

	public void setListaFechamentoMes(List<FechamentoMes> listaFechamentoMes) {
		this.listaFechamentoMes = listaFechamentoMes;
	}

	/**
	 * Soma de TUDO que está em aberto no sistema (excedente + faturamento mensal, qualquer
	 * situação de contrato) — não depende de nenhuma busca feita na tela, é uma visão geral
	 * fixa. Calculado uma vez e mantido em cache (o bean é @SessionScoped) pra não rodar essa
	 * consulta a cada re-render da página; some com um novo login/sessão.
	 */
	public Double getTotalGeralAbertoSistema() {
		if (totalGeralAbertoSistema == null) {
			totalGeralAbertoSistema = fechamentoMesService.buscarTotalGeralAbertoSistema();
		}
		return totalGeralAbertoSistema;
	}

	/**
	 * Mesmo espírito de getTotalGeralAbertoSistema(), mas só dos contratos com situação Ativo
	 * (agrupados por idInformacaoFinanceiraUnico) — excedente + faturamento mensal juntos.
	 */
	public Double getTotalGeralAbertoAtivos() {
		if (totalGeralAbertoAtivos == null) {
			totalGeralAbertoAtivos = fechamentoMesService.buscarTotalGeralAbertoPorSituacao("Ativo");
		}
		return totalGeralAbertoAtivos;
	}

	/**
	 * Mesmo espírito de getTotalGeralAbertoAtivos(), mas só dos contratos com situação
	 * Renovado.
	 */
	public Double getTotalGeralAbertoRenovados() {
		if (totalGeralAbertoRenovados == null) {
			totalGeralAbertoRenovados = fechamentoMesService.buscarTotalGeralAbertoPorSituacao("Renovado");
		}
		return totalGeralAbertoRenovados;
	}

	/**
	 * Soma do valorFinal de todos os fechamentos atualmente listados (resultado da busca).
	 */
	public Double getFaturamentoTotalListado() {
		Double total = 0.;
		if (listaFechamentoMes != null) {
			for (FechamentoMes fm : listaFechamentoMes) {
				if (fm.getValorFinal() != null) {
					total += fm.getValorFinal();
				}
			}
		}
		return total;
	}

	/**
	 * Soma do valorFaturar DAS LINHAS listadas (só o(s) mês(es) que a busca atual trouxe pra
	 * cada contrato), filtrado pela formaCobranca da versão de informacaoFinanceira à qual
	 * cada fechamento está ligado. 'Anual' é excedente de verdade (cobrança além da
	 * mensalidade fixa do contrato); 'Mensal' é a cobrança mensal recorrente normal — não é
	 * excedente, mas ainda é dinheiro em aberto, por isso mostramos os dois separados.
	 */
	private Double somarValorFaturarListado(String formaCobranca) {
		Double total = 0.;
		if (listaFechamentoMes != null) {
			for (FechamentoMes fm : listaFechamentoMes) {
				if (fm.getValorFaturar() != null && fm.getValorFaturar() > 0
						&& (fm.getFaturado() == null || !fm.getFaturado())
						&& fm.getInformacaoFinanceira() != null
						&& formaCobranca.equals(fm.getInformacaoFinanceira().getFormaCobranca())) {
					total += fm.getValorFaturar();
				}
			}
		}
		return total;
	}

	public Double getExcedenteTotalListado() {
		return somarValorFaturarListado("Anual");
	}

	public Double getFaturamentoMensalTotalListado() {
		return somarValorFaturarListado("Mensal");
	}

	/**
	 * Soma do total em aberto (todos os meses pendentes, não só o(s) que aparece(m) na busca
	 * atual) dos contratos que estão listados, filtrado por formaCobranca — o mesmo total que
	 * já aparece empilhado na coluna "Excedente/Fat." de cada linha, sem repetir o mesmo
	 * contrato duas vezes caso ele apareça em mais de uma linha da busca (ex.: Mês/Ano=Todos).
	 */
	private Double somarTotalGeralContratosListados(String formaCobranca) {
		if (listaFechamentoMes == null || listaFechamentoMes.isEmpty()) {
			return 0.;
		}
		Set<Long> idsUnicos = new HashSet<>();
		for (FechamentoMes fm : listaFechamentoMes) {
			if (fm.getIdInformacaoFinanceiraUnico() != null) {
				idsUnicos.add(fm.getIdInformacaoFinanceiraUnico());
			}
		}
		Map<Long, Double> totais = fechamentoMesService.buscarTotaisAbertosEmLote(new ArrayList<>(idsUnicos),
				formaCobranca);
		Double total = 0.;
		for (Double v : totais.values()) {
			total += v;
		}
		return total;
	}

	public Double getExcedenteTotalGeralContratosListados() {
		return somarTotalGeralContratosListados("Anual");
	}

	public Double getFaturamentoMensalTotalGeralContratosListados() {
		return somarTotalGeralContratosListados("Mensal");
	}

	public Long getIdFechamentoAutenticidade() {
		return idFechamentoAutenticidade;
	}

	public void setIdFechamentoAutenticidade(Long idFechamentoAutenticidade) {
		this.idFechamentoAutenticidade = idFechamentoAutenticidade;
	}

	public List<InformacaoFinanceira> getInformacaoesFinanceirasSelecionadas() {
		return informacaoesFinanceirasSelecionadas;
	}

	public void setInformacaoesFinanceirasSelecionadas(List<InformacaoFinanceira> informacaoesFinanceirasSelecionadas) {
		this.informacaoesFinanceirasSelecionadas = informacaoesFinanceirasSelecionadas;
	}

	public ContasReceberParcelas getParcelaFechamento() {
		return parcelaFechamento;
	}

	public void setParcelaFechamento(ContasReceberParcelas parcelaFechamento) {
		this.parcelaFechamento = parcelaFechamento;
	}

	public List<FechamentoMes> getListaFechamentoMesExcedente() {
		return listaFechamentoMesExcedente;
	}

	public void setListaFechamentoMesExcedente(List<FechamentoMes> listaFechamentoMesExcedente) {
		this.listaFechamentoMesExcedente = listaFechamentoMesExcedente;
	}

	public Double getTotalAberto() {
		return totalAberto;
	}

	public void setTotalAberto(Double totalAberto) {
		this.totalAberto = totalAberto;
	}

	public Double getTotalDesconto() {
		return totalDesconto;
	}

	public void setTotalDesconto(Double totalDesconto) {
		this.totalDesconto = totalDesconto;
	}

	public FechamentoMes getFechamentoMesFaturamento() {
		return fechamentoMesFaturamento;
	}

	public void setFechamentoMesFaturamento(FechamentoMes fechamentoMesFaturamento) {
		this.fechamentoMesFaturamento = fechamentoMesFaturamento;
	}

	public List<FechamentoMes> getListaFechamentoMesFaturadoSemObservacao() {
		return listaFechamentoMesFaturadoSemObservacao;
	}

	public void setListaFechamentoMesFaturadoSemObservacao(
			List<FechamentoMes> listaFechamentoMesFaturadoSemObservacao) {
		this.listaFechamentoMesFaturadoSemObservacao = listaFechamentoMesFaturadoSemObservacao;
	}

	public List<InformacaoFinanceira> getListInformacoesFinanceiras() {
		return listInformacoesFinanceiras;
	}

	public void setListInformacoesFinanceiras(List<InformacaoFinanceira> listInformacoesFinanceiras) {
		this.listInformacoesFinanceiras = listInformacoesFinanceiras;
	}

	public Cliente getClienteSelecionado() {
		return clienteSelecionado;
	}

	public void setClienteSelecionado(Cliente clienteSelecionado) {
		this.clienteSelecionado = clienteSelecionado;
	}

	public List<FechamentoMes> getListaTodosFechamentosCliente() {
		return listaTodosFechamentosCliente;
	}

	public void setListaTodosFechamentosCliente(List<FechamentoMes> listaTodosFechamentosCliente) {
		this.listaTodosFechamentosCliente = listaTodosFechamentosCliente;
	}

	public FechamentoMes getFechamentoMesSelecionado() {
		return fechamentoMesSelecionado;
	}

	public void setFechamentoMesSelecionado(FechamentoMes fechamentoMesSelecionado) {
		this.fechamentoMesSelecionado = fechamentoMesSelecionado;
	}

	public List<PontoColeta> getListaPontosColeta() {
		return listaPontosColeta;
	}

	public void setListaPontosColeta(List<PontoColeta> listaPontosColeta) {
		this.listaPontosColeta = listaPontosColeta;
	}

	public FechamentoMesCertificadoPeriodo getCertificadoPeriodo() {
		return certificadoPeriodo;
	}

	public void setCertificadoPeriodo(FechamentoMesCertificadoPeriodo certificadoPeriodo) {
		this.certificadoPeriodo = certificadoPeriodo;
	}

	public String getContratosFechados() {
		return contratosFechados;
	}

	public void setContratosFechados(String contratosFechados) {
		this.contratosFechados = contratosFechados;
	}

	/** por enquanto, retorna pelo nome/razão social do cliente **/
	// public List<InformacaoFinanceira> completarAtividade(String str) {
	// List<InformacaoFinanceira> list =
	// daoInformacoesFinanceiras.listaComStatus(InformacaoFinanceira.class);
	// List<InformacaoFinanceira> selecionados = new ArrayList<>();
	// for (InformacaoFinanceira cur : list) {
	// if (cur.getCliente().getNomeRazaoSocial().toLowerCase().startsWith(str))
	// {
	// selecionados.add(cur);
	// }
	// }
	// return selecionados;
	// }

}
