package base.controle;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.Format;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;

import org.hibernate.Session;

import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.Coleta;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppAssinatura;
import base.modelo.ColetaAppProximaColeta;
import base.modelo.ItensColeta;
import base.modelo.PontoColeta;
import base.modelo.Usuario;
import base.service.CidadeService;
import base.service.ColetaAppAssinaturaService;
import base.service.ColetaAppService;
import base.service.ItensColetaService;
import util.ChamarRelatorio;
import util.ConverteStringDate;
import util.EnviarEmailThread;
import util.ExibirMensagem;
import util.FecharDialog;
import util.FormatarMB;
import util.Mensagem;
import util.email.EmailColetaMensagem;
import dao.GenericDAO;

@ViewScoped
@Named("coletaAppMB")
public class ColetaAppMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private ColetaApp coletaApp;
	private ColetaApp coletaAppEnviarEmail;
	private List<ColetaApp> listaColetaApp;
	private Date dataInicialConsulta;
	private Date dataFinalConsulta = new Date();
	private ColetaAppProximaColeta proximaColeta;

	private Cliente clienteSelecionadoConsulta;
	private PontoColeta pontoColetaSelecionadoConsulta;

	private List<ColetaApp> listaColetaAppSelecionado = new ArrayList<ColetaApp>();

	private Cidade cidadeSelecionadaConsulta = null;

	@Inject
	private EntityManager manager;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp; // faz as buscas
	
	@Inject
	private GenericDAO<Coleta> daoColeta; // faz as buscas

	@Inject
	private GenericDAO<ItensColeta> daoItensColeta; // faz as buscas

	@Inject
	private GenericDAO<ColetaAppAssinatura> daoColetaAppAssinatura; // faz as buscas

	@Inject
	private ColetaAppService coletaAppService; // inserir no banco
	
	@Inject
	private ItensColetaService itensColetaAppService; // inserir no banco

	@Inject
	private ColetaAppAssinaturaService coletaAppAssinaturaService; // inserir no banco

	@PostConstruct
	public void inicializar() {

		coletaApp = new ColetaApp();
		Calendar c = Calendar.getInstance(); // this takes current date
		c.set(Calendar.DAY_OF_MONTH, 1);
		// System.out.println(c.getTime());
		dataInicialConsulta = c.getTime();

		// listCidade = daoCidade.listaComStatus(ColetaApp.class);
		// listaColetaApp = new ArrayList<>();
		// listaColetaApp = daoColetaApp.listaComStatus(ColetaApp.class,
		// "dataUltimaColeta desc");

	}
	
	
	
	
	public void atualizarA2JaneiroJulho2022() {
		List<ColetaApp> lcapp = daoColetaApp.listar(ColetaApp.class,"a>0 and dataUltimaColeta BETWEEN '2022-01-01' and '2022-12-31'");
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		for(ColetaApp c:lcapp) {
			List<Coleta> lc = daoColeta.listar(Coleta.class, "pontoColeta.id="+c.getPontoColetaSelecionado().getId()+" and dataColeta BETWEEN '"+format.format(c.getDataUltimaColeta())+"' and '"+format.format(c.getDataUltimaColeta())+"'");
			for(Coleta col:lc) {
				List<ItensColeta> itensColeta = daoItensColeta.listar(ItensColeta.class, "coleta.id="+col.getId());
				for(ItensColeta item:itensColeta) {
					if(item.getGrupoResiduo().getDescricao().equals("A")) {
						item.setQuantidadeColetada(c.getA());
						itensColetaAppService.inserirAlterar(item);
					}
				}
			}
		}
		ExibirMensagem.exibirMensagemErro(
				"Finalizado!");
	}

	public void selecionarColeta(ColetaApp c) {
		// O import pra Coleta oficial agora é automático (logo após o app enviar),
		// então bloquear "já importado" aqui bloquearia praticamente sempre. A
		// alteração é propagada pra Coleta em salvarColeta()/propagarAlteracao();
		// o único caso que ainda barra é coleta já faturada (ver salvarColeta()).
		this.coletaApp = c;
	}
	
	public void excluirColeta(ColetaApp c) {
		if (!c.getImportado()) {
			c.setStatus(false);
			coletaAppService.inserirAlterar(c);
			consultar();
		} else {
			ExibirMensagem.exibirMensagem(
					"Coleta já importada, não é possível excluir. Acesse o menu Coletas para fazer a alteração!");
		}

	}

	public void salvarColeta() {
		try {
			coletaAppService.inserirAlterar(coletaApp);
			coletaAppService.propagarAlteracao(coletaApp);
			this.consultar();
			ExibirMensagem.exibirMensagem("Coleta Alterada!");
		} catch (IllegalStateException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		}
	}

	public void dataProximaColeta(ColetaApp co) {
		proximaColeta = coletaAppService.proximaColeta(co.getPontoColetaSelecionado());
	}

	public void imprimirFichaColeta() {

		String codigoImpressao = ConverteStringDate.retornaDataddMMyyyyHHmmss(new Date());

		for (ColetaApp c : listaColetaAppSelecionado) {
			try {
				// System.out.println(c.getId());
				ColetaAppAssinatura assinatura = new ColetaAppAssinatura();
				List<ColetaAppAssinatura> la = daoColetaAppAssinatura.listar(ColetaAppAssinatura.class,
						"coletaApp.id=" + c.getId());
				if (la.size() > 0) {
					assinatura = la.get(0);
				} else {
					assinatura.setColetaApp(c);
					assinatura.setAssinaturaByte(
							"ZGF0YTppbWFnZS9wbmc7YmFzZTY0LGlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFXZ0FBQUNXQ0FZQUFBRHpDaElJQUFBQUFYTlNSMElBcnM0YzZRQUFCUXRKUkVGVWVGN3QxRUVOQUFBTUFySGgzL1JzM0tOVFFNckN6aEVnUUlCQVVtREpWRUlSSUVDQXdCbG9UMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJRzJnOFFJRUFnS21DZ284V0lSWUFBQVFQdEJ3Z1FJQkFWTU5EUllzUWlRSUNBZ2ZZREJBZ1FpQW9ZNkdneFloRWdRTUJBK3dFQ0JBaEVCUXgwdEJpeENCQWdZS0Q5QUFFQ0JLSUNCanBhakZnRUNCQXcwSDZBQUFFQ1VRRURIUzFHTEFJRUNCaG9QMENBQUlHb2dJR09GaU1XQVFJRURMUWZJRUNBUUZUQVFFZUxFWXNBQVFJUFhuY0FsekhCK3pzQUFBQUFTVVZPUks1Q1lJST0="
									.getBytes());
				}
				assinatura.setSituacao(c.getSituacao());
				assinatura.setClienteEnderecoCompleto(c.getClienteEndereco());
				assinatura.setDadosMotoristaVeiculo(c.getDadosTransporte());
				assinatura.setPontoColetaEnderecoCompleto(c.getPontoColetaEndereco());
				assinatura.setLixoColetado(EmailColetaMensagem.getColetas(c));
				assinatura.setDataInclusao(c.getDataUltimaColeta());
				assinatura.setCodigoImpressao(codigoImpressao);

				coletaAppAssinaturaService.inserirAlterar(assinatura);

			} catch (Exception e) {
				e.printStackTrace();
				ExibirMensagem.exibirMensagem(
						"Erro ao imprimir a ficha do ponto " + c.getPontoColetaSelecionado().getNomeRazaoSocial());
			}
		}

		if (listaColetaAppSelecionado.size() > 0) {
			DateFormat format = new SimpleDateFormat("ddMMyyyyHHmm");
			HashMap parametro = new HashMap<>();
			parametro.put("CODIGO_IMPRESSAO", codigoImpressao);

			ChamarRelatorio ch = new ChamarRelatorio("ficha_coleta.jasper", parametro,
					"recibo_coleta_" + format.format(new Date()));
			Session sessions = manager.unwrap(Session.class);
			sessions.doWork(ch);

		}
	}

	public void consultar() {
		StringBuilder query = new StringBuilder();
		if (dataInicialConsulta != null && dataFinalConsulta != null) {
//			/ConverteStringDate.retornaD
			query.append("dataUltimaColeta BETWEEN '" + ConverteStringDate.retornaData(dataInicialConsulta)
					+ " 00:00:00' and '" + ConverteStringDate.retornaData(dataFinalConsulta) + " 23:59:59'");
		}
		if (clienteSelecionadoConsulta != null && clienteSelecionadoConsulta.getId() != null) {
			if (query.toString().length() > 2) {
				query.append(" and ");
			}
			query.append("pontoColetaSelecionado.cliente.id=" + clienteSelecionadoConsulta.getId());
		}

		if (pontoColetaSelecionadoConsulta != null && pontoColetaSelecionadoConsulta.getId() != null) {
			if (query.toString().length() > 2) {
				query.append(" and ");
			}
			query.append("pontoColetaSelecionado.id=" + pontoColetaSelecionadoConsulta.getId());
		}

		if (cidadeSelecionadaConsulta != null) {
			if (query.toString().length() > 2) {
				query.append(" and ");
			}
			query.append("pontoColetaSelecionado.cidade.id=" + cidadeSelecionadaConsulta.getId());
		}

		if (query.toString().length() < 2) {
			query.append(" and id>0");
		}

		listaColetaApp = new ArrayList<>();
		listaColetaApp = daoColetaApp.listar(ColetaApp.class, query.toString() + " order by dataUltimaColeta desc");
//		listaColetaApp = daoColetaApp.listar(ColetaApp.class, query.toString() + " order by id desc");

	}

	public void coletaEnviarEmail(ColetaApp co) {
		this.coletaAppEnviarEmail = co;
	}

	public void enviarEmail(ColetaApp co) {
		// ColetaApp co = this.coletaAppEnviarEmail;

		String mensagem = EmailColetaMensagem.getMensagemComMTR(co);

		String email = "";

		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

		if (co.getPontoColetaSelecionado().getCliente().getEmail().trim().length() > 5) {
			Thread t = new Thread(new EnviarEmailThread(co.getPontoColetaSelecionado().getCliente().getEmail().trim(),
					"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta()) + " -"
							+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
					mensagem));
			t.start();
		}

		if (co.getEmailNovo().trim().length() > 5) {
			Thread t = new Thread(new EnviarEmailThread(co.getEmailNovo().trim(),
					"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta()) + " -"
							+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
					mensagem));
			t.start();
		}

		if (co.getPontoColetaSelecionado().getCliente().getEmail2().trim().length() > 5
				&& !co.getPontoColetaSelecionado().getCliente().getEmail2().trim()
						.equalsIgnoreCase(co.getPontoColetaSelecionado().getCliente().getEmail().trim())) {
			Thread t = new Thread(new EnviarEmailThread(co.getPontoColetaSelecionado().getCliente().getEmail2().trim(),
					"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta()) + " -"
							+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
					mensagem));
			t.start();
		}

		// formatar a mensagem
		ExibirMensagem.exibirMensagem("Solicitação de e-mail enviada!!");
	}

	/**
	 * Rede de segurança: o import pra Coleta/ItensColeta oficiais agora roda
	 * automaticamente logo após o app enviar (ver ServicoHello.cadastroColeta
	 * -> ColetaAppService.importar). Este botão fica só pra pegar o que
	 * falhar no automático (ex.: ponto sem contrato financeiro ativo na
	 * hora) — reaproveita o mesmo método, então o resultado é idêntico.
	 */
	public void importarDados() {
		List<ColetaApp> listaImportar = daoColetaApp.listar(ColetaApp.class, "importado is false or importado = null");
		for (ColetaApp ca : listaImportar) {
			coletaAppService.importar(ca);
		}
		ExibirMensagem.exibirMensagem("Importação das coletas realizada com sucesso!");
	}

	public void relatorioColetasRealizadas() {

		HashMap parametro = new HashMap<>();
		parametro.put("DATA_INICIAL", ConverteStringDate.retornaData(dataInicialConsulta) + " 00:00:00");
		parametro.put("DATA_FINAL", ConverteStringDate.retornaData(dataFinalConsulta) + " 23:59:59");
		parametro.put("DATA_FORMATADA", ConverteStringDate.retornaDataddMMyyyyBarra(dataInicialConsulta) + " - "
				+ ConverteStringDate.retornaDataddMMyyyyBarra(dataFinalConsulta));

		ChamarRelatorio ch = new ChamarRelatorio("coletas_app.jasper", parametro,
				"relatório_coletas_" + ConverteStringDate.retornaDataddMMyyyyBarra(dataInicialConsulta) + " - "
						+ ConverteStringDate.retornaDataddMMyyyyBarra(dataFinalConsulta));
		Session sessions = manager.unwrap(Session.class);
		sessions.doWork(ch);
	}

	public void filtro(String tipo) {
		if (tipo.equals("novoEmail")) {
			listaColetaApp = new ArrayList<>();
			listaColetaApp = daoColetaApp.listar(ColetaApp.class, "LENGTH(emailNovo)>3 order by dataUltimaColeta desc");
		}
	}

	public ColetaApp getColetaAppEnviarEmail() {
		return coletaAppEnviarEmail;
	}

	public void setColetaAppEnviarEmail(ColetaApp coletaAppEnviarEmail) {
		this.coletaAppEnviarEmail = coletaAppEnviarEmail;
	}

	public ColetaApp getColetaApp() {
		return coletaApp;
	}

	public void setColetaApp(ColetaApp coletaApp) {
		this.coletaApp = coletaApp;
	}

	public List<ColetaApp> getListaColetaApp() {
		return listaColetaApp;
	}

	public void setListaColetaApp(List<ColetaApp> listaColetaApp) {
		this.listaColetaApp = listaColetaApp;
	}

	public Date getDataInicialConsulta() {
		return dataInicialConsulta;
	}

	public void setDataInicialConsulta(Date dataInicialConsulta) {
		this.dataInicialConsulta = dataInicialConsulta;
	}

	public Date getDataFinalConsulta() {
		return dataFinalConsulta;
	}

	public void setDataFinalConsulta(Date dataFinalConsulta) {
		this.dataFinalConsulta = dataFinalConsulta;
	}

	public List<ColetaApp> getListaColetaAppSelecionado() {
		return listaColetaAppSelecionado;
	}

	public void setListaColetaAppSelecionado(List<ColetaApp> listaColetaAppSelecionado) {
		this.listaColetaAppSelecionado = listaColetaAppSelecionado;
	}

	public Cliente getClienteSelecionadoConsulta() {
		return clienteSelecionadoConsulta;
	}

	public void setClienteSelecionadoConsulta(Cliente clienteSelecionadoConsulta) {
		this.clienteSelecionadoConsulta = clienteSelecionadoConsulta;
	}

	public PontoColeta getPontoColetaSelecionadoConsulta() {
		return pontoColetaSelecionadoConsulta;
	}

	public void setPontoColetaSelecionadoConsulta(PontoColeta pontoColetaSelecionadoConsulta) {
		this.pontoColetaSelecionadoConsulta = pontoColetaSelecionadoConsulta;
	}

	public ColetaAppProximaColeta getProximaColeta() {
		return proximaColeta;
	}

	public void setProximaColeta(ColetaAppProximaColeta proximaColeta) {
		this.proximaColeta = proximaColeta;
	}

	public Cidade getCidadeSelecionadaConsulta() {
		return cidadeSelecionadaConsulta;
	}

	public void setCidadeSelecionadaConsulta(Cidade cidadeSelecionadaConsulta) {
		this.cidadeSelecionadaConsulta = cidadeSelecionadaConsulta;
	}

}
