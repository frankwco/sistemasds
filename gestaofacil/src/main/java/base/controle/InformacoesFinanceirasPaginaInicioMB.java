package base.controle;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
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
import javax.swing.text.MaskFormatter;

import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import base.modelo.Pessoa;
import base.modelo.PontoColeta;
import base.modelo.Cliente;
import base.modelo.Contrato;
import base.modelo.FechamentoMes;
import base.modelo.GrupoResiduo;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.MensagemSite;
import base.service.ClienteService;
import base.service.ContratoService;
import base.service.FechamentoMesService;
import base.service.GrupoResiduoService;
import base.service.InformacoesFinanceirasService;
import base.service.ItensInformacaoFinanceiraGrupoResiduoService;
import base.service.ItensInformacaoFinanceiraPontoColetaService;
import base.service.MensagemSiteService;
import base.service.PontoColetaService;
import base.service.UsuarioService;
import util.ChamarRelatorio;
import util.CriptografiaSenha;
import util.EnviarEmailThread;
import util.ExibirMensagem;
import util.FecharDialog;
import util.GeradorSenhas;
import util.Mensagem;
import util.email.EmailAtualizacaoCadastro;
import util.email.EmailColetaMensagem;
import dao.GenericDAO;
import dao.InformacaoFinaceiraDAO;
import net.sf.jasperreports.engine.util.ConcurrentMapping.Mapper;

@ViewScoped
@Named("informacoesFinanceirasPaginaInicioMB")
public class InformacoesFinanceirasPaginaInicioMB implements Serializable {

	private static final long serialVersionUID = 1L;
	private Date dataContratosVencer;

	private List<InformacaoFinanceira> listInformacoesFinanceirasVencidas;
	private List<InformacaoFinanceira> listInformacoesFinanceirasVencer;
	private Cliente clienteSelecionado;

	@Inject
	private UsuarioService pessoaService;

	@Inject
	private ClienteService clienteService; // inserir no banco

	@Inject
	private GenericDAO<InformacaoFinanceira> daoInformacoesFinanceiras; // faz as buscas

	@Inject
	private InformacoesFinanceirasService informacoesFinanceirasService; // inserir no banco

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta; // faz as buscas

	@Inject
	private PontoColetaService pontoColetaService; // inserir no banco

	@Inject
	private GenericDAO<PontoColeta> daoGrupoResiduos; // faz as buscas

	@Inject
	private GrupoResiduoService grupoResiduosService; // inserir no banco

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
	private GenericDAO<Contrato> daoContrato; // faz as buscas

	@Inject
	private InformacaoFinaceiraDAO daoInformacaoFinanceiraEspecifico; // faz as buscas

	@Inject
	private ContratoService contratoService; // inserir no banco

	@Inject
	private GenericDAO<MensagemSite> daoMensagemSite; // faz as buscas

	@Inject
	private MensagemSiteService mensagemSiteService; // inserir no banco

	@Inject
	private FechamentoMesService fechamentoMesService; // faz as buscas

	private List<MensagemSite> listaMensagemSite;
	private List<MensagemSite> mensagensAtualizarEmail;
	private MensagemSite mensagemSite;

	private Double totalExcedentesAbertos = 0.;

	private int numero=0;

	@Inject
	private EntityManager manager;

	@PostConstruct
	public void inicializar() {

		preencherMensagemSite();
		buscarContratosVencidos();
		totalExcedentesAbertos = fechamentoMesService.buscarTotalExcedentesAbertos();

	}
	 public void incrementar() {
		 //System.out.println("AAAA");
	 numero++;
	 }

	private void preencherMensagemSite() {
		listaMensagemSite = daoMensagemSite.listar(MensagemSite.class,
				"tipo='contato' or tipo is null order by id desc");
		mensagensAtualizarEmail = daoMensagemSite.listar(MensagemSite.class, "tipo='atualizarDados' order by id desc");
	}

	public void lerMensagem(MensagemSite m) {
		mensagemSite = m;
		m.setLida(true);
		mensagemSiteService.inserirAlterar(m);
	}

	public void salvarCliente() {
		if (clienteSelecionado != null && clienteSelecionado.getId() != null) {
			clienteService.inserirAlterar(clienteSelecionado);
			ExibirMensagem.exibirMensagem("Cliente atualizado com sucesso!!");

			if(clienteSelecionado.getEmail().length()>5) {
			String senha = GeradorSenhas.gerarSenha();
			String novaSenha = senha.charAt(0) + "" + senha.charAt(1) + "" + senha.charAt(3) + "" + senha.charAt(5) + ""
					+ senha.charAt(6);

			pessoaService.updateSenha(CriptografiaSenha.criptografar(novaSenha), clienteSelecionado.getEmail());
			clienteService.updateSenhaCliente(CriptografiaSenha.criptografar(novaSenha), clienteSelecionado.getEmail());

			String mensagem = EmailAtualizacaoCadastro.getMensagemAtualizacaoCadastroEmail(clienteSelecionado,
					novaSenha);

			String email = "";
			email = clienteSelecionado.getEmail();

			if (email.length() > 5) {
				Thread t = new Thread(new EnviarEmailThread(email,
						"<NÃO RESPONDA> Atualização de cadastro na DS Transporte  -" + clienteSelecionado.getNomeRazaoSocial(),
						mensagem));
				t.start();
			}
			}
			
			if(clienteSelecionado.getEmail2().length()>5) {
				String senha = GeradorSenhas.gerarSenha();
				String email = clienteSelecionado.getEmail2();
				String novaSenha = senha.charAt(0) + "" + senha.charAt(1) + "" + senha.charAt(3) + "" + senha.charAt(5) + ""
						+ senha.charAt(6);

				pessoaService.updateSenhaEmail2(CriptografiaSenha.criptografar(novaSenha), email);
				clienteService.updateSenhaClienteEmail2(CriptografiaSenha.criptografar(novaSenha), email);

				String mensagem = EmailAtualizacaoCadastro.getMensagemAtualizacaoCadastroEmail(clienteSelecionado,
						novaSenha);

				

				if (email.length() > 5) {
					Thread t = new Thread(new EnviarEmailThread(email,
							"<NÃO RESPONDA> Atualização de cadastro na DS Transporte  -" + clienteSelecionado.getNomeRazaoSocial(),
							mensagem));
					t.start();
				}
				}
			// formatar a mensagem
		} else {
			ExibirMensagem.exibirMensagem(
					"Cliente não encontrado. Tente acessar o cadastro de cliente para atualizar o e-mail!!");
		}
	}
	
	public void cancelarSolicitacao(MensagemSite m) {
		m.setLida(true);
		mensagemSiteService.inserirAlterar(m);
	}

	public void atualizarEmail(MensagemSite m) {
		m.setLida(true);
		mensagemSiteService.inserirAlterar(m);

		String cpfCnpj = m.getCpfCnpj();

		cpfCnpj = cpfCnpj.replace(".", "");
		cpfCnpj = cpfCnpj.replace("-", "");
		cpfCnpj = cpfCnpj.replace("/", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.replace(" ", "");
		cpfCnpj = cpfCnpj.trim();
		//System.out.println(cpfCnpj + " " + cpfCnpj.length() + " - " + m.getNomeRazaoSocial());

		if (cpfCnpj.length() == 11) {

			try {
				MaskFormatter mask = new MaskFormatter("###.###.###-##");
				mask.setValueContainsLiteralCharacters(false);
				//System.out.println("CNPJ : " + mask.valueToString(cpfCnpj));
				cpfCnpj = mask.valueToString(cpfCnpj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (cpfCnpj.length() == 14) {
			try {
				MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
				mask.setValueContainsLiteralCharacters(false);
				//System.out.println("CNPJ : " + mask.valueToString(cpfCnpj));
				cpfCnpj = mask.valueToString(cpfCnpj);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			//System.out.println("Tamanho invalido");
			cpfCnpj = "5252515235";
		}

		List<Cliente> lc = daoCliente.listarPorCampo(Cliente.class, "cpfCnpj", cpfCnpj);
		if (lc.size() > 0) {
			clienteSelecionado = lc.get(0);
			clienteSelecionado.setEmailAntigo(clienteSelecionado.getEmail());
			clienteSelecionado.setEmail2Antigo(clienteSelecionado.getEmail2Antigo());
			clienteSelecionado.setEmail(m.getEmail());
			clienteSelecionado.setEmail2(m.getEmail2().equalsIgnoreCase(m.getEmail())?"":m.getEmail2());

			if (m.getTelefones().trim().length() > 5) {
				clienteSelecionado.setTelefone1Antigo(clienteSelecionado.getTelefone1());
				clienteSelecionado.setTelefone1(m.getTelefones());
			}
			if (m.getTelefones2().trim().length() > 5) {
				clienteSelecionado.setTelefone2Antigo(clienteSelecionado.getTelefone2());
				clienteSelecionado.setTelefone2(m.getTelefones2());
			}
		//	System.out.println("Cliente " + clienteSelecionado.getNomeRazaoSocial());
		} else {
			clienteSelecionado = new Cliente();
			clienteSelecionado.setNomeRazaoSocial("Cliente não encontrado com o CPF/CNPJ " + cpfCnpj
					+ ", tente encontrar pelo cadastro de clientes!!");
		//	System.out.println("Cliente não encontrado");

		}

	}

	public void buscarContratosVencidos() {

		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		listInformacoesFinanceirasVencidas = daoInformacaoFinanceiraEspecifico
				.buscarInformacaoFinanceiraNomeData("dataFinal between '2000-01-01' and '" + format.format(new Date())
						+ "' and situacaoContrato='Ativo' order by dataFinal asc");

		DateFormat formatMes = new SimpleDateFormat("MM");
		Integer mes = Integer.parseInt(formatMes.format(new Date()));

		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.set(Calendar.MONTH, mes);
		dataContratosVencer = c.getTime();

		listInformacoesFinanceirasVencer = daoInformacaoFinanceiraEspecifico
				.buscarInformacaoFinanceiraNomeData("dataFinal between '" + format.format(new Date()) + "' and '"
						+ format.format(c.getTime()) + "' and situacaoContrato='Ativo' order by dataFinal asc");
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

	public Date getDataContratosVencer() {
		return dataContratosVencer;
	}

	public void setDataContratosVencer(Date dataContratosVencer) {
		this.dataContratosVencer = dataContratosVencer;
	}

	public List<MensagemSite> getListaMensagemSite() {
		return listaMensagemSite;
	}

	public void setListaMensagemSite(List<MensagemSite> listaMensagemSite) {
		this.listaMensagemSite = listaMensagemSite;
	}

	public MensagemSite getMensagemSite() {
		return mensagemSite;
	}

	public void setMensagemSite(MensagemSite mensagemSite) {
		this.mensagemSite = mensagemSite;
	}

	public List<MensagemSite> getMensagensAtualizarEmail() {
		return mensagensAtualizarEmail;
	}

	public void setMensagensAtualizarEmail(List<MensagemSite> mensagensAtualizarEmail) {
		this.mensagensAtualizarEmail = mensagensAtualizarEmail;
	}

	public Cliente getClienteSelecionado() {
		return clienteSelecionado;
	}

	public void setClienteSelecionado(Cliente clienteSelecionado) {
		this.clienteSelecionado = clienteSelecionado;
	}

	public Double getTotalExcedentesAbertos() {
		return totalExcedentesAbertos;
	}
	   public int getNumero() {
	        return numero;
	    }
}
