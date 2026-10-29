package base.controle;

import java.io.Serializable;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.primefaces.PrimeFaces;
import org.primefaces.event.TabChangeEvent;

import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.FechamentoMes;
import base.modelo.InformacaoFinanceira;
import base.modelo.PontoColeta;
import base.modelo.Rota;
import base.service.ClienteService;
import base.service.FechamentoMesService;
import base.service.PontoColetaService;
import util.CriptografiaSenha;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("clienteMB")
public class ClienteMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Cliente cliente;
	private List<Cliente> clienteBusca;
	private List<Cliente> listCliente;
	private List<PontoColeta> pontosColeta;
	private boolean definirPontoColeta = true;
	private int tabAtiva = 0;
	private Rota rotaSelecionada;
	private List<InformacaoFinanceira> listaInformacaoFinanceira;
	List<FechamentoMes> listaFechamentoMes = new ArrayList<>();
	private String novaSenhaAdmin;

	@Inject
	private GenericDAO<InformacaoFinanceira> daoInformacaoFinanceira; 

	@Inject
	private GenericDAO<Cliente> daoCliente; 

	@Inject
	private GenericDAO<PontoColeta> daoPontosColeta; 

	@Inject
	private ClienteService clienteService; 

	@Inject
	private PontoColetaService pontoColetaService; 
	
	@Inject
	private FechamentoMesService fechamentoMesService; 

	@PostConstruct
	public void inicializar() {
		cliente = new Cliente();
		listCliente = new ArrayList<>();
		carregarLista();
		clienteBusca = new ArrayList<>();
	}
	
	public void buscarFechamentoMesCliente(Long idCliente) {
		listaFechamentoMes = fechamentoMesService.buscarFechamentoMes(idCliente);
	}
	
	public void removerAcentos() {
		List<Cliente> lc = daoCliente.listaComStatus(Cliente.class);
		for(Cliente c:lc) {
			c.setNomeRazaoSocial(Normalizer.normalize(c.getNomeRazaoSocial(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			c.setNomeFantasia(Normalizer.normalize(c.getNomeFantasia(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			clienteService.inserirAlterar(c);
		}
		
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}
	
	public void removerAcentosEndereco() {
		List<Cliente> lc = daoCliente.listaComStatus(Cliente.class);
		for(Cliente c:lc) {
			c.setEndereco(Normalizer.normalize(c.getEndereco(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
			
			clienteService.inserirAlterar(c);
		}
		
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}
	
	public void atualizarRamoAtividade() {
		List<Cliente>  cl = daoCliente.listaComStatus(Cliente.class);
		for(Cliente c:cl) {
			c.setRamoAtividade(c.getObservacao());
			clienteService.inserirAlterar(c);
		}
		ExibirMensagem.exibirMensagem("raMO DE ativiade atualizado");
	}
	
	public void buscarInformacaoFinanceira(Long idCliente) {
		listaInformacaoFinanceira = daoInformacaoFinanceira.listar(InformacaoFinanceira.class, "cliente.id="+idCliente+" order by dataInicial asc");
		
	}

	public void definirPonto() {
		System.out.println("Definir: " + definirPontoColeta);
	}

	public void preencherLista(Cliente t) {
		// PrimeFaces.current().resetInputs("frmModalSalvar");
		this.cliente = t;
	}
	
	public String chamarPontosColeta(Cliente t) {
		// PrimeFaces.current().resetInputs("frmModalSalvar");
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);
		session.setAttribute("idCliente", t.getId());
		return "pontosColeta";
	}
	


	public void preencherListaPontosColeta(Cliente t) {
		this.cliente = t;
		pontosColeta = daoPontosColeta.listar(PontoColeta.class, "cliente.id=" + this.cliente.getId());
	}

	public List<Cliente> completar(String str) {
		List<Cliente> list = daoCliente.listaComStatus(Cliente.class);
		List<Cliente> selecionados = new ArrayList<>();
		for (Cliente cur : list) {
			if (cur.getNomeRazaoSocial().toLowerCase().contains(str.toLowerCase())
					|| cur.getNomeFantasia().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void inativar(Cliente t) {
		t.setStatus(false);
		clienteService.inserirAlterar(t);
		// clienteService.update(" Cliente set status = false where id = " +
		// t.getId());
		criarNovoObjeto("Física");
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		
		cliente.setNomeRazaoSocial(Normalizer.normalize(cliente.getNomeRazaoSocial(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
		cliente.setNomeFantasia(Normalizer.normalize(cliente.getNomeFantasia(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
		cliente.setEndereco(Normalizer.normalize(cliente.getEndereco(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));

		try {
			if (cliente != null && cliente.getCpfCnpj() != null && !cliente.getCpfCnpj().equals("000.000.000-00")
					&& !cliente.getCpfCnpj().equals("00.000.000/0000-00")) {
				//System.out.println("CLiente IF: " + cliente.getId());
				if (cliente.getId() == null) {
					List<Cliente> list = daoCliente.listar(Cliente.class, "cpfCnpj='" + cliente.getCpfCnpj() + "'");
					if (list.size() < 1) {

						cliente.setTipo("Cliente");
						cliente.setStatus(true);
						clienteService.inserirAlterar(cliente);
						if (definirPontoColeta == true) {
							PontoColeta pontoColeta = new PontoColeta();
							pontoColeta.setDataUltimaAlteracao(new Date());
							pontoColeta.setStatus(true);
							pontoColeta.setCliente(cliente);
							pontoColeta.setCep(cliente.getCep());
							pontoColeta.setCidade(cliente.getCidade());
							pontoColeta.setCpfCnpj(cliente.getCpfCnpj());
							pontoColeta.setDiaVencimentoFatura(cliente.getDiaVencimentoFatura());
							pontoColeta.setEmail(cliente.getEmail());
							pontoColeta.setEndereco(cliente.getEndereco());
							pontoColeta.setModeloCadastroPonto("Ponto de Coleta com os Dados do Cliente");
							pontoColeta.setTipo("Ponto Coleta");
							pontoColeta.setNomeFantasia(cliente.getNomeFantasia());
							pontoColeta.setNomeRazaoSocial(cliente.getNomeRazaoSocial());
							pontoColeta.setNumero(cliente.getNumero());
							pontoColeta.setResponsavel(cliente.getResponsavel());
							pontoColeta.setRgInscricaoEstadual(cliente.getRgInscricaoEstadual());
							pontoColeta.setTelefone1(cliente.getTelefone1());
							pontoColeta.setTelefone2(cliente.getTelefone2());
							pontoColeta.setTipoPessoa(cliente.getTipoPessoa());
							pontoColeta.setBairro(cliente.getBairro());
							pontoColeta.setEdificio(cliente.getEdificio());
							pontoColeta.setSala(cliente.getSala());
							pontoColeta.setAndar(cliente.getAndar());
							pontoColeta.setComplemento(cliente.getComplemento());
							pontoColeta.setObservacao(cliente.getObservacao());
							pontoColeta.setEmail2(cliente.getEmail2());
							pontoColeta.setEmail3(cliente.getEmail3());
							pontoColeta.setApelido(cliente.getApelido());
							pontoColeta.setRazaoSocialCliente(cliente.getNomeRazaoSocial());
							pontoColeta.setRota(rotaSelecionada);
							
							pontoColeta.setRamoAtividade(cliente.getRamoAtividade());
					
							pontoColeta.setCpfResponsavel(cliente.getCpfResponsavel());
							pontoColeta.setRgResponsavel(cliente.getRgResponsavel());
							pontoColeta.setOrgaoEmissorRgResponsavel(cliente.getOrgaoEmissorRgResponsavel());
							pontoColeta.setEnderecoResponsavel(cliente.getEnderecoResponsavel());
							pontoColeta.setCidadeResponsavel(cliente.getCidadeResponsavel());
							
							pontoColetaService.inserirAlterar(pontoColeta);
						}
						ExibirMensagem.exibirMensagem(Mensagem.SUCESSO + "");
						FecharDialog.fecharDialogCliente();
						criarNovoObjeto("Física");
						carregarLista();
					} else {
						ExibirMensagem.exibirMensagem("Já tem um cliente cadastrado para este CPF/CNPJ: "
								+ list.get(0).getNomeRazaoSocialNomeFantasia());
					}
				} else {

					cliente.setStatus(true);
					clienteService.inserirAlterar(cliente);
					PontoColeta pontoColeta;
					List<PontoColeta> lp = daoPontosColeta.listar(PontoColeta.class, "cliente.id=" + cliente.getId()
							+ " and modeloCadastroPonto='Ponto de Coleta com os Dados do Cliente'");
					if (lp.size() > 0) {						
						pontoColeta = lp.get(0);
						pontoColeta.setDataUltimaAlteracao(new Date());
						pontoColeta.setCep(cliente.getCep());
						pontoColeta.setCidade(cliente.getCidade());
						pontoColeta.setCpfCnpj(cliente.getCpfCnpj());
						pontoColeta.setDiaVencimentoFatura(cliente.getDiaVencimentoFatura());
						pontoColeta.setEmail(cliente.getEmail());
						pontoColeta.setEndereco(cliente.getEndereco());
						pontoColeta.setNomeFantasia(cliente.getNomeFantasia());
						pontoColeta.setNomeRazaoSocial(cliente.getNomeRazaoSocial());
						pontoColeta.setNumero(cliente.getNumero());
						pontoColeta.setResponsavel(cliente.getResponsavel());
						pontoColeta.setRgInscricaoEstadual(cliente.getRgInscricaoEstadual());
						pontoColeta.setTelefone1(cliente.getTelefone1());
						pontoColeta.setTelefone2(cliente.getTelefone2());
						pontoColeta.setTipoPessoa(cliente.getTipoPessoa());
						pontoColeta.setBairro(cliente.getBairro());

						pontoColeta.setEdificio(cliente.getEdificio());
						pontoColeta.setSala(cliente.getSala());
						pontoColeta.setAndar(cliente.getAndar());
						pontoColeta.setComplemento(cliente.getComplemento());
						pontoColeta.setObservacao(cliente.getObservacao());
						pontoColeta.setEmail2(cliente.getEmail2());
						pontoColeta.setEmail3(cliente.getEmail3());
						pontoColeta.setApelido(cliente.getApelido());
						pontoColeta.setRazaoSocialCliente(cliente.getNomeRazaoSocial());
						
						pontoColeta.setRamoAtividade(cliente.getRamoAtividade());
						
						pontoColeta.setCpfResponsavel(cliente.getCpfResponsavel());
						pontoColeta.setRgResponsavel(cliente.getRgResponsavel());
						pontoColeta.setOrgaoEmissorRgResponsavel(cliente.getOrgaoEmissorRgResponsavel());
						pontoColeta.setEnderecoResponsavel(cliente.getEnderecoResponsavel());
						pontoColeta.setCidadeResponsavel(cliente.getCidadeResponsavel());

						pontoColetaService.inserirAlterar(pontoColeta);
					}
					ExibirMensagem.exibirMensagem(Mensagem.SUCESSO + " - sucesso 2");
					FecharDialog.fecharDialogCliente();
					criarNovoObjeto("Física");
					carregarLista();
				}
			} else {
				ExibirMensagem.exibirMensagem("CPF ou CNPJ está incorreto!");
			}
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void definirSenhaAdmin() {
		if (cliente == null || cliente.getId() == null) {
			ExibirMensagem.exibirMensagemErro("Salve o cadastro do cliente antes de definir a senha administrativa.");
			return;
		}
		if (novaSenhaAdmin == null || novaSenhaAdmin.trim().length() < 6) {
			ExibirMensagem.exibirMensagemErro("A senha administrativa deve ter pelo menos 6 caracteres!");
			return;
		}
		Calendar validade = Calendar.getInstance();
		validade.add(Calendar.HOUR, 24);
		clienteService.definirSenhaAdmin(cliente.getId(), CriptografiaSenha.criptografar(novaSenhaAdmin),
				validade.getTime());
		cliente.setSenhaAdminValidade(validade.getTime());
		novaSenhaAdmin = "";
		ExibirMensagem.exibirMensagem("Senha administrativa definida! Válida até "
				+ new SimpleDateFormat("dd/MM/yyyy HH:mm").format(validade.getTime()) + ".");
	}

	public void revogarSenhaAdmin() {
		if (cliente == null || cliente.getId() == null) {
			return;
		}
		clienteService.revogarSenhaAdmin(cliente.getId());
		cliente.setSenhaAdminValidade(null);
		ExibirMensagem.exibirMensagem("Senha administrativa revogada.");
	}

	public String getNovaSenhaAdmin() {
		return novaSenhaAdmin;
	}

	public void setNovaSenhaAdmin(String novaSenhaAdmin) {
		this.novaSenhaAdmin = novaSenhaAdmin;
	}

	public void criarNovoObjeto(String tipoPessoa) {
		cliente = new Cliente();
		definirPontoColeta = true;
		cliente.setTipoPessoa(tipoPessoa);
		
		setTabAtiva(0);
	}

	public void carregarLista() {
		listCliente = daoCliente.listaComStatus(Cliente.class,"cidade.nome, nomeRazaoSocial");
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public List<Cliente> getClienteBusca() {
		return clienteBusca;
	}

	public void setClienteBusca(List<Cliente> clienteBusca) {
		this.clienteBusca = clienteBusca;
	}

	public List<Cliente> getListCliente() {
		return listCliente;
	}

	public void setListCliente(List<Cliente> listCliente) {
		this.listCliente = listCliente;
	}

	public List<PontoColeta> getPontosColeta() {
		return pontosColeta;
	}

	public void setPontosColeta(List<PontoColeta> pontosColeta) {
		this.pontosColeta = pontosColeta;
	}

	public boolean isDefinirPontoColeta() {
		return definirPontoColeta;
	}

	public void setDefinirPontoColeta(boolean definirPontoColeta) {
		this.definirPontoColeta = definirPontoColeta;
	}

	public int getTabAtiva() {
		return tabAtiva;
	}

	public void setTabAtiva(int tabAtiva) {
		this.tabAtiva = tabAtiva;
	}

	public Rota getRotaSelecionada() {
		return rotaSelecionada;
	}

	public void setRotaSelecionada(Rota rotaSelecionada) {
		this.rotaSelecionada = rotaSelecionada;
	}
	public List<InformacaoFinanceira> getListaInformacaoFinanceira() {
		return listaInformacaoFinanceira;
	}
	public void setListaInformacaoFinanceira(List<InformacaoFinanceira> listaInformacaoFinanceira) {
		this.listaInformacaoFinanceira = listaInformacaoFinanceira;
	}

	public List<FechamentoMes> getListaFechamentoMes() {
		return listaFechamentoMes;
	}

	public void setListaFechamentoMes(List<FechamentoMes> listaFechamentoMes) {
		this.listaFechamentoMes = listaFechamentoMes;
	}
	
	

}
