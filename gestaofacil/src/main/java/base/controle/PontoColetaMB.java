package base.controle;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.primefaces.event.ReorderEvent;

import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.FechamentoMes;
import base.modelo.PontoColeta;
import base.modelo.Rota;
import base.service.PontoColetaService;
import util.ChamarRelatorio;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("pontoColetaMB")
public class PontoColetaMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private PontoColeta pontoColeta;
	private Integer ordemRotaSelecionada;
	private List<PontoColeta> pontoColetaBusca;
	private List<PontoColeta> pontosColetaAlterarRota;
	private List<PontoColeta> listPontoColeta;
	private Integer tabAtiva = 0;
	private Rota rotaSelecionada;
	private Long idRotaSelecionada = 0L;

	private Cidade cidadeBusca;
	private Rota rotaBusca;
	private String tipoBusca = "";
	private List<FechamentoMes> listaFechamentoMes;

	private String modeloCadastroPonto = "";

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta; // faz as buscas

	@Inject
	private GenericDAO<FechamentoMes> daoFechamentoMes; // faz as buscas

	@Inject
	private GenericDAO<Cliente> daoCliente; // faz as buscas

	@Inject
	private PontoColetaService pontoColetaService; // inserir no banco

	@Inject
	private EntityManager manager;

	@PostConstruct
	public void inicializar() {

		pontoColeta = new PontoColeta();

		listPontoColeta = new ArrayList<>();

		pontoColetaBusca = new ArrayList<>();
		listarPontosColeta(null);

	}

	public void removerAcentos() {
		List<PontoColeta> lc = daoPontoColeta.listaComStatus(PontoColeta.class);
		for (PontoColeta c : lc) {
			c.setNomeRazaoSocial(
					Normalizer.normalize(c.getNomeRazaoSocial(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			c.setNomeFantasia(
					Normalizer.normalize(c.getNomeFantasia(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			pontoColetaService.inserirAlterar(c);
		}

		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}
	
	public void removerAcentosEndereco() {
		List<PontoColeta> lc = daoPontoColeta.listaComStatus(PontoColeta.class);
		for (PontoColeta c : lc) {
			
			c.setEndereco(
					Normalizer.normalize(c.getEndereco(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			pontoColetaService.inserirAlterar(c);
		}

		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}

	public void atualizarSorrifacil() {
		PontoColeta p = daoPontoColeta.buscarPorId(PontoColeta.class, 1498L);
		if (p != null) {
//			System.out.println(p.getStatus());
			p.setStatus(true);
			pontoColetaService.inserirAlterar(p);
//			System.out.println("atualizado");
			ExibirMensagem.exibirMensagem("Atualizado!!");
		}

	}

	public void atualizarPhitomedica() {
		PontoColeta p = daoPontoColeta.buscarPorId(PontoColeta.class, 622L);
		if (p != null) {
//			System.out.println(p.getStatus());
			p.setStatus(true);
			pontoColetaService.inserirAlterar(p);
//			System.out.println("atualizado");
			ExibirMensagem.exibirMensagem("Atualizado!!");
		}

	}

	public void onRowReorder(ReorderEvent event) {
//		FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO,
//				"From: " + event.getFromIndex() + ", To:" + event.getToIndex(),
//				"From: " + event.getFromIndex() + ", To:" + event.getToIndex());
//		FacesContext.getCurrentInstance().addMessage(null, msg);
//		for (PontoColeta p : pontoColetaBusca) {
//			System.out.print(p.getId() + ", ");
//		}

		if (tipoBusca.equals("rotaCidade")) {
			for (int x = 0; x < pontoColetaBusca.size(); x++) {
				PontoColeta p = pontoColetaBusca.get(x);
				p.setOrdem((x + 1));
				pontoColetaService.inserirAlterar(p);
			}

			ExibirMensagem.exibirMensagem("Ordens alteradas!!");
		} else {
			ExibirMensagem.exibirMensagemErro("Para reordenar é necessário efetuar a busca por Rota e Cidade!!");
		}

		System.out.println("Nova Linha");
	}

	public void gerarRelatorioPontosCidade() {

//		HashMap parametro = new HashMap<>();
//		parametro.put("ID_FECHAMENTO_MES", fec.getId());
		ChamarRelatorio ch = new ChamarRelatorio("relatorio_pontos_coleta_cidade.jasper", null,
				"relatorio_pontos_coleta_cidade");
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void gerarRelatorioPontosRotas() {

//		HashMap parametro = new HashMap<>();
//		parametro.put("ID_FECHAMENTO_MES", fec.getId());
		ChamarRelatorio ch = new ChamarRelatorio("pontos_coleta_rotas.jasper", null, "pontos_coleta_rotas");
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void buscarPontosRotas(String tipo) {
		tipoBusca = tipo;
		pontoColetaBusca = new ArrayList<>();
		if (tipo.equals("tudo")) {
			pontoColetaBusca = daoPontoColeta.listar(PontoColeta.class,
					"cidade.id=" + cidadeBusca.getId() + " order by ordem asc");
		} else if (tipo.equals("rota")) {
			pontoColetaBusca = daoPontoColeta.listar(PontoColeta.class,
					"rota.id=" + rotaBusca.getId() + " order by cidade.nome, ordem asc");
		} else {
			pontoColetaBusca = daoPontoColeta.listar(PontoColeta.class,
					"cidade.id=" + cidadeBusca.getId() + "and rota.id=" + rotaBusca.getId() + " order by ordem asc");
		}
	}

	public void alterarOrdemRota() {
		int controle = 0;
		int contador = 1;

		pontoColetaService.inserirAlterar(pontoColeta);
		contador = pontoColeta.getOrdem();
		// RECUPERAR TODOS OS PONTOS ONDE A ORDEM É MAIOR OU IGUAL AO ALTERADO REMOVENDO
		// ELE PRÓPRIO.
		// INCREMENTAR 1 EM TODOS

		for (int x = 0; x < listPontoColeta.size(); x++) {
			PontoColeta pc = listPontoColeta.get(x);

			if (pc.getOrdem() >= pontoColeta.getOrdem() && !pontoColeta.getId().equals(pc.getId())
					&& pc.getRota() != null && pontoColeta.getRota() != null
					&& pc.getRota().getId().equals(pontoColeta.getRota().getId())) {
				contador++;
				pc.setOrdem(contador);
				controle = 1;

				pontoColetaService.inserirAlterar(pc);

			}

		}
		if (controle == 1) {
			FecharDialog.fecharDialogAlterarOrdemRotaPontoColeta();
			listarPontosColeta(null);
		}
	}

	public void alterarRota() {
		int controle = 0;

		for (PontoColeta p : pontosColetaAlterarRota) {

			if (p.getRota() != null) {
				idRotaSelecionada = p.getRota().getId();
				ordemRotaSelecionada = p.getOrdem();
			}

			p.setRota(pontoColeta.getRota());
			pontoColeta = p;

			if (pontoColeta != null) {

				if (pontoColeta.getRota() != null && !idRotaSelecionada.equals(pontoColeta.getRota().getId())) {
					// System.out.println("Hereee");
					controle = 1;
					pontoColeta.setOrdem(1);
					pontoColeta.setNomeRota(pontoColeta.getRota().getNome());
					pontoColetaService.inserirAlterar(pontoColeta);

					List<PontoColeta> lpc = daoPontoColeta.listar(PontoColeta.class,
							"rota.id=" + idRotaSelecionada + " and ordem>=" + ordemRotaSelecionada);
					for (PontoColeta pc : lpc) {
						// System.out.println("aaa");
						if (pc.getOrdem() > 1) {
							pc.setOrdem((pc.getOrdem() - 1));
							pontoColetaService.inserirAlterar(pc);
						}
					}

				}
			}
		}
		if (controle == 1) {
			pontosColetaAlterarRota = new ArrayList<>();
			listPontoColeta = new ArrayList<>();
			ExibirMensagem.exibirMensagem("Rota alterada com sucesso!!");
			FecharDialog.fecharDialogAlterarRotaPontoColeta();
		}
		listarPontosColeta(null);
		criarNovoObjeto();
	}

	public void listarPontosColeta(Long id) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
		if (session.getAttribute("idCliente") != null) {
			listPontoColeta = daoPontoColeta.listar(PontoColeta.class,
					"cliente.id=" + session.getAttribute("idCliente"));
			session.removeAttribute("idCliente");
		} else if (id != null) {
			listPontoColeta = daoPontoColeta.listar(PontoColeta.class,
					"cliente.id=" + id + " order by nomeRota, ordem asc");
		} else {

			listPontoColeta = daoPontoColeta.listaComStatus(PontoColeta.class, " nomeRota, ordem asc");
		}

	}

	public void selecionarFormaCadastro() {
		modeloCadastroPonto="";
		// pontoColeta = new PontoColeta();
		// pontoColeta.setModeloCadastroPonto(modeloCadastroPonto);
	}

	public void cadastrarPontoColetaDadosCliente() {
		//System.out.println("AQui" + modeloCadastroPonto + " " + pontoColeta.getCliente().getId());
		if (pontoColeta != null && pontoColeta.getCliente() != null) {
			Cliente c = pontoColeta.getCliente();
			pontoColeta = new PontoColeta();
			pontoColeta.setModeloCadastroPonto(modeloCadastroPonto);
			pontoColeta.setCliente(c);
		}else {
			ExibirMensagem.exibirMensagemErro("Selecione o Cliente");
		}
		if (pontoColeta != null && pontoColeta.getCliente() != null
				&& modeloCadastroPonto.equals("Ponto de Coleta com os Dados do Cliente")) {
			List<PontoColeta> lp = daoPontoColeta.listar(PontoColeta.class,
					"cliente.id=" + pontoColeta.getCliente().getId()
							+ " and modeloCadastroPonto='Ponto de Coleta com os Dados do Cliente'");
			
			if (lp.size() < 1) {
				
				this.setarDadosPontoColeta();
			} else {
			
				ExibirMensagem.exibirMensagem("Já existe um ponto cadastrado com os dados deste cliente");
				pontoColeta = new PontoColeta();
			}
		}
	}

	private void setarDadosPontoColeta() {
		Cliente cliente = daoCliente.buscarPorId(Cliente.class, pontoColeta.getCliente().getId());
		pontoColeta.setCep(cliente.getCep());
		pontoColeta.setCidade(cliente.getCidade());
		pontoColeta.setCpfCnpj(cliente.getCpfCnpj());
		pontoColeta.setDiaVencimentoFatura(cliente.getDiaVencimentoFatura());
		pontoColeta.setEmail(cliente.getEmail());
		pontoColeta.setEndereco(cliente.getEndereco());
		if (pontoColeta.getId() == null) {
			pontoColeta.setModeloCadastroPonto(modeloCadastroPonto);
			pontoColeta.setTipo("Ponto Coleta");
		}
		pontoColeta.setNomeFantasia(cliente.getNomeFantasia());
		pontoColeta.setNomeRazaoSocial(cliente.getNomeRazaoSocial());
		pontoColeta.setNumero(cliente.getNumero());
		pontoColeta.setResponsavel(cliente.getResponsavel());
		pontoColeta.setRgInscricaoEstadual(cliente.getRgInscricaoEstadual());
		pontoColeta.setTelefone1(cliente.getTelefone1());
		pontoColeta.setTelefone2(cliente.getTelefone2());
		pontoColeta.setTipoPessoa(cliente.getTipoPessoa());
		pontoColeta.setBairro(cliente.getBairro());
		
		pontoColeta.setRamoAtividade(cliente.getRamoAtividade());

		pontoColeta.setEdificio(cliente.getEdificio());
		pontoColeta.setSala(cliente.getSala());
		pontoColeta.setAndar(cliente.getAndar());
		pontoColeta.setComplemento(cliente.getComplemento());
		pontoColeta.setObservacao(cliente.getObservacao());
		pontoColeta.setEmail2(cliente.getEmail2());
		pontoColeta.setApelido(cliente.getApelido());
		pontoColeta.setRazaoSocialCliente(cliente.getNomeRazaoSocial());

		pontoColeta.setCpfResponsavel(cliente.getCpfResponsavel());
		pontoColeta.setRgResponsavel(cliente.getRgResponsavel());
		pontoColeta.setOrgaoEmissorRgResponsavel(cliente.getOrgaoEmissorRgResponsavel());
		pontoColeta.setEnderecoResponsavel(cliente.getEnderecoResponsavel());
		pontoColeta.setCidadeResponsavel(cliente.getCidadeResponsavel());
	}

//	public void preencherListaPontosColeta(Cliente t) {
//		pontosColeta = daoPontoColeta.listar(PontoColeta.class, "cliente.id=" + this.cliente.getId());
//	}

	public void mostrar() {
		System.out.println("this: " + this.pontoColeta.getCliente().getTipoPessoa());
	}

	public void preencherLista(PontoColeta t) {

		this.pontoColeta = t;
		modeloCadastroPonto = "";
		if (t.getRota() != null) {
			idRotaSelecionada = t.getRota().getId();
			ordemRotaSelecionada = t.getOrdem();
		}
	}

	public List<PontoColeta> completar(String str) {
		if (str == null) {
			str = "";
		}
		List<PontoColeta> list = daoPontoColeta.listaComStatus(PontoColeta.class);
		List<PontoColeta> selecionados = new ArrayList<>();
		for (PontoColeta cur : list) {
			if (cur.getNomeRazaoSocial().toLowerCase().contains(str)
					|| cur.getNomeFantasia().toLowerCase().contains(str)) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void inativar(PontoColeta t) {
		t.setStatus(false);
		pontoColetaService.inserirAlterar(t);
		// pontoColetaService.update(" PontoColeta set status = false where id = " +
		// t.getId());
		listarPontosColeta(t.getCliente().getId());
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);

	}

	public void atualizarRazaoPonto() {
		for (PontoColeta p : listPontoColeta) {
			p.setRazaoSocialCliente(p.getCliente().getNomeRazaoSocial());
			pontoColetaService.inserirAlterar(p);
		}
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}

	public void salvar() {
		pontoColeta.setDataUltimaAlteracao(new Date());
		pontoColeta.setNomeRazaoSocial(Normalizer.normalize(pontoColeta.getNomeRazaoSocial(), Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", ""));
		pontoColeta.setNomeFantasia(Normalizer.normalize(pontoColeta.getNomeFantasia(), Normalizer.Form.NFD)
				.replaceAll("[^\\p{ASCII}]", ""));
		pontoColeta.setEndereco(Normalizer.normalize(pontoColeta.getEndereco(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));

		if (pontoColeta != null && pontoColeta.getCliente() != null
				&& pontoColeta.getModeloCadastroPonto().equals("Ponto de Coleta com os Dados do Cliente")) {
			this.setarDadosPontoColeta();
		}

		try {
			if (pontoColeta.getId() == null) {
				pontoColeta.setRazaoSocialCliente(pontoColeta.getCliente().getNomeRazaoSocial());
				pontoColeta.setModeloCadastroPonto(modeloCadastroPonto);
				pontoColeta.setStatus(true);
				pontoColetaService.inserirAlterar(pontoColeta);

			} else {
				pontoColeta.setRazaoSocialCliente(pontoColeta.getCliente().getNomeRazaoSocial());
				pontoColeta.setStatus(true);
				pontoColetaService.inserirAlterar(pontoColeta);
			}
			listarPontosColeta(pontoColeta.getCliente().getId());
			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogPontoColeta();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}

	}

	public void criarNovoObjeto() {
		pontoColeta = new PontoColeta();
		modeloCadastroPonto = "";
		tabAtiva = 0;
	}

	public List<PontoColeta> getPontoColetaBusca() {
		return pontoColetaBusca;
	}

	public void setPontoColetaBusca(List<PontoColeta> pontoColetaBusca) {
		this.pontoColetaBusca = pontoColetaBusca;
	}

	public List<PontoColeta> getListPontoColeta() {
		return listPontoColeta;
	}

	public void setListPontoColeta(List<PontoColeta> listPontoColeta) {
		this.listPontoColeta = listPontoColeta;
	}

	public PontoColeta getPontoColeta() {
		return pontoColeta;
	}

	public void setPontoColeta(PontoColeta pontoColeta) {
		this.pontoColeta = pontoColeta;
	}

	public String getModeloCadastroPonto() {
		return modeloCadastroPonto;
	}

	public void setModeloCadastroPonto(String modeloCadastroPonto) {
		this.modeloCadastroPonto = modeloCadastroPonto;
	}

	public Integer getTabAtiva() {
		return tabAtiva;
	}

	public void setTabAtiva(Integer tabAtiva) {
		this.tabAtiva = tabAtiva;
	}

	public Rota getRotaSelecionada() {
		return rotaSelecionada;
	}

	public void setRotaSelecionada(Rota rotaSelecionada) {
		this.rotaSelecionada = rotaSelecionada;
	}

	public List<PontoColeta> getPontosColetaAlterarRota() {
		return pontosColetaAlterarRota;
	}

	public void setPontosColetaAlterarRota(List<PontoColeta> pontosColetaAlterarRota) {
		this.pontosColetaAlterarRota = pontosColetaAlterarRota;
	}

	public Cidade getCidadeBusca() {
		return cidadeBusca;
	}

	public void setCidadeBusca(Cidade cidadeBusca) {
		this.cidadeBusca = cidadeBusca;
	}

	public Rota getRotaBusca() {
		return rotaBusca;
	}

	public void setRotaBusca(Rota rotaBusca) {
		this.rotaBusca = rotaBusca;
	}

}
