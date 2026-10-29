package base.controle;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.SerializationUtils;
import org.dozer.DozerBeanMapper;
import org.hibernate.Session;
import org.primefaces.PrimeFaces;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import base.modelo.Pessoa;
import base.modelo.PontoColeta;
import base.modelo.Cliente;
import base.modelo.Configuracao;
import base.modelo.ConfiguracaoAjustes;
import base.modelo.ContasReceber;
import base.modelo.ContasReceberParcelas;
import base.modelo.Contrato;
import base.modelo.FechamentoMes;
import base.modelo.GrupoResiduo;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.RelatorioCobrancaItem;
import base.service.ConfiguracaoService;
import base.service.RelatorioCobrancaService;
import base.service.ContasReceberParcelasService;
import base.service.ContasReceberService;
import base.service.ContratoService;
import base.service.GrupoResiduoService;
import base.service.InformacoesFinanceirasService;
import base.service.ItensInformacaoFinanceiraGrupoResiduoService;
import base.service.ItensInformacaoFinanceiraPontoColetaService;
import base.service.PontoColetaService;
import util.AdicionarLetraEPenultimaVirgula;
import util.ArredondarNumeros;
import util.ChamarRelatorio;
import util.ExibirMensagem;
import util.FecharDialog;
import util.FormataDataMB;
import util.FormatarMB;
import util.Mensagem;
import util.NumeroExtenso;
import dao.GenericDAO;
import net.sf.jasperreports.engine.util.ConcurrentMapping.Mapper;

@ViewScoped
@Named("informacoesFinanceirasMB")
public class InformacoesFinanceirasMB implements Serializable {

	private static final long serialVersionUID = 1L;

	// Cache do DozerBeanMapper (construtor é muito pesado ~2-5s)
	private static final org.dozer.Mapper DOZER_MAPPER = new DozerBeanMapper();

	// Instância reutilizável para formatação (métodos são stateless)
	private static final FormatarMB FORMATAR_UTIL = new FormatarMB();

	private String tipoContrato = "";
	private String classesContratadaCabecalho = "";
	private String situacaoConsulta = "Todos Ativos";

	private InformacaoFinanceira informacoesFinanceiras;
	private InformacaoFinanceira informacoesFinanceirasRenovada = new InformacaoFinanceira();
	private InformacaoFinanceira informacoesFinanceirasAntiga;
	private List<InformacaoFinanceira> informacoesFinanceirasBusca;
	private List<InformacaoFinanceira> listaInformacoesFinanceirasHistorico;
	private List<InformacaoFinanceira> listInformacoesFinanceiras;
	private List<InformacaoFinanceira> listInformacoesFinanceirasVencidas;
	private List<InformacaoFinanceira> listInformacoesFinanceirasVencer;
	private List<InformacaoFinanceira> listInformacoesFinanceirasCliente;

	private List<ContasReceberParcelas> listaContasReceberParcelas = new ArrayList<>();
	private ContasReceber contasReceber = new ContasReceber();
	private ContasReceberParcelas contasReceberParcela = new ContasReceberParcelas();

	private PontoColeta pontoColeta;
	private List<PontoColeta> pontosColetas;
	private GrupoResiduo grupoResiduo;
	private List<GrupoResiduo> gruposRediduos;
	private int tabIndex = 0;

	private ItensInformacaoFinanceiraPontoColeta itemInformacaoFinanceiraPontoColeta;
	private List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesFinanceiraPontoColeta;
	private ItensInformacaoFinanceiraGrupo itemInformacaoFinanceiraGrupo;
	private List<ItensInformacaoFinanceiraGrupo> listaItensInformacoesFinanceiraGrupo;

	private List<InformacaoFinanceira> informacoesFinanceirasSelecionadas;
	private List<RenovacaoLoteItem> listaRenovacaoLote;

	private Date dataVencimentoInicial = new Date();
	private Date dataVencimentoFinal = new Date();
	private String tipoCobrancaConsulta = "Todos";

	@Autowired
	private FormatarMB formatar;

	@Inject
	private GenericDAO<InformacaoFinanceira> daoInformacoesFinanceiras; // faz as buscas

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraGrupo> daoItensGrupo; // faz as buscas

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraPontoColeta> daoItensInformacaoFinanceiraPontoColeta;

	@Inject
	private GenericDAO<ConfiguracaoAjustes> daoConfiguracaoAjustes; // faz as buscas

	@Inject
	private InformacoesFinanceirasService informacoesFinanceirasService; // inserir no banco

	@Inject
	private GenericDAO<Configuracao> daoConfiguracao; // faz as buscas

	@Inject
	private ConfiguracaoService configuracaoService; // inserir no banco

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta; // faz as buscas

	@Inject
	private PontoColetaService pontoColetaService; // inserir no banco

	@Inject
	private GenericDAO<PontoColeta> daoGrupoResiduos; // faz as buscas

	@Inject
	private GrupoResiduoService grupoResiduosService; // inserir no banco

	@Inject
	private GenericDAO<GrupoResiduo> daoGrupoResiduo; // faz as buscas

	@Inject
	private ContasReceberService contaReceberService; // inserir no banco

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraPontoColeta> daoItemInformacaoFinanceiraPontoColeta; // faz as buscas

	@Inject
	private ItensInformacaoFinanceiraPontoColetaService itemInformacaoFinanceiraPontoColetaService; // inserir no banco

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraGrupo> daoItemInformacaoFinanceiraGrupoResiduo; // faz as buscas

	@Inject
	private ItensInformacaoFinanceiraGrupoResiduoService itemInformacaoFinanceiraGrupoResiduoService; // inserir no

	@Inject
	private GenericDAO<Cliente> daoCliente;// banco

	@Inject
	private GenericDAO<ContasReceberParcelas> daoContasReceberParcelas;// banco

	@Inject
	private GenericDAO<ContasReceber> daoContasReceber;// banco

	@Inject
	private GenericDAO<Contrato> daoContrato; // faz as buscas

	@Inject
	private ContratoService contratoService; // inserir no banco

	@Inject
	private ContasReceberParcelasService contasReceberParcelaService; // inserir no banco

	@Inject
	private RelatorioCobrancaService relatorioCobrancaService;

	@Inject
	private EntityManager manager;

	public List<RelatorioCobrancaItem> buscarItensEmAberto(Long clienteId) {
		if (clienteId == null) {
			return new ArrayList<>();
		}
		return relatorioCobrancaService.buscarItensEmAberto(clienteId);
	}

	@PostConstruct
	public void inicializar() {

		informacoesFinanceiras = new InformacaoFinanceira();

		listInformacoesFinanceiras = new ArrayList<>();
		// listInformacoesFinanceiras =
		// daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
		// "vigente is true order by dataFinal asc");
		informacoesFinanceirasBusca = new ArrayList<>();
		listaItensInformacoesFinanceiraGrupo = new ArrayList<>();
		listaItensInformacoesFinanceiraPontoColeta = new ArrayList<>();

		informacoesFinanceirasSelecionadas = new ArrayList<>();
		listaRenovacaoLote = new ArrayList<>();

		itemInformacaoFinanceiraPontoColeta = new ItensInformacaoFinanceiraPontoColeta();
		itemInformacaoFinanceiraGrupo = new ItensInformacaoFinanceiraGrupo();

	}

	public void atribuirDataVigencia() {
		informacoesFinanceiras.setDataInicial(informacoesFinanceiras.getDataEmissaoContrato());
	}

	public void corrigirIdUnicoOKUHARA() {
		InformacaoFinanceira i1 = daoInformacoesFinanceiras.buscarPorId(InformacaoFinanceira.class, 1284L);
		i1.setIdInformacaoFinanceiraUnico(1284L);
		informacoesFinanceirasService.inserirAlterar(i1);

		InformacaoFinanceira i2 = daoInformacoesFinanceiras.buscarPorId(InformacaoFinanceira.class, 1286L);
		i2.setIdInformacaoFinanceiraUnico(1284L);
		informacoesFinanceirasService.inserirAlterar(i2);

		InformacaoFinanceira i3 = daoInformacoesFinanceiras.buscarPorId(InformacaoFinanceira.class, 1288L);
		i3.setIdInformacaoFinanceiraUnico(1288L);
		informacoesFinanceirasService.inserirAlterar(i3);
		ExibirMensagem.exibirMensagem("atualizado!");
	}

	public void buscarHistoricoCliente() {
		if (informacoesFinanceiras != null && informacoesFinanceiras.getCliente() != null
				&& informacoesFinanceiras.getCliente().getId() != null) {
			listaInformacoesFinanceirasHistorico = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
					"cliente.id=" + informacoesFinanceiras.getCliente().getId());
		} else {
			ExibirMensagem.exibirMensagem("Nenhum histórico encontrado para este cliente!");
		}
	}

	public void atualizarPeriodicidadeColeta() {
		List<InformacaoFinanceira> iff = daoInformacoesFinanceiras.listaComStatus(InformacaoFinanceira.class);
		for (InformacaoFinanceira i : iff) {
			i.setPeriodicidadeColeta("Quinzenal");
			informacoesFinanceirasService.inserirAlterar(i);
		}
		ExibirMensagem.exibirMensagem("PEriodicidade atualizada com sucesso!!!!");
	}

	/**
	 * Utilitário de uso pontual: atribui o Grupo de Resíduos "A4" a todos os
	 * contratos com situacaoContrato = 'Ativo' que ainda não o possuem, e
	 * recalcula o campo "gruposColetados" dos Pontos de Coleta desses contratos
	 * (é esse campo, cacheado no PontoColeta, que o app usa para decidir quais
	 * classes de resíduo oferecer no lançamento — por isso precisa ser
	 * atualizado aqui, e não só a tabela de itens do grupo).
	 */
	public void atribuirGrupoA4TodosContratosAtivos() {
		List<GrupoResiduo> grupos = daoGrupoResiduo.listar(GrupoResiduo.class, "descricao='A4' and status=true");
		if (grupos.isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Grupo de Resíduos \"A4\" não encontrado/ativo!");
			return;
		}
		GrupoResiduo grupoA4 = grupos.get(0);

		List<InformacaoFinanceira> ativos = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
				"situacaoContrato='Ativo'");

		int adicionados = 0;
		int jaPossuiam = 0;

		for (InformacaoFinanceira contrato : ativos) {
			List<ItensInformacaoFinanceiraGrupo> existentes = daoItemInformacaoFinanceiraGrupoResiduo.listar(
					ItensInformacaoFinanceiraGrupo.class,
					"informacoesFinanceiras.id=" + contrato.getId() + " and grupoResiduos.id=" + grupoA4.getId());

			if (existentes.isEmpty()) {
				ItensInformacaoFinanceiraGrupo novo = new ItensInformacaoFinanceiraGrupo();
				novo.setInformacoesFinanceiras(contrato);
				novo.setGrupoResiduos(grupoA4);
				novo.setStatus(true);
				novo.setCobraValorParte(false);
				novo.setValorFixo(0.);
				novo.setPesoValorFixo(0.);
				novo.setValorPorQuiloExcedente(0.);

				itemInformacaoFinanceiraGrupoResiduoService.inserirAlterar(novo);
				adicionados++;
			} else {
				jaPossuiam++;
			}

			// Sempre recalcula, mesmo quando o item já existia: cobre os contratos
			// atualizados numa execução anterior que ainda não tiveram o cache do
			// PontoColeta corrigido.
			atualizarGruposColetadosPontosColeta(contrato);
		}

		ExibirMensagem.exibirMensagem("Grupo A4 atribuído a " + adicionados
				+ " contrato(s) ativo(s)! (" + jaPossuiam + " já possuíam o grupo)");
	}

	/**
	 * Recalcula a partir do banco (e não de listas em memória do dialog) o
	 * campo "gruposColetados" de cada Ponto de Coleta vinculado ao contrato
	 * informado, com base nos Grupos de Resíduos atualmente ativos nesse
	 * contrato. Mesma lógica de {@link #gruposColetados()}, porém utilizável
	 * fora do fluxo de edição/salvamento do dialog (ex.: rotinas em lote).
	 */
	private void atualizarGruposColetadosPontosColeta(InformacaoFinanceira contrato) {
		List<ItensInformacaoFinanceiraGrupo> itensGrupo = daoItemInformacaoFinanceiraGrupoResiduo.listar(
				ItensInformacaoFinanceiraGrupo.class, "informacoesFinanceiras.id=" + contrato.getId() + " and status=true");

		String gruposColetadosStr = "";
		for (ItensInformacaoFinanceiraGrupo g : itensGrupo) {
			gruposColetadosStr += (gruposColetadosStr.equals("") ? "" : ";") + g.getGrupoResiduos().getDescricao();
		}
		gruposColetadosStr += ";";

		List<ItensInformacaoFinanceiraPontoColeta> itensPonto = daoItemInformacaoFinanceiraPontoColeta.listar(
				ItensInformacaoFinanceiraPontoColeta.class, "informacoesFinanceiras.id=" + contrato.getId());

		for (ItensInformacaoFinanceiraPontoColeta i : itensPonto) {
			i.getPontoColeta().setGruposColetados(gruposColetadosStr);
			pontoColetaService.inserirAlterar(i.getPontoColeta());
		}
	}

	public void imprimirContrato(InformacaoFinanceira f) {
		//System.out.println("Imprimir... ");
		if (f == null && informacoesFinanceirasRenovada != null) {
			f = informacoesFinanceirasRenovada;
		}

		classesContratadaCabecalho = "";
		String cabecalho = "";
		String texto = "";
		if (f != null) {
			// System.out.println("Imprimir");
			texto = gerarContrato(f);
			cabecalho = f.getCodigoContratoAtual()
					+ " - CONTRATO PARTICULAR DE PRESTAÇÃO DE SERVIÇOS PARA RESÍDUOS<br/>CLASSES: "
					+ classesContratadaCabecalho;
		}

		if (f != null && f.getContrato() != null) {
			// System.out.println("no idd");

			HashMap parametro = new HashMap<>();
			parametro.put("TEXTO_CONTRATO", texto);
			parametro.put("CABECALHO", cabecalho);
			if (f.getRomaneioContratoComDadosDo().equals("Ponto de Coleta")) {
				if (f.getPontoColetaGerarContrato() != null) {
					parametro.put("CONTRATANTE", f.getPontoColetaGerarContrato().getNomeRazaoSocial());
				}
			} else {
				parametro.put("CONTRATANTE", f.getCliente().getNomeRazaoSocial());
			}

			ChamarRelatorio ch = new ChamarRelatorio("contrato.jasper", parametro,
					"contrato_" + f.getCliente().getNomeRazaoSocial());
			Session sessions = manager.unwrap(Session.class);
			sessions.doWork(ch);
		} else if (informacoesFinanceiras != null && informacoesFinanceiras.getContrato() != null) {
			HashMap parametro = new HashMap<>();
			parametro.put("TEXTO_CONTRATO", texto);
			parametro.put("CABECALHO", cabecalho);
			if (f.getRomaneioContratoComDadosDo().equals("Ponto de Coleta")) {
				if (f.getPontoColetaGerarContrato() != null) {
					parametro.put("CONTRATANTE", f.getPontoColetaGerarContrato().getNomeRazaoSocial());
				}
			} else {
				parametro.put("CONTRATANTE", f.getCliente().getNomeRazaoSocial());
			}
			ChamarRelatorio ch = new ChamarRelatorio("contrato.jasper", parametro,
					"contrato_" + informacoesFinanceiras.getCliente().getNomeRazaoSocial());
			Session sessions = manager.unwrap(Session.class);
			sessions.doWork(ch);
		}
	}

	public void informarParcela(ContasReceberParcelas p) {
		this.contasReceberParcela = p;
	}

	public void excluirParcela(ContasReceberParcelas p) {
		if (p != null && p.getId() != null) {
			p.setStatus(false);
			contasReceberParcelaService.inserirAlterar(p);
			buscarContasReceberParcelas();
			ExibirMensagem.exibirMensagem("Parcela removida!");

		} else {
			listaContasReceberParcelas.remove(p);
			ExibirMensagem.exibirMensagem("Parcela removida!");
		}
	}

	public void buscarContasReceberParcelas() {
		if (informacoesFinanceiras != null && informacoesFinanceiras.getId() != null) {
			List<ContasReceber> lc = daoContasReceber.listar(ContasReceber.class,
					"informacaoFinanceira.id=" + informacoesFinanceiras.getId());
			if (lc.size() > 0) {
				listaContasReceberParcelas = lc.get(0).getParcelas();
			}
		}
	}

	public void salvarParcela() {
		if (contasReceberParcela != null && contasReceberParcela.getId() != null) {
			contasReceberParcelaService.inserirAlterar(contasReceberParcela);
			ExibirMensagem.exibirMensagem("Parcela alterada com sucesso!");
			FecharDialog.fecharDialogAlterarParcela();
		} else {
			ExibirMensagem.exibirMensagem("Parcela alterada com sucesso!");
			FecharDialog.fecharDialogAlterarParcela();
		}
	}

	public void carregarInformacaoContasReceber() {
		if (informacoesFinanceiras.getDataPrimeiroPagamento() != null) {
			contasReceber.setDataPrimeiroVencimento(informacoesFinanceiras.getDataPrimeiroPagamento());
		}

		contasReceber.setNossoNumero(0);
		if (informacoesFinanceiras.getQuantidadeMesesContrato() != null) {
			// System.out.println("aqui no pegar q");
			contasReceber.setQuantidadeParcelas(informacoesFinanceiras.getQuantidadeMesesContrato());
		}
//		contas.setAnoPrimeiraParcela(Integer.parseInt(new FormataDataMB().formatarData(informacoesFinanceiras.getDataPrimeiroPagamento(), "yyyy", "0")));
//		contas.setMesPrimeiraParcela(Integer.parseInt(new FormataDataMB().formatarData(informacoesFinanceiras.getDataPrimeiroPagamento(), "MM", "0")));
		contasReceber.setValorParcelas(0.);
		if (informacoesFinanceiras.getExisteValorFixoMensal().equals("Mensal para todos Pontos")) {
			contasReceber.setValorParcelas(
					contasReceber.getValorParcelas() + informacoesFinanceiras.getValorFixoMensalTotal());

		} else if (informacoesFinanceiras.getExisteValorFixoMensal().equals("Mensal por Ponto")) {
			contasReceber.setValorParcelas(
					contasReceber.getValorParcelas() + (informacoesFinanceiras.getValorFixoMensalPonto()
							* listaItensInformacoesFinanceiraPontoColeta.size()));

		}
	}

	public void gerarParcelas() {
		// ContasReceber contas = new ContasReceber();
		if (informacoesFinanceiras.getDataPrimeiroPagamento() != null
				&& informacoesFinanceiras.getQuantidadeMesesContrato() != null) {
			listaContasReceberParcelas = contaReceberService.gerarParcelas(contasReceber);
		} else {
			ExibirMensagem.exibirMensagemErro("Preencher a data do 1º pagamento e a quantidade de meses do contrato!!");
		}

	}

	public String gerarContrato(InformacaoFinanceira inf) {

		// List<Contrato> lc = daoContrato.listaComStatus(Contrato.class);
		String texto = "";
//		InformacaoFinanceira inf;

		if (inf.getContrato() != null) {
			Cliente clienteGerar = null;
			if (inf.getRomaneioContratoComDadosDo().equals("Ponto de Coleta")) {
				if (inf.getPontoColetaGerarContrato() != null) {
					// Mapeia PontoColeta → Cliente via Dozer (cache singleton, sem clone)
					clienteGerar = DOZER_MAPPER.map(inf.getPontoColetaGerarContrato(), Cliente.class);
//					clienteGerar.setNomeRazaoSocial(inf.getPontoColetaGerarContrato().getNomeRazaoSocial());
				}
			} else {
				clienteGerar = inf.getCliente();
			}
			texto = inf.getContrato().getTextoModelo();
			// System.out.println(inf.getCliente().getNomeRazaoSocial());
			// System.out.println(texto);
			// System.out.println(texto.contains("::nomeRazaoSocial"));
			texto = texto.replace("::nomeRazaoSocial::", clienteGerar.getNomeRazaoSocial());
			// System.out.println(texto);
			if (!inf.getCliente().getNomeFantasia().trim().equals("")) {
				texto = texto.replace("::nomeFantasia::", "(" + clienteGerar.getNomeFantasia() + ")");
			} else {
				texto = texto.replace("::nomeFantasia::", "");
			}

			if (inf.getCliente().getTipoPessoa().equalsIgnoreCase("física")) {
				String textoTipoPessoa = "pessoa física inscrito(a) no CPF sob nº ";
				texto = texto.replace("::cpfCnpj::", textoTipoPessoa + "<b>" + clienteGerar.getCpfCnpj() + "</b>");
			} else {
				String textoTipoPessoa = "pessoa jurídica de direito privado inscrito(a) no CNPJ sob nº ";
				texto = texto.replace("::cpfCnpj::", textoTipoPessoa + "<b>" + clienteGerar.getCpfCnpj() + "</b>");
			}

			texto = texto.replace("::rgInscricaoEstadual::", clienteGerar.getRgInscricaoEstadual());
			// texto = texto.replace("::cep::", "CEP: " + inf.getCliente().getCep());
			texto = texto.replace("::ramoAtividade::", clienteGerar.getRamoAtividade());
			texto = texto.replace("::periodicidadeColeta::", inf.getPeriodicidadeColeta());

			String textoEndereco = "";
			textoEndereco += clienteGerar.getEndereco();

			if (!clienteGerar.getNumero().trim().equals("")) {
				textoEndereco += ", Nº " + clienteGerar.getNumero();
			}

			if (!clienteGerar.getEdificio().trim().equals("")) {
				textoEndereco += " (edifício: " + clienteGerar.getEdificio();
				if (!clienteGerar.getAndar().trim().equals("")) {
					textoEndereco += ", andar: " + clienteGerar.getAndar();
				}
				if (!clienteGerar.getSala().trim().equals("")) {
					textoEndereco += ", sala: " + clienteGerar.getSala();
				}
				textoEndereco += ")";

			} else {
				if (!clienteGerar.getSala().trim().equals("")) {
					textoEndereco += ", sala: " + clienteGerar.getSala();
				}
			}

			if (!clienteGerar.getBairro().trim().equals("")) {
				textoEndereco += ", " + clienteGerar.getBairro();
			}

			if (!clienteGerar.getCep().trim().equals("")) {
				textoEndereco += " - CEP: " + clienteGerar.getCep();
			}

			if (!clienteGerar.getComplemento().trim().equals("")) {
				textoEndereco += ", complemento: " + clienteGerar.getComplemento();
			}

			textoEndereco += " - <b>" + clienteGerar.getCidade().getNome() + "</b>, estado do <b>"
					+ clienteGerar.getCidade().getEstado().getNome() + "</b>";

			texto = texto.replace("::endereco::", textoEndereco);
			texto = texto.replace("::quantidadeMesesContrato::", String.valueOf(inf.getQuantidadeMesesContrato()));

			// texto = texto.replace("::endereco::", inf.getCliente().getEndereco());
			// texto = texto.replace("::numero::", inf.getCliente().getNumero());
			// texto = texto.replace("::cidade::", inf.getCliente().getCidade().getNome() +
			// ", estado do "
			// + inf.getCliente().getCidade().getEstado().getNome());
			// texto = texto.replace("::bairro::", inf.getCliente().getBairro());
			// texto = texto.replace("::edificio::", inf.getCliente().getEdificio());
			// texto = texto.replace("::sala::", inf.getCliente().getSala());
			// texto = texto.replace("::andar::", inf.getCliente().getAndar());
			// texto = texto.replace("::complemento::", inf.getCliente().getComplemento());

			if (!clienteGerar.getEmail().trim().equals("")) {
				texto = texto.replace("::email::", "E-mail: <b>" + clienteGerar.getEmail() + "</b>,");
			} else {
				texto = texto.replace("::email::", "");
			}

			if (!clienteGerar.getTelefone1().trim().equals("")) {
				String textoTelefone = "";
				textoTelefone += clienteGerar.getTelefone1().replace("_", "");

				if (!clienteGerar.getTelefone2().trim().equals("") && clienteGerar.getTelefone2().trim().length() > 9) {
					textoTelefone += " e " + clienteGerar.getTelefone2().replace("_", "");
				}

				texto = texto.replace("::telefone1::", "Telefone: " + textoTelefone + ",");
			}

			String textoResponsavel = "";
			if (clienteGerar.getResponsavel() != null) {
				textoResponsavel += " representado por <b>" + clienteGerar.getResponsavel() + "</b>";

				if (!clienteGerar.getCpfResponsavel().trim().equals("")) {
					textoResponsavel += ", inscrito no CPF: " + clienteGerar.getCpfResponsavel();
				}
				if (!clienteGerar.getRgResponsavel().trim().equals("")) {
					textoResponsavel += ", RG: " + clienteGerar.getRgResponsavel();
				}

				if (!clienteGerar.getEnderecoResponsavel().trim().equals("")
						|| clienteGerar.getCidadeResponsavel() != null) {
					textoResponsavel += ", residente e domiciliado na";
				}

				if (!clienteGerar.getEnderecoResponsavel().trim().equals("")) {
					textoResponsavel += " " + clienteGerar.getEnderecoResponsavel();
				}

				if (clienteGerar.getCidadeResponsavel() != null) {
					textoResponsavel += " cidade de " + clienteGerar.getCidadeResponsavel().getNome() + "-"
							+ clienteGerar.getCidadeResponsavel().getEstado().getNome();
				}
				textoResponsavel += ",";
			}
			texto = texto.replace("::responsavel::", textoResponsavel);
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
			texto = texto.replace("::dataInicial::", format.format(inf.getDataInicial()));

			// List<ItensInformacaoFinanceiraGrupo> itensGrupo =
			// daoItensGrupo.listar(ItensInformacaoFinanceiraGrupo.class,
			// "informacoesFinanceiras.id=" + inf.getId());

			String informacoesPagamento = "";

			if (inf.getExisteValorFixoMensal().equals("Mensal por Ponto")) {
				Double vv = 0.;
				if (inf.getFormaPagamento().equals("Mensal")) {
					vv = (inf.getValorFixoMensalPonto() * inf.getQuantidadeMesesContrato())
							/ inf.getQuantidadeMesesContrato();
				} else if (inf.getFormaPagamento().equals("Bimestral")) {
					vv = (inf.getValorFixoMensalPonto() * inf.getQuantidadeMesesContrato()) / 6;
				} else if (inf.getFormaPagamento().equals("Trimestral")) {
					vv = (inf.getValorFixoMensalPonto() * inf.getQuantidadeMesesContrato()) / 4;
				} else if (inf.getFormaPagamento().equals("Semestral")) {
					vv = (inf.getValorFixoMensalPonto() * inf.getQuantidadeMesesContrato()) / 2;
				} else if (inf.getFormaPagamento().equals("Anual")) {
					vv = (inf.getValorFixoMensalPonto() * inf.getQuantidadeMesesContrato()) / 1;
				}
				informacoesPagamento += "Valor fixo " + inf.getFormaPagamento() + " por ponto de R$ "
						+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(vv) + " ("
						+ new NumeroExtenso(new BigDecimal(vv)).toMonetario() + ") ";

			} else if (inf.getExisteValorFixoMensal().equals("Mensal para todos Pontos")) {
				Double vv = 0.;
				if (inf.getFormaPagamento().equals("Mensal")) {
					vv = (inf.getValorFixoMensalTotal() * inf.getQuantidadeMesesContrato())
							/ inf.getQuantidadeMesesContrato();
				} else if (inf.getFormaPagamento().equals("Bimestral")) {
					vv = (inf.getValorFixoMensalTotal() * inf.getQuantidadeMesesContrato()) / 6;
				} else if (inf.getFormaPagamento().equals("Trimestral")) {
					vv = (inf.getValorFixoMensalTotal() * inf.getQuantidadeMesesContrato()) / 4;
				} else if (inf.getFormaPagamento().equals("Semestral")) {
					vv = (inf.getValorFixoMensalTotal() * inf.getQuantidadeMesesContrato()) / 2;
				} else if (inf.getFormaPagamento().equals("Anual")) {
					vv = (inf.getValorFixoMensalTotal() * inf.getQuantidadeMesesContrato()) / 1;
				}

				informacoesPagamento += "Valor fixo " + inf.getFormaPagamento() + " para todos os pontos de R$ "
						+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(vv) + " ("
						+ new NumeroExtenso(new BigDecimal(vv)).toMonetario() + ") ";
			} else if (inf.getCobradoPorQuilo()) {
				informacoesPagamento += "Valor cobrado por quilo por todo material coletado de R$ "
						+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(inf.getValorPorQuilo()) + " ("
						+ new NumeroExtenso(new BigDecimal(inf.getValorPorQuilo())).toMonetario() + ") ";
			}
			informacoesPagamento += " vencíveis todo dia " + inf.getDiaVencimentoFatura() + " ("
					+ new NumeroExtenso(new BigDecimal(inf.getDiaVencimentoFatura())).toString() + ")";
			// SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
			informacoesPagamento += ", a partir de " + format.format(inf.getDataPrimeiroPagamento());

			String gruposFranquia = "";
			String gruposForaFranquia = "";
			String gruposContratados = "";

			if (inf.getId() != null) {

				this.listaItensInformacoesFinanceiraGrupo = daoItemInformacaoFinanceiraGrupoResiduo
						.listar(ItensInformacaoFinanceiraGrupo.class, "informacoesFinanceiras.id=" + inf.getId());
			}
			int contador = 0;
			for (ItensInformacaoFinanceiraGrupo ig : this.listaItensInformacoesFinanceiraGrupo) {
				if (!ig.isCobraValorParte()) {
					if (gruposFranquia.equals("")) {
						gruposFranquia += "\"" + ig.getGrupoResiduos().getDescricao() + "\"";
					} else {
						gruposFranquia += ", \"" + ig.getGrupoResiduos().getDescricao() + "\"";
					}
				} else {
					contador++;
					String letra = "";
					switch (contador) {
					case 1:
						letra = "A";
						break;
					case 2:
						letra = "B";
						break;
					case 3:
						letra = "C";
						break;
					case 4:
						letra = "D";
						break;
					case 5:
						letra = "E";
						break;

					default:
						break;
					}
					if (gruposForaFranquia.equals("")) {

						gruposForaFranquia += letra + ". O valor de R$ "
								+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(ig.getValorPorQuiloExcedente())
								+ " (" + new NumeroExtenso(new BigDecimal(ig.getValorPorQuiloExcedente())).toMonetario()
								+ ") de resíduos da classe <b>" + ig.getGrupoResiduos().getDescricao()
								+ "</b> por quilo produzidos/recolhidos; ";
					} else {
						gruposForaFranquia += " e<br/>" + letra + ". O valor de R$ "
								+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(ig.getValorPorQuiloExcedente())
								+ " (" + new NumeroExtenso(new BigDecimal(ig.getValorPorQuiloExcedente())).toMonetario()
								+ ") de resíduos da classe <b>" + ig.getGrupoResiduos().getDescricao()
								+ "</b> por quilo produzidos/recolhidos; ";
					}
				}
				if (gruposContratados.equals("")) {
					gruposContratados += " <b>\"" + ig.getGrupoResiduos().getDescricao() + "\"</b>";
				} else {
					gruposContratados += ", <b>\"" + ig.getGrupoResiduos().getDescricao() + "\"</b>";
				}
			}

			gruposContratados = AdicionarLetraEPenultimaVirgula.substituirOcorrencia(gruposContratados, ",", " e ", 1);
			classesContratadaCabecalho = gruposContratados;

			gruposFranquia = AdicionarLetraEPenultimaVirgula.substituirOcorrencia(gruposFranquia, ",", " e ", 1);

			if (!gruposFranquia.equals("")) {
				informacoesPagamento += ", com franquia de até "
						+ FORMATAR_UTIL.formatarNumeroTresCasasDecimais(inf.getPesoMensal());
				informacoesPagamento += " quilos/mês produzidos/recolhidos de resíduos das classes";
				informacoesPagamento += " " + gruposFranquia;
				if (!inf.getCobraExcedente().equals("Não")) {
					informacoesPagamento += ". O excedente desses resíduos será cobrado o preço de R$ "
							+ FORMATAR_UTIL.formatarNumeroDuasCasasDecimais(inf.getValorExcedente()) + " ("
							+ new NumeroExtenso(inf.getValorExcedente()).toMonetario()
							+ ") por quilo produzidos/recolhidos;";
				}
			}
			if (!gruposForaFranquia.equals("")) {

				informacoesPagamento += " e<br/>";
				informacoesPagamento += gruposForaFranquia;

			}

			if (informacoesPagamento.equals("")) {
				informacoesPagamento = "<b>INFORMAÇÕES NÃO ENCONTRADAS!!!!!!!!!</b>";
			}

			if (gruposContratados.equals("")) {

				gruposContratados += "<b>NENHUM GRUPO CADASTRADO PARA ESTE CONTRATO!!!!!!!</b>";
			}

			texto = texto.replace("::gruposContratados::", gruposContratados);
			texto = texto.replace("::valoresCobrados::", informacoesPagamento);

			informacoesFinanceiras.setTextoDocumentoContrato(texto);

//			HashMap parametro = new HashMap<>();
//			parametro.put("TEXTO_CONTRATO", texto);
//			ChamarRelatorio ch = new ChamarRelatorio("contrato.jasper", parametro, "modeloContrato");
//			Session sessions = manager.unwrap(Session.class);
//			sessions.doWork(ch);
//		}
		}
		return texto;
	}

	public void setarDiaPagamento() {
		if (informacoesFinanceiras != null && informacoesFinanceiras.getDataPrimeiroPagamento() != null) {
			// System.out.println("DEntro do setar dia pagamento!!");
			DateFormat format = new SimpleDateFormat("dd");
			DateFormat mes = new SimpleDateFormat("MM");
			DateFormat ano = new SimpleDateFormat("yyyy");
			contasReceber.setMesPrimeiraParcela(
					(Integer.parseInt(mes.format(informacoesFinanceiras.getDataPrimeiroPagamento())) - 1));
			contasReceber.setAnoPrimeiraParcela(
					Integer.parseInt(ano.format(informacoesFinanceiras.getDataPrimeiroPagamento())));

			if (contasReceber.getMesPrimeiraParcela() <= 1) {
				contasReceber.setMesPrimeiraParcela(12);
				contasReceber.setAnoPrimeiraParcela(contasReceber.getAnoPrimeiraParcela() - 1);
			}

			informacoesFinanceiras.setDiaVencimentoFatura(
					Integer.parseInt(format.format(informacoesFinanceiras.getDataPrimeiroPagamento())));
			carregarInformacaoContasReceber();
		}
	}

	public void atualizarAtivoInativo() {
		listInformacoesFinanceiras = new ArrayList<>();
		listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
				"vigente is true order by dataFinal asc");
		for (InformacaoFinanceira f : listInformacoesFinanceiras) {

			f.setSituacaoContrato("Ativo");
			informacoesFinanceirasService.inserirAlterar(f);

		}
		ExibirMensagem.exibirMensagem("Finalizado!");
	}

	public void buscarContratosVencidos() {

		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		listInformacoesFinanceirasVencidas = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
				"dataFinal between '2000-01-01' and '" + format.format(new Date()) + "' and situacaoContrato='Ativo'");

		listInformacoesFinanceirasVencer = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
				"dataFinal between '" + format.format(new Date())
						+ "' and '3000-01-01' and situacaoContrato='Ativo' order by dataFinal asc");
	}

	public void nada() {
		//System.out.println(situacaoConsulta);
	}

	/*public void buscarContratosVencimento() {
		if (situacaoConsulta.equals("Todos Ativos")) {
			buscarContratosAtivosInativos("Ativo");
		} else if (situacaoConsulta.equals("Todos Inativos")) {
			buscarContratosAtivosInativos("Inativo");
		} else if (situacaoConsulta.equals("Todos os Contratos")) {
			buscarContratosAtivosInativos("Todos");
		} else if (situacaoConsulta.equals("Todos Renovados")) {
			buscarContratosAtivosInativos("Renovado");
		} else if (situacaoConsulta.equals("Ativos por Data")) {

			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (dataVencimentoInicial != null && dataVencimentoFinal != null) {
				listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
						"dataFinal between '" + format.format(dataVencimentoInicial) + "' and '"
								+ format.format(dataVencimentoFinal)
								+ "' and situacaoContrato='Ativo' order by dataFinal asc");

			} else {
				ExibirMensagem.exibirMensagemErro("Selecione as datas!");
			}
		} else if (situacaoConsulta.equals("Inativos por Data")) {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (dataVencimentoInicial != null && dataVencimentoFinal != null) {
				listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
						"dataFinal between '" + format.format(dataVencimentoInicial) + "' and '"
								+ format.format(dataVencimentoFinal)
								+ "' and situacaoContrato='Inativo' order by dataFinal asc");

			} else {
				ExibirMensagem.exibirMensagemErro("Selecione as datas!");
			}
		} else if (situacaoConsulta.equals("Renovados por Data")) {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			if (dataVencimentoInicial != null && dataVencimentoFinal != null) {
				listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
						"dataFinal between '" + format.format(dataVencimentoInicial) + "' and '"
								+ format.format(dataVencimentoFinal)
								+ "' and situacaoContrato='Renovado' order by dataFinal asc");

			} else {
				ExibirMensagem.exibirMensagemErro("Selecione as datas!");
			}
		}

	}*/
	
	public void buscarContratosVencimento() {

	    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

	    boolean filtraPorData = "Ativos por Data".equals(situacaoConsulta)
	            || "Inativos por Data".equals(situacaoConsulta)
	            || "Renovados por Data".equals(situacaoConsulta);

	    if (filtraPorData) {
	        if (dataVencimentoInicial == null || dataVencimentoFinal == null) {
	            ExibirMensagem.exibirMensagemErro("Selecione as datas!");
	            return;
	        }
	    }

	    StringBuilder where = new StringBuilder();
	    where.append("SELECT DISTINCT inf FROM InformacaoFinanceira inf");
	    where.append(" LEFT JOIN FETCH inf.cliente");
	    where.append(" LEFT JOIN FETCH inf.contrato");
	    where.append(" LEFT JOIN FETCH inf.pontoColetaGerarContrato");
	    where.append(" WHERE inf.status = true");

	    // Filtro por período (quando aplicável)
	    if (filtraPorData) {
	        where.append(" AND inf.dataFinal BETWEEN '")
	             .append(format.format(dataVencimentoInicial))
	             .append("' AND '")
	             .append(format.format(dataVencimentoFinal))
	             .append("'");
	    }

	    // Filtro por situação
	    if ("Todos Ativos".equals(situacaoConsulta) || "Ativos por Data".equals(situacaoConsulta)) {
	        where.append(" AND inf.situacaoContrato='Ativo'");
	    } else if ("Todos Inativos".equals(situacaoConsulta) || "Inativos por Data".equals(situacaoConsulta)) {
	        where.append(" AND inf.situacaoContrato='Inativo'");
	    } else if ("Todos Renovados".equals(situacaoConsulta) || "Renovados por Data".equals(situacaoConsulta)) {
	        where.append(" AND inf.situacaoContrato='Renovado'");
	    }

	    // Filtro por tipo da cobrança
	    if (tipoCobrancaConsulta != null && !"Todos".equals(tipoCobrancaConsulta)) {
	        where.append(" AND inf.formaCobranca='").append(tipoCobrancaConsulta).append("'");
	    }

	    where.append(" ORDER BY inf.dataFinal ASC");

	    listInformacoesFinanceiras = manager.createQuery(where.toString(), InformacaoFinanceira.class)
	            .getResultList();
	}

	public void buscarContratosAtivosInativos(String tipo) {

		if (tipo.equals("Ativo") || tipo.equals("Inativo") || tipo.equals("Renovado")) {
			listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
					"situacaoContrato='" + tipo + "' order by dataFinal asc");

		} else if (tipo.equals("Todos")) {
			listInformacoesFinanceiras = daoInformacoesFinanceiras.listaComStatus(InformacaoFinanceira.class,
					"dataFinal asc");
		}
	}
	
	public void buscarContratosAviso() {		
			listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
					"situacaoAviso is true order by dataFinal asc");		
	}

	public void atualizarDataFinalGeral() {
		Calendar c = Calendar.getInstance();
		for (InformacaoFinanceira i : listInformacoesFinanceiras) {

			// LocalDateTime.from(i.getDataFinal().toInstant()).plusMonths(1);

			c.setTime(i.getDataInicial());
			int mes = c.get(Calendar.MONTH);
			c.set(Calendar.MONTH, (mes + i.getQuantidadeMesesContrato()));
			i.setDataFinal(c.getTime());
			informacoesFinanceirasService.inserirAlterar(i);
		}
		ExibirMensagem.exibirMensagem("Finalizado!");
	}

	public void atualizarIdUnico() {

		for (InformacaoFinanceira i : listInformacoesFinanceiras) {
			i.setIdInformacaoFinanceiraUnico(i.getId());
			informacoesFinanceirasService.inserirAlterar(i);
		}
		ExibirMensagem.exibirMensagem("Finalizado!");
	}

	public void buscarDadosContratoCliente() {
		String nomeUsuario = "";
		Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null) {
			//System.out.println("Aquii usuario sessão");
			Object obj = authentication.getPrincipal();
			if (obj instanceof UserDetails) {
				nomeUsuario = ((UserDetails) obj).getUsername();
			} else {
				nomeUsuario = obj.toString();
			}
		}

		List<Cliente> usu = daoCliente.listar(Cliente.class,
				"(email='" + nomeUsuario + "' or email2='" + nomeUsuario + "' or email3='" + nomeUsuario + "')");

		// Um mesmo e-mail de login pode estar cadastrado em mais de um registro de
		// cliente (cadastro duplicado). Buscar em todos evita que o contrato ativo
		// fique "escondido" atrás de um cadastro duplicado sem contrato.
		if (!usu.isEmpty()) {
			String idsClientes = usu.stream().map(c -> String.valueOf(c.getId()))
					.collect(java.util.stream.Collectors.joining(","));

			listInformacoesFinanceirasCliente = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
					"cliente.id in (" + idsClientes + ")"
							+ " and (situacaoContrato='Ativo' or situacaoContrato is null) order by dataFinal asc");
		}
	}

	public String chamarContasReceber(InformacaoFinanceira inf) {
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fc.getExternalContext().getSession(true);
		session.setAttribute("informacaoFinanceira", inf);
		return "contasReceber";
	}

	public void zerarValorColetaFazParteContrato() {
		if (itemInformacaoFinanceiraGrupo.isCobraValorParte()) {
			itemInformacaoFinanceiraGrupo.setValorPorQuiloExcedente(0.);
			itemInformacaoFinanceiraGrupo.setPesoValorFixo(0.);
			itemInformacaoFinanceiraGrupo.setValorFixo(0.);
		}
	}

	public void zerarExisteValorFixoMensal() {
		informacoesFinanceiras.setValorFixoMensalTotal(0.);
		informacoesFinanceiras.setValorFixoMensalPonto(0.);
		carregarInformacaoContasReceber();
	}

	public void zerarDiaFechamento() {
		informacoesFinanceiras.setDiaFechamento(0);
	}
	
	public void setarFormaCobrancaColetaUnica() {
		if(informacoesFinanceiras.getPeriodicidadeColeta().equals("Coleta Única")) {
			informacoesFinanceiras.setFormaCobranca("Mensal");
		}
	}

	public void zerarCobraValorExcedente() {
		informacoesFinanceiras.setPesoMensal(0.);
		informacoesFinanceiras.setValorExcedente(0.);
	}

	public void zerarCobradoPorQuilo() {
		informacoesFinanceiras.setValorPorQuilo(0.);
	}

	private void buscarItensPontoColeta() {
		if (informacoesFinanceiras != null && informacoesFinanceiras.getId() != null) {
			listaItensInformacoesFinanceiraPontoColeta = daoItemInformacaoFinanceiraPontoColeta.listar(
					ItensInformacaoFinanceiraPontoColeta.class,
					"informacoesFinanceiras.id=" + informacoesFinanceiras.getId());
		}
	}

	private void buscarItensGrupo() {
		if (informacoesFinanceiras != null && informacoesFinanceiras.getId() != null) {
			listaItensInformacoesFinanceiraGrupo = daoItemInformacaoFinanceiraGrupoResiduo.listar(
					ItensInformacaoFinanceiraGrupo.class,
					"informacoesFinanceiras.id=" + informacoesFinanceiras.getId());
		}
	}

	private void salvarItensPontoColeta() {
		for (ItensInformacaoFinanceiraPontoColeta it : listaItensInformacoesFinanceiraPontoColeta) {
			it.setInformacoesFinanceiras(informacoesFinanceiras);
			itemInformacaoFinanceiraPontoColetaService.inserirAlterar(it);
		}
	}

	private void salvarItensGrupo() {
		for (ItensInformacaoFinanceiraGrupo it : listaItensInformacoesFinanceiraGrupo) {
			it.setInformacoesFinanceiras(informacoesFinanceiras);
			itemInformacaoFinanceiraGrupoResiduoService.inserirAlterar(it);
		}
	}

	public void removerItensPontoColeta(ItensInformacaoFinanceiraPontoColeta item) {
		if (informacoesFinanceiras != null) {
			if (informacoesFinanceiras.getId() == null) {
				if (listaItensInformacoesFinanceiraPontoColeta.contains(item)) {
					listaItensInformacoesFinanceiraPontoColeta.remove(item);
					ExibirMensagem.exibirMensagem("Ponto exluído com sucesso!");
				}
			} else {
				item.setStatus(false);
				itemInformacaoFinanceiraPontoColetaService.inserirAlterar(item);
				ExibirMensagem.exibirMensagem("Ponto excluído com sucesso!");
				buscarItensPontoColeta();
			}
			itemInformacaoFinanceiraPontoColeta = new ItensInformacaoFinanceiraPontoColeta();
		}
	}

	public void removerItensGrupo(ItensInformacaoFinanceiraGrupo item) {
		if (informacoesFinanceiras != null) {
			if (informacoesFinanceiras.getId() == null) {
				if (!listaItensInformacoesFinanceiraGrupo.contains(item)) {
					listaItensInformacoesFinanceiraGrupo.remove(item);
					ExibirMensagem.exibirMensagem("Grupo exluído com sucesso!");
				}
			} else {
				item.setStatus(false);
				itemInformacaoFinanceiraGrupoResiduoService.inserirAlterar(item);
				ExibirMensagem.exibirMensagem("Grupo excluído com sucesso!");
				buscarItensGrupo();
			}
			itemInformacaoFinanceiraGrupo = new ItensInformacaoFinanceiraGrupo();
		}
	}

	private boolean containsItemPontoColeta(ItensInformacaoFinanceiraPontoColeta comp) {
		for (ItensInformacaoFinanceiraPontoColeta it : listaItensInformacoesFinanceiraPontoColeta) {

			if (it.getPontoColeta().getId().equals(comp.getPontoColeta().getId())) {
				return true;
			}
		}
		return false;
	}

	public void inserirItensPontoColetaCliente() {
		if (informacoesFinanceiras != null) {
			if (informacoesFinanceiras.getId() == null && informacoesFinanceiras.getCliente() != null) {
				listaItensInformacoesFinanceiraPontoColeta = new ArrayList<>();
				List<PontoColeta> itensP = daoPontoColeta.listar(PontoColeta.class,
						"cliente.id=" + informacoesFinanceiras.getCliente().getId());
				for (PontoColeta pp : itensP) {
					ItensInformacaoFinanceiraPontoColeta it = new ItensInformacaoFinanceiraPontoColeta();
					it.setPontoColeta(pp);
					listaItensInformacoesFinanceiraPontoColeta.add(it);
				}
				if (itensP.size() > 0) {
					ExibirMensagem.exibirMensagem("Ponto(s) adicionado(s)!!");
				} else {
					ExibirMensagem.exibirMensagem("Não foram encontrados pontos para este cliente!!");
				}
			}
			setarDiaPagamento();
		}
	}

	public void inserirItensPontoColeta() {
		if (informacoesFinanceiras != null && itemInformacaoFinanceiraPontoColeta.getPontoColeta() != null) {
			if (informacoesFinanceiras.getId() == null) {
				if (!containsItemPontoColeta(itemInformacaoFinanceiraPontoColeta)) {
					listaItensInformacoesFinanceiraPontoColeta.add(itemInformacaoFinanceiraPontoColeta);

					ExibirMensagem.exibirMensagem("Ponto adicionado!");
				} else {
					ExibirMensagem.exibirMensagem("Ponto de Coleta já está adicionado!");
				}
			} else {
				if (!containsItemPontoColeta(itemInformacaoFinanceiraPontoColeta)) {
					itemInformacaoFinanceiraPontoColeta.setInformacoesFinanceiras(informacoesFinanceiras);
					itemInformacaoFinanceiraPontoColetaService.inserirAlterar(itemInformacaoFinanceiraPontoColeta);
					ExibirMensagem.exibirMensagem("Ponto adicionado!");
					buscarItensPontoColeta();
				} else {
					ExibirMensagem.exibirMensagem("Ponto de Coleta já está adicionado!");
				}
			}
			itemInformacaoFinanceiraPontoColeta = new ItensInformacaoFinanceiraPontoColeta();
			setarDiaPagamento();
		} else {
			ExibirMensagem.exibirMensagem("Selecionar o Ponto de Coleta!");
		}
	}

	private boolean containsItemGrupo(ItensInformacaoFinanceiraGrupo comp) {
		for (ItensInformacaoFinanceiraGrupo it : listaItensInformacoesFinanceiraGrupo) {
			if (it.getId() == null) {
				if (it.getGrupoResiduos().getDescricao().equals(comp.getGrupoResiduos().getDescricao())) {
					return true;
				}

			} else if (!it.getId().equals(comp.getId())
					&& it.getGrupoResiduos().getDescricao().equals(comp.getGrupoResiduos().getDescricao())) {
				return true;
			}
		}
		return false;
	}

	public void alterarGrupoResiduos(ItensInformacaoFinanceiraGrupo it) {
		this.itemInformacaoFinanceiraGrupo = it;
	}

	// ESSE É O MÉTODO QUE ESTOU FAZENDO

	private void gruposColetados() {

		String gruposColetados = "";
		for (ItensInformacaoFinanceiraGrupo g : listaItensInformacoesFinanceiraGrupo) {
			if (gruposColetados.equals("")) {
				gruposColetados = (g.getGrupoResiduos().getDescricao());
			} else {
				gruposColetados += ";" + g.getGrupoResiduos().getDescricao();
			}

		}
		gruposColetados += ";";
		for (ItensInformacaoFinanceiraPontoColeta i : listaItensInformacoesFinanceiraPontoColeta) {
			i.getPontoColeta().setGruposColetados(gruposColetados);
			
			if (informacoesFinanceiras.getId() != null) {
				
				pontoColetaService.inserirAlterar(i.getPontoColeta());
			}
		}
	}

	public void inserirItensGrupo() {
		if (itemInformacaoFinanceiraGrupo.getGrupoResiduos() != null) {
			if (informacoesFinanceiras != null) {
				if (!itemInformacaoFinanceiraGrupo.isCobraValorParte()) {
					itemInformacaoFinanceiraGrupo.setValorFixo(0.);
					itemInformacaoFinanceiraGrupo.setPesoValorFixo(0.);
					itemInformacaoFinanceiraGrupo.setValorPorQuiloExcedente(0.);
				}

				if (informacoesFinanceiras.getId() == null) {
					if (!containsItemGrupo(itemInformacaoFinanceiraGrupo)) {
						listaItensInformacoesFinanceiraGrupo.add(itemInformacaoFinanceiraGrupo);

						// gruposColetados();

						ExibirMensagem.exibirMensagem("Grupo adicionado!");
					} else {
						ExibirMensagem.exibirMensagem("Grupo já está adicionado!");
					}
				} else {
					if (!containsItemGrupo(itemInformacaoFinanceiraGrupo)) {
						itemInformacaoFinanceiraGrupo.setInformacoesFinanceiras(informacoesFinanceiras);
						itemInformacaoFinanceiraGrupoResiduoService.inserirAlterar(itemInformacaoFinanceiraGrupo);

						buscarItensGrupo();
						
						ExibirMensagem.exibirMensagem("Grupo adicionado!");

					} else {
						ExibirMensagem.exibirMensagem("Grupo já está adicionado!");
					}
				}
				gruposColetados();
				itemInformacaoFinanceiraGrupo = new ItensInformacaoFinanceiraGrupo();
			}
		} else {
			ExibirMensagem.exibirMensagem("Selecionar o Grupo!");
		}
	}

	public void preencherLista(InformacaoFinanceira t) {
		informacoesFinanceirasAntiga = new InformacaoFinanceira();
		informacoesFinanceirasRenovada = new InformacaoFinanceira();
		tipoContrato = "Alteração de Contrato";
		this.informacoesFinanceiras = t;
		buscarItensPontoColeta();
		buscarItensGrupo();

		gerarContrato(t);
	}

	public void renovarContrato(InformacaoFinanceira t) {

		tipoContrato = "Renovação de Contrato";
		Long idIF = t.getId();
		this.informacoesFinanceirasAntiga = (InformacaoFinanceira) SerializationUtils.clone(t);

		List<ConfiguracaoAjustes> lconf = daoConfiguracaoAjustes.listar(ConfiguracaoAjustes.class, "situacao='Ativo'");

		this.informacoesFinanceiras = calcularRenovacao(t, lconf);
		this.listaItensInformacoesFinanceiraPontoColeta = clonarItensPontoColeta(idIF, this.informacoesFinanceiras);
		this.listaItensInformacoesFinanceiraGrupo = clonarItensGrupo(idIF, this.informacoesFinanceiras, lconf);

		setarDiaPagamento();
		gerarParcelas();
	}

	/**
	 * Calcula o clone de renovação de um contrato: desloca as datas em
	 * quantidadeMesesContrato meses e reajusta os valores conforme a
	 * ConfiguracaoAjustes ativa. Não altera nenhum campo compartilhado da tela.
	 */
	private InformacaoFinanceira calcularRenovacao(InformacaoFinanceira t, List<ConfiguracaoAjustes> lconf) {
		InformacaoFinanceira renovado = (InformacaoFinanceira) SerializationUtils.clone(t);
		renovado.setQuantidadeMesesContrato(12);
		renovado.setId(null);
		renovado.setSituacaoContrato("Ativo");
		renovado.setDataEmissaoContrato(renovado.getDataFinal());
		renovado.setDataInicial(renovado.getDataFinal());

		Calendar c = Calendar.getInstance();
		c.setTime(renovado.getDataFinal());
		int mes = c.get(Calendar.MONTH);
		c.set(Calendar.MONTH, (mes + renovado.getQuantidadeMesesContrato()));
		renovado.setDataFinal(c.getTime());

		Calendar cpp = Calendar.getInstance();
		cpp.setTime(renovado.getDataPrimeiroPagamento());
		int mescpp = cpp.get(Calendar.MONTH);
		cpp.set(Calendar.MONTH, (mescpp + renovado.getQuantidadeMesesContrato()));
		renovado.setDataPrimeiroPagamento(cpp.getTime());

		if (lconf.size() > 0) {
			ConfiguracaoAjustes conf = lconf.get(0);
			renovado.setValorFixoMensalPonto(
					(double) Math.round(t.getValorFixoMensalPonto() * ((conf.getPorcentagemValorFixo() / 100) + 1)));

			renovado.setValorFixoMensalTotal(
					(double) Math.round(t.getValorFixoMensalTotal() * ((conf.getPorcentagemValorFixo() / 100) + 1)));

			renovado.setValorExcedente(ArredondarNumeros
					.umaCasaDecimal(t.getValorExcedente() * ((conf.getPorcentagemExcedente() / 100) + 1)));

			renovado.setValorPorQuilo(ArredondarNumeros
					.umaCasaDecimal(t.getValorPorQuilo() * ((conf.getPorcentagemValorQuilo() / 100) + 1)));
		}
		return renovado;
	}

	/** Clona os Pontos de Coleta de um contrato para o novo contrato renovado. */
	private List<ItensInformacaoFinanceiraPontoColeta> clonarItensPontoColeta(Long idOriginal,
			InformacaoFinanceira renovado) {
		List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesPC = daoItemInformacaoFinanceiraPontoColeta
				.listar(ItensInformacaoFinanceiraPontoColeta.class, "informacoesFinanceiras.id=" + idOriginal);

		List<ItensInformacaoFinanceiraPontoColeta> clones = new ArrayList<>();
		for (ItensInformacaoFinanceiraPontoColeta iif : listaItensInformacoesPC) {
			ItensInformacaoFinanceiraPontoColeta i = (ItensInformacaoFinanceiraPontoColeta) SerializationUtils
					.clone(iif);
			i.setId(null);
			i.setStatus(true);
			i.setInformacoesFinanceiras(renovado);
			clones.add(i);
		}
		return clones;
	}

	/** Clona os Grupos de Resíduos de um contrato para o novo contrato renovado, reajustando o excedente. */
	private List<ItensInformacaoFinanceiraGrupo> clonarItensGrupo(Long idOriginal, InformacaoFinanceira renovado,
			List<ConfiguracaoAjustes> lconf) {
		List<ItensInformacaoFinanceiraGrupo> listaItensInformacoesFG = daoItemInformacaoFinanceiraGrupoResiduo
				.listar(ItensInformacaoFinanceiraGrupo.class, "informacoesFinanceiras.id=" + idOriginal);

		List<ItensInformacaoFinanceiraGrupo> clones = new ArrayList<>();
		for (ItensInformacaoFinanceiraGrupo ifg : listaItensInformacoesFG) {
			ItensInformacaoFinanceiraGrupo i = (ItensInformacaoFinanceiraGrupo) SerializationUtils.clone(ifg);
			i.setInformacoesFinanceiras(renovado);
			i.setId(null);
			i.setStatus(true);
			i.setPesoValorFixoAnterior(i.getPesoValorFixo());
			i.setValorPorQuiloExcedenteAnterior(i.getValorPorQuiloExcedente());

			if (lconf.size() > 0) {
				ConfiguracaoAjustes conf = lconf.get(0);
				i.setValorPorQuiloExcedente(ArredondarNumeros.umaCasaDecimal(
						i.getValorPorQuiloExcedente() * ((conf.getPorcentagemQuiloGrupoParte() / 100) + 1)));
			}
			clones.add(i);
		}
		return clones;
	}

	/**
	 * Valida a seleção feita na tabela e monta o resumo de renovação em lote,
	 * calculando a prévia (datas + valores reajustados) para cada contrato
	 * selecionado que esteja "Ativo". Não altera os campos compartilhados usados
	 * pelo dialog de edição/renovação individual.
	 */
	public void prepararRenovacaoLote() {
		listaRenovacaoLote = new ArrayList<>();

		if (informacoesFinanceirasSelecionadas == null || informacoesFinanceirasSelecionadas.isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Selecione ao menos um contrato Ativo para renovar!");
			return;
		}

		List<ConfiguracaoAjustes> lconf = daoConfiguracaoAjustes.listar(ConfiguracaoAjustes.class, "situacao='Ativo'");
		int ignorados = 0;

		for (InformacaoFinanceira original : informacoesFinanceirasSelecionadas) {
			if (!"Ativo".equals(original.getSituacaoContrato())) {
				ignorados++;
				continue;
			}

			RenovacaoLoteItem item = new RenovacaoLoteItem();
			item.setContratoOriginal(original);

			InformacaoFinanceira renovado = calcularRenovacao(original, lconf);
			item.setContratoRenovado(renovado);
			item.setItensPontoColeta(clonarItensPontoColeta(original.getId(), renovado));
			item.setItensGrupo(clonarItensGrupo(original.getId(), renovado, lconf));
			item.setSalvo(null);

			listaRenovacaoLote.add(item);
		}

		if (listaRenovacaoLote.isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Nenhum contrato Ativo foi selecionado!");
			return;
		}
		if (ignorados > 0) {
			ExibirMensagem
					.exibirMensagem(ignorados + " contrato(s) ignorado(s) por não estarem com situação 'Ativo'.");
		}
	}

	/**
	 * Salva todos os contratos do resumo de renovação em lote. Para cada item,
	 * aponta os campos compartilhados de edição para os dados daquele contrato e
	 * reutiliza os mesmos métodos privados usados pelo salvamento individual de
	 * uma renovação, para não duplicar a lógica de persistência. Uma falha em um
	 * contrato não interrompe o processamento dos demais.
	 */
	public void salvarRenovacaoLote() {
		if (listaRenovacaoLote == null || listaRenovacaoLote.isEmpty()) {
			ExibirMensagem.exibirMensagemErro("Nenhum contrato para renovar!");
			return;
		}

		int sucesso = 0;
		int falha = 0;

		for (RenovacaoLoteItem item : listaRenovacaoLote) {
			if (Boolean.TRUE.equals(item.getSalvo())) {
				continue;
			}

			try {
				this.informacoesFinanceiras = item.getContratoRenovado();
				this.listaItensInformacoesFinanceiraGrupo = item.getItensGrupo();
				this.listaItensInformacoesFinanceiraPontoColeta = item.getItensPontoColeta();
				this.contasReceber = new ContasReceber();
				this.listaContasReceberParcelas = new ArrayList<>();

				DateFormat formataMes = new SimpleDateFormat("MM");
				informacoesFinanceiras
						.setMesInicio(Integer.parseInt(formataMes.format(informacoesFinanceiras.getDataInicial())));

				DateFormat formataAno = new SimpleDateFormat("yyyy");
				informacoesFinanceiras
						.setAnoInicio(Integer.parseInt(formataAno.format(informacoesFinanceiras.getDataInicial())));

				Calendar pf = Calendar.getInstance();
				pf.setTime(informacoesFinanceiras.getDataPrimeiroPagamento());
				int pri = pf.get(Calendar.MONTH);
				pf.set(Calendar.MONTH, (pri + informacoesFinanceiras.getQuantidadeMesesContrato()));
				informacoesFinanceiras.setDataUltimoPagamento(pf.getTime());

				List<InformacaoFinanceira> antigos = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
						"idInformacaoFinanceiraUnico=" + item.getContratoOriginal().getIdInformacaoFinanceiraUnico());
				SimpleDateFormat formatObs = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				for (InformacaoFinanceira antigo : antigos) {
					antigo.setSituacaoContrato("Renovado");
					antigo.setObservacaoSituacaoContrato(antigo.getObservacaoSituacaoContrato()
							+ ". Contrato Renovado no dia " + formatObs.format(new Date()));
					informacoesFinanceirasService.inserirAlterar(antigo);
				}
				informacoesFinanceiras.setStatus(true);
				informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);

				salvarItensGrupo();
				salvarItensPontoColeta();
				setarDiaPagamento();
				gerarParcelas();
				salvarParcelas();

				gruposColetados();
				for (ItensInformacaoFinanceiraPontoColeta it : listaItensInformacoesFinanceiraPontoColeta) {
					PontoColeta pontoPeriodo = it.getPontoColeta();
					if (informacoesFinanceiras.getPeriodicidadeColeta().equals("Semanal")) {
						pontoPeriodo.setQuantidadeColetaMes(4);
					} else if (informacoesFinanceiras.getPeriodicidadeColeta().equals("Quinzenal")) {
						pontoPeriodo.setQuantidadeColetaMes(2);
					} else {
						pontoPeriodo.setQuantidadeColetaMes(1);
					}
					pontoPeriodo.setPeriodicidadeColeta(informacoesFinanceiras.getPeriodicidadeColeta());

					// Coleta Única: renovar o contrato representa um novo pedido de coleta.
					// Zera a última coleta para que o ponto volte a aparecer como pendente
					// no app dos motoristas, sem depender de baixa manual do contrato anterior.
					if ("Coleta Única".equals(informacoesFinanceiras.getPeriodicidadeColeta())) {
						pontoPeriodo.setDiaUltimaColeta(0);
						pontoPeriodo.setMesUltimaColeta(0);
						pontoPeriodo.setAnoUltimaColeta(0);
						pontoPeriodo.setDataUltimaColeta(null);
						pontoPeriodo.setPrimeiraColeta(true);
						pontoPeriodo.setDataUltimaAlteracao(new Date());
					}

					pontoColetaService.inserirAlterar(pontoPeriodo);
				}

				if (informacoesFinanceiras.getCodigoContratoAtual() == null
						|| informacoesFinanceiras.getCodigoContratoAtual().equals(0)
						|| informacoesFinanceiras.getCodigoContratoAtual() < 1) {
					List<Configuracao> lconfig = daoConfiguracao.listar(Configuracao.class,
							"descricao='Código Contrato Atual'");
					if (lconfig.size() > 0) {
						Configuracao config = lconfig.get(0);
						Integer n = config.getUltimoCodigoContratoAtual();
						informacoesFinanceiras.setCodigoContratoAtual(n);
						config.setUltimoCodigoContratoAtual(n + 1);
						configuracaoService.inserirAlterar(config);
					} else {
						informacoesFinanceiras.setCodigoContratoAtual(
								(Integer.parseInt(String.valueOf(informacoesFinanceiras.getId())) + 1400));
					}
					informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
				}

				item.setContratoRenovado(informacoesFinanceiras);
				item.setSalvo(true);
				item.setMensagemErro(null);
				sucesso++;
			} catch (Exception e) {
				item.setSalvo(false);
				item.setMensagemErro(e.getMessage());
				e.printStackTrace();
				falha++;
			}
		}

		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(
				sucesso + " contrato(s) renovado(s) com sucesso" + (falha > 0 ? ", " + falha + " com erro." : "."));
		carregarLista();
	}

	public void inativar(InformacaoFinanceira t) {
		t.setStatus(false);
		t.setSituacaoContrato("Inativo");
		informacoesFinanceirasService.inserirAlterar(t);
		// informacoesFinanceirasService.update(" InformacaoFinanceira set status =
		// false where id = " +
		// t.getId());
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	// MEXENDO AQUI NO SALVAR PARCELAS...
	private void salvarParcelas() {
		int controle = 0;
		ContasReceber cr = new ContasReceber();
		for (ContasReceberParcelas p : listaContasReceberParcelas) {
			// primeiro salvar o contas receber e depois as parcelas.....
			if (controle == 0) {
				controle = 1;

				cr.setInformacaoFinanceira(informacoesFinanceiras);
				cr.setQuantidadeParcelas(p.getQuantidadeParcelas());
				cr.setStatus(true);
				cr.setDataCadastro(new Date());
				cr.setValorParcelas(p.getValorParcela());
				contaReceberService.inserirAlterar(cr);
			}
			p.setIdInformacaoFinanceiraUnico(informacoesFinanceiras.getIdInformacaoFinanceiraUnico());
			p.setContasReceber(cr);
			contasReceberParcelaService.inserirAlterar(p);
		}
	}

	public void atualizarMesAnoInicio() {
		List<InformacaoFinanceira> fi = daoInformacoesFinanceiras.listaComStatus(InformacaoFinanceira.class);
		for (InformacaoFinanceira f : fi) {

			Calendar pf = Calendar.getInstance();
			pf.setTime(f.getDataPrimeiroPagamento());
			int pri = pf.get(Calendar.MONTH);
			pf.set(Calendar.MONTH, (pri + f.getQuantidadeMesesContrato()));
			f.setDataUltimoPagamento(pf.getTime());

			DateFormat formataMes = new SimpleDateFormat("MM");
			f.setMesInicio(Integer.parseInt(formataMes.format(f.getDataInicial())));

			DateFormat formataAno = new SimpleDateFormat("yyyy");
			f.setAnoInicio(Integer.parseInt(formataAno.format(f.getDataInicial())));

			informacoesFinanceirasService.inserirAlterar(f);
		}
		ExibirMensagem.exibirMensagem("Finalizado a atualização do mes ano");
	}

	public void atualizarGrupoPontoColeta() {
		// System.out.println("Dentro da Lista de Inventáriossss");
		List<PontoColeta> lp = daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class, "");

		for (PontoColeta coleta : lp) {
			// preencher ponto de coleta
			List<ItensInformacaoFinanceiraPontoColeta> listaPontos = daoItensInformacaoFinanceiraPontoColeta
					.listar(ItensInformacaoFinanceiraPontoColeta.class, "pontoColeta.id=" + coleta.getId()
							+ " and informacoesFinanceiras.situacaoContrato='Ativo' and informacoesFinanceiras.status is true and informacoesFinanceiras.situacaoContrato='Ativo'");

			if (listaPontos.size() > 0) {
				if (listaPontos.get(0).getInformacoesFinanceiras() != null) {
					if (listaPontos.get(0).getInformacoesFinanceiras().getSituacaoContrato().equals("Ativo")) {
						List<ItensInformacaoFinanceiraGrupo> listaGrupos = daoItensGrupo
								.listar(ItensInformacaoFinanceiraGrupo.class,
										"informacoesFinanceiras.id="
												+ listaPontos.get(0).getInformacoesFinanceiras().getId()
												+ " order by grupoResiduos.ordem");
						for (ItensInformacaoFinanceiraGrupo g : listaGrupos) {
							if (coleta.getGruposColetados().equals("")) {
								coleta.setGruposColetados(g.getGrupoResiduos().getDescricao());
							} else {
								coleta.setGruposColetados(
										coleta.getGruposColetados() + ";" + g.getGrupoResiduos().getDescricao());
							}

						}
						coleta.setGruposColetados(coleta.getGruposColetados() + ";");
						pontoColetaService.inserirAlterar(coleta);

					} else {
						coleta.setContratoAtivo(false);
					}
				}
			}
		}

		ExibirMensagem.exibirMensagem("Finalizado Atualizar ponto Coleta gruposs coletados!!");
	}

	public void updateItensInformacaoFinanceiraPontoColeta() {
		List<ItensInformacaoFinanceiraPontoColeta> li = daoItensInformacaoFinanceiraPontoColeta
				.listaComStatus(ItensInformacaoFinanceiraPontoColeta.class);
		for (ItensInformacaoFinanceiraPontoColeta it : li) {
			PontoColeta pontoPeriodo = it.getPontoColeta();
			if (pontoPeriodo != null) {
				if (it.getInformacoesFinanceiras().getPeriodicidadeColeta() == null) {
					it.getInformacoesFinanceiras().setPeriodicidadeColeta("Quinzenal");
					informacoesFinanceirasService.inserirAlterar(it.getInformacoesFinanceiras());
				}

				if (it.getInformacoesFinanceiras().getPeriodicidadeColeta().equals("Semanal")) {
					pontoPeriodo.setQuantidadeColetaMes(4);
				} else if (it.getInformacoesFinanceiras().getPeriodicidadeColeta().equals("Quinzenal")) {
					pontoPeriodo.setQuantidadeColetaMes(2);
				} else {
					pontoPeriodo.setQuantidadeColetaMes(1);
				}
				pontoPeriodo.setPeriodicidadeColeta(it.getInformacoesFinanceiras().getPeriodicidadeColeta());

				pontoColetaService.inserirAlterar(pontoPeriodo);
			} else {
				System.out.println("Ponto NULOOO");
			}
		}

		ExibirMensagem.exibirMensagem("Finalizado Atualizado com sucesso!!!!!");
	}

	public void salvarAviso() {

		try {
			if (informacoesFinanceiras.getId() != null) {
				informacoesFinanceiras.setStatus(true);
				informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
			}

			for (ItensInformacaoFinanceiraPontoColeta it : listaItensInformacoesFinanceiraPontoColeta) {
				PontoColeta pontoPeriodo = it.getPontoColeta();

				pontoPeriodo.setSituacaoAviso(informacoesFinanceiras.getSituacaoAviso());
				pontoPeriodo.setAviso(informacoesFinanceiras.getAviso());

				pontoColetaService.inserirAlterar(pontoPeriodo);
			}

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogAvisos();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
		tabIndex = 0;

	}

	public void salvar(String imprimir) {

		if (!informacoesFinanceiras.getRomaneioContratoComDadosDo().equals("Ponto de Coleta")) {
			informacoesFinanceiras.setPontoColetaGerarContrato(null);
		}

		informacoesFinanceiras.setDataInicial(informacoesFinanceiras.getDataEmissaoContrato());

		Calendar c = Calendar.getInstance();
		c.setTime(informacoesFinanceiras.getDataInicial());
		int mes = c.get(Calendar.MONTH);
		c.set(Calendar.MONTH, (mes + informacoesFinanceiras.getQuantidadeMesesContrato()));
		informacoesFinanceiras.setDataFinal(c.getTime());

		Calendar pf = Calendar.getInstance();
		pf.setTime(informacoesFinanceiras.getDataPrimeiroPagamento());
		int pri = pf.get(Calendar.MONTH);
		pf.set(Calendar.MONTH, (pri + informacoesFinanceiras.getQuantidadeMesesContrato()));
		informacoesFinanceiras.setDataUltimoPagamento(pf.getTime());

		DateFormat formataMes = new SimpleDateFormat("MM");
		informacoesFinanceiras
				.setMesInicio(Integer.parseInt(formataMes.format(informacoesFinanceiras.getDataInicial())));

		DateFormat formataAno = new SimpleDateFormat("yyyy");
		informacoesFinanceiras
				.setAnoInicio(Integer.parseInt(formataAno.format(informacoesFinanceiras.getDataInicial())));

		boolean contratoNovo = informacoesFinanceiras.getId() == null;

		try {
			if (informacoesFinanceiras.getId() == null) {
				if (tipoContrato.equals("Novo Contrato")) {

					informacoesFinanceiras.setStatus(true);
					informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
					informacoesFinanceiras.setIdInformacaoFinanceiraUnico(informacoesFinanceiras.getId());
					informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
				} else {
					List<InformacaoFinanceira> li = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
							"idInformacaoFinanceiraUnico=" + informacoesFinanceiras.getIdInformacaoFinanceiraUnico());
					for (InformacaoFinanceira i : li) {
						i.setSituacaoContrato("Renovado");
						SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
						i.setObservacaoSituacaoContrato(i.getObservacaoSituacaoContrato()
								+ ". Contrato Renovado no dia " + format.format(new Date()));
						informacoesFinanceirasService.inserirAlterar(i);
					}
					informacoesFinanceiras.setStatus(true);
					informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
				}
				salvarItensGrupo();
				salvarItensPontoColeta();
				salvarParcelas();
			} else {
				informacoesFinanceiras.setStatus(true);
				informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
			}

			gruposColetados();

			for (ItensInformacaoFinanceiraPontoColeta it : listaItensInformacoesFinanceiraPontoColeta) {
				PontoColeta pontoPeriodo = it.getPontoColeta();
				if (informacoesFinanceiras.getPeriodicidadeColeta().equals("Semanal")) {
					pontoPeriodo.setQuantidadeColetaMes(4);
				} else if (informacoesFinanceiras.getPeriodicidadeColeta().equals("Quinzenal")) {
					pontoPeriodo.setQuantidadeColetaMes(2);
				} else {
					pontoPeriodo.setQuantidadeColetaMes(1);
				}
				pontoPeriodo.setPeriodicidadeColeta(informacoesFinanceiras.getPeriodicidadeColeta());

				// Coleta Única: um novo contrato (Novo Contrato ou renovação) representa
				// um novo pedido de coleta. Zera a última coleta para que o ponto volte a
				// aparecer como pendente no app dos motoristas, sem depender de baixa manual.
				if (contratoNovo && "Coleta Única".equals(informacoesFinanceiras.getPeriodicidadeColeta())) {
					pontoPeriodo.setDiaUltimaColeta(0);
					pontoPeriodo.setMesUltimaColeta(0);
					pontoPeriodo.setAnoUltimaColeta(0);
					pontoPeriodo.setDataUltimaColeta(null);
					pontoPeriodo.setPrimeiraColeta(true);
					pontoPeriodo.setDataUltimaAlteracao(new Date());
				}

				pontoColetaService.inserirAlterar(pontoPeriodo);
			}

			Long idInfS = informacoesFinanceiras.getId();

			if (informacoesFinanceiras.getCodigoContratoAtual() == null
					|| informacoesFinanceiras.getCodigoContratoAtual().equals(0)
					|| informacoesFinanceiras.getCodigoContratoAtual() < 1) {
				List<Configuracao> lconfig = daoConfiguracao.listar(Configuracao.class,
						"descricao='Código Contrato Atual'");
				if (lconfig.size() > 0) {
					Configuracao config = lconfig.get(0);
					Integer n = config.getUltimoCodigoContratoAtual();
					informacoesFinanceiras.setCodigoContratoAtual(n);
					n = n + 1;
					config.setUltimoCodigoContratoAtual(n);
					configuracaoService.inserirAlterar(config);
				} else {
					informacoesFinanceiras.setCodigoContratoAtual((Integer.parseInt(String.valueOf(idInfS)) + 1400));
				}
				informacoesFinanceirasService.inserirAlterar(informacoesFinanceiras);
			}
			//System.out.println("ANTES SALVAR");
			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogInformacoesFinanceiras();
			carregarLista();
			if (imprimir.equals("sim") || imprimir.equals("salvarImprimir")) {

				informacoesFinanceirasRenovada = daoInformacoesFinanceiras.buscarPorId(InformacaoFinanceira.class, idInfS);
				//System.out.println("SIM SALVAR");
				if (informacoesFinanceirasRenovada != null && imprimir.equals("sim")) {
					RequestContext.getCurrentInstance().execute("PF('dlgVisualizarContinuar').show();");
					RequestContext.getCurrentInstance().update("frmVisualizarContinuar");
				}else {
					RequestContext.getCurrentInstance().update("frmTabela");
					RequestContext.getCurrentInstance().update(":frmTabela");
					imprimirContrato(informacoesFinanceirasRenovada);
				}
			} else {
				informacoesFinanceirasRenovada = null;
			}

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
		tabIndex = 0;

	}

	/** por enquanto, retorna pelo nome/razão social do cliente **/
	public List<InformacaoFinanceira> completarInformacaoFinanceira(String str) {
		List<InformacaoFinanceira> list = daoInformacoesFinanceiras.listaComStatus(InformacaoFinanceira.class);
		List<InformacaoFinanceira> selecionados = new ArrayList<>();
		for (InformacaoFinanceira cur : list) {
			if (cur.getCliente().getNomeRazaoSocialNomeFantasia().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		contasReceber = new ContasReceber();
		informacoesFinanceiras = new InformacaoFinanceira();
		informacoesFinanceirasAntiga = new InformacaoFinanceira();
		listaItensInformacoesFinanceiraGrupo = new ArrayList<>();
		listaItensInformacoesFinanceiraPontoColeta = new ArrayList<>();
		itemInformacaoFinanceiraPontoColeta = new ItensInformacaoFinanceiraPontoColeta();
		itemInformacaoFinanceiraGrupo = new ItensInformacaoFinanceiraGrupo();
		tabIndex = 0;
		tipoContrato = "Novo Contrato";
		setarDiaPagamento();
	}

	public void carregarLista() {
		//listInformacoesFinanceiras = daoInformacoesFinanceiras.listar(InformacaoFinanceira.class,
		//		"situacaoContrato='Ativo' order by dataFinal asc");
		buscarContratosVencimento();
	}

	public InformacaoFinanceira getInformacoesFinanceiras() {
		return informacoesFinanceiras;
	}

	public void setInformacoesFinanceiras(InformacaoFinanceira informacoesFinanceiras) {
		this.informacoesFinanceiras = informacoesFinanceiras;
	}

	public List<InformacaoFinanceira> getInformacoesFinanceirasBusca() {
		return informacoesFinanceirasBusca;
	}

	public void setInformacoesFinanceirasBusca(List<InformacaoFinanceira> informacoesFinanceirasBusca) {
		this.informacoesFinanceirasBusca = informacoesFinanceirasBusca;
	}

	public List<InformacaoFinanceira> getListInformacoesFinanceiras() {
		return listInformacoesFinanceiras;
	}

	public void setListInformacoesFinanceiras(List<InformacaoFinanceira> listInformacoesFinanceiras) {
		this.listInformacoesFinanceiras = listInformacoesFinanceiras;
	}

	public List<InformacaoFinanceira> getInformacoesFinanceirasSelecionadas() {
		return informacoesFinanceirasSelecionadas;
	}

	public void setInformacoesFinanceirasSelecionadas(List<InformacaoFinanceira> informacoesFinanceirasSelecionadas) {
		this.informacoesFinanceirasSelecionadas = informacoesFinanceirasSelecionadas;
	}

	public List<RenovacaoLoteItem> getListaRenovacaoLote() {
		return listaRenovacaoLote;
	}

	public void setListaRenovacaoLote(List<RenovacaoLoteItem> listaRenovacaoLote) {
		this.listaRenovacaoLote = listaRenovacaoLote;
	}

	public PontoColeta getPontoColeta() {
		return pontoColeta;
	}

	public void setPontoColeta(PontoColeta pontoColeta) {
		this.pontoColeta = pontoColeta;
	}

	public List<PontoColeta> getPontosColetas() {
		return pontosColetas;
	}

	public void setPontosColetas(List<PontoColeta> pontosColetas) {
		this.pontosColetas = pontosColetas;
	}

	public GrupoResiduo getGrupoResiduo() {
		return grupoResiduo;
	}

	public void setGrupoResiduo(GrupoResiduo grupoResiduo) {
		this.grupoResiduo = grupoResiduo;
	}

	public List<GrupoResiduo> getGruposRediduos() {
		return gruposRediduos;
	}

	public void setGruposRediduos(List<GrupoResiduo> gruposRediduos) {
		this.gruposRediduos = gruposRediduos;
	}

	public ItensInformacaoFinanceiraPontoColeta getItemInformacaoFinanceiraPontoColeta() {
		return itemInformacaoFinanceiraPontoColeta;
	}

	public void setItemInformacaoFinanceiraPontoColeta(
			ItensInformacaoFinanceiraPontoColeta itemInformacaoFinanceiraPontoColeta) {
		this.itemInformacaoFinanceiraPontoColeta = itemInformacaoFinanceiraPontoColeta;
	}

	public List<ItensInformacaoFinanceiraPontoColeta> getListaItensInformacoesFinanceiraPontoColeta() {
		return listaItensInformacoesFinanceiraPontoColeta;
	}

	public void setListaItensInformacoesFinanceiraPontoColeta(
			List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesFinanceiraPontoColeta) {
		this.listaItensInformacoesFinanceiraPontoColeta = listaItensInformacoesFinanceiraPontoColeta;
	}

	public ItensInformacaoFinanceiraGrupo getItemInformacaoFinanceiraGrupo() {
		return itemInformacaoFinanceiraGrupo;
	}

	public void setItemInformacaoFinanceiraGrupo(ItensInformacaoFinanceiraGrupo itemInformacaoFinanceiraGrupo) {
		this.itemInformacaoFinanceiraGrupo = itemInformacaoFinanceiraGrupo;
	}

	public List<ItensInformacaoFinanceiraGrupo> getListaItensInformacoesFinanceiraGrupo() {
		return listaItensInformacoesFinanceiraGrupo;
	}

	public void setListaItensInformacoesFinanceiraGrupo(
			List<ItensInformacaoFinanceiraGrupo> listaItensInformacoesFinanceiraGrupo) {
		this.listaItensInformacoesFinanceiraGrupo = listaItensInformacoesFinanceiraGrupo;
	}

	public int getTabIndex() {
		return tabIndex;
	}

	public void setTabIndex(int tabIndex) {
		this.tabIndex = tabIndex;
	}

	public List<InformacaoFinanceira> getListInformacoesFinanceirasCliente() {
		return listInformacoesFinanceirasCliente;
	}

	public void setListInformacoesFinanceirasCliente(List<InformacaoFinanceira> listInformacoesFinanceirasCliente) {
		this.listInformacoesFinanceirasCliente = listInformacoesFinanceirasCliente;
	}

	public Date getDataVencimentoInicial() {
		return dataVencimentoInicial;
	}

	public void setDataVencimentoInicial(Date dataVencimentoInicial) {
		this.dataVencimentoInicial = dataVencimentoInicial;
	}

	public Date getDataVencimentoFinal() {
		return dataVencimentoFinal;
	}

	public void setDataVencimentoFinal(Date dataVencimentoFinal) {
		this.dataVencimentoFinal = dataVencimentoFinal;
	}

	public List<InformacaoFinanceira> getListInformacoesFinanceirasVencidas() {
		return listInformacoesFinanceirasVencidas;
	}

	public void setListInformacoesFinanceirasVencidas(List<InformacaoFinanceira> listInformacoesFinanceirasVencidas) {
		this.listInformacoesFinanceirasVencidas = listInformacoesFinanceirasVencidas;
	}

	public List<InformacaoFinanceira> getListInformacoesFinanceirasVencer() {
		return listInformacoesFinanceirasVencer;
	}

	public void setListInformacoesFinanceirasVencer(List<InformacaoFinanceira> listInformacoesFinanceirasVencer) {
		this.listInformacoesFinanceirasVencer = listInformacoesFinanceirasVencer;
	}

	public List<ContasReceberParcelas> getListaContasReceberParcelas() {
		return listaContasReceberParcelas;
	}

	public void setListaContasReceberParcelas(List<ContasReceberParcelas> listaContasReceberParcelas) {
		this.listaContasReceberParcelas = listaContasReceberParcelas;
	}

	public ContasReceber getContasReceber() {
		return contasReceber;
	}

	public void setContasReceber(ContasReceber contasReceber) {
		this.contasReceber = contasReceber;
	}

	public ContasReceberParcelas getContasReceberParcela() {
		return contasReceberParcela;
	}

	public void setContasReceberParcela(ContasReceberParcelas contasReceberParcela) {
		this.contasReceberParcela = contasReceberParcela;
	}

	public String getTipoContrato() {
		return tipoContrato;
	}

	public void setTipoContrato(String tipoContrato) {
		this.tipoContrato = tipoContrato;
	}

	public List<InformacaoFinanceira> getListaInformacoesFinanceirasHistorico() {
		return listaInformacoesFinanceirasHistorico;
	}

	public void setListaInformacoesFinanceirasHistorico(
			List<InformacaoFinanceira> listaInformacoesFinanceirasHistorico) {
		this.listaInformacoesFinanceirasHistorico = listaInformacoesFinanceirasHistorico;
	}

	public InformacaoFinanceira getInformacoesFinanceirasAntiga() {
		return informacoesFinanceirasAntiga;
	}

	public void setInformacoesFinanceirasAntiga(InformacaoFinanceira informacoesFinanceirasAntiga) {
		this.informacoesFinanceirasAntiga = informacoesFinanceirasAntiga;
	}

	public InformacaoFinanceira getInformacoesFinanceirasRenovada() {
		return informacoesFinanceirasRenovada;
	}

	public void setInformacoesFinanceirasRenovada(InformacaoFinanceira informacoesFinanceirasRenovada) {
		this.informacoesFinanceirasRenovada = informacoesFinanceirasRenovada;
	}

	public String getSituacaoConsulta() {
		return situacaoConsulta;
	}

	public void setSituacaoConsulta(String situacaoConsulta) {
		this.situacaoConsulta = situacaoConsulta;
	}
	
	public String getTipoCobrancaConsulta() {
	    return tipoCobrancaConsulta;
	}

	public void setTipoCobrancaConsulta(String tipoCobrancaConsulta) {
	    this.tipoCobrancaConsulta = tipoCobrancaConsulta;
	}

}
