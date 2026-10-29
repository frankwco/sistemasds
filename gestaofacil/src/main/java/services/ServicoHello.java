package services;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.enterprise.inject.New;
import javax.inject.Inject;
import javax.naming.AuthenticationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import base.modelo.Atividade;
import base.modelo.CategoriaIndicador;
import base.modelo.Cidade;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppAssinatura;
import base.modelo.ColetaAppPontosSemColeta;
import base.modelo.ColetaAppProximaColeta;
import base.modelo.Estado;
import base.modelo.GrupoLancamento;
import base.modelo.Indicador;
import base.modelo.ItensColeta;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.ItensLancamento;
import base.modelo.Lancamento;
import base.modelo.LogTarefas;
import base.modelo.MensagemSite;
import base.modelo.Ocorrencia;
import base.modelo.Pessoa;
import base.modelo.PontoColeta;
import base.modelo.PontoColetaRetorno;
import base.modelo.Processo;
import base.modelo.Retorno;
import base.modelo.Usuario;
import base.modelo.Veiculo;
import base.service.ClienteService;
import base.service.ColetaAppAssinaturaService;
import base.service.ColetaAppService;
import base.service.ItensLancamentoService;
import base.service.LancamentoService;
import base.service.LogTarefasService;
import base.service.MensagemSiteService;
import base.service.OcorrenciaService;
import base.service.PontoColetaService;
import dados.ImcMock;
import dados.TimesMock;
import dao.ColetaAppDAO;
import dao.GenericDAO;
import modelo.Imc;
import modelo.Time;
import util.ConverteStringDate;
import util.CriptografiaSenha;
import util.EnviarEmail;
import util.EnviarEmailIF;
import util.EnviarEmailThread;
import util.ExibirMensagem;
import util.FormatarMB;
import util.FuncoesMatematicas;
import util.Mensagem;
import util.MensagemRetorno;
import util.email.EmailColetaMensagem;

@Path("/service")
public class ServicoHello {

	@Inject
	private LogTarefasService logTarefasService;

	@Inject
	private ColetaAppAssinaturaService coletaAppAssinaturaService;

	@Inject
	private GenericDAO<Processo> daoProcesso; // faz as buscas

	@Inject
	private GenericDAO<Indicador> daoIndicador; // faz as buscas

	@Inject
	private GenericDAO<Atividade> daoAtividade; // faz as buscas

	@Inject
	private GenericDAO<CategoriaIndicador> daoCategoriaIndicadores; // faz as buscas

	@Inject
	private GenericDAO<GrupoLancamento> daoGrupoLancamento; // faz as buscas

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta; // faz as buscas,

	@Inject
	private FuncoesMatematicas funcoesMatematicas;

	@Inject
	private OcorrenciaService ocorrenciaService; // inserir no banco

	@Inject
	private ClienteService clienteService; // inserir no banco

	@Inject
	private LancamentoService lancamentoService; // inserir no banco

	@Inject
	private ItensLancamentoService itensLancamentoService; // inserir no banco

	@Inject
	private MensagemSiteService mensagemSiteService; // inserir no banco

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraGrupo> daoItensInformacaoFinanceiraGrupo;

	@Inject
	private ColetaAppService coletaAppService; // inserir no banco

	@Inject
	private PontoColetaService pontoColetaService; // inserir no banco

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp;

	@Inject
	private GenericDAO<Cidade> daoCidade;

	@Inject
	private ColetaAppDAO coletaAppDAO;

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraPontoColeta> daoItensInformacaoFinanceiraPontoColeta; // faz

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/pontosSemColetas")
	public Response pontosSemColetas(@QueryParam("dataInicial") String dataInicial,
			@QueryParam("dataFinal") String dataFinal) {

		// List<ColetaAppPontosSemColeta> c

		GenericEntity<List<ColetaAppPontosSemColeta>> lista = new GenericEntity<List<ColetaAppPontosSemColeta>>(
				coletaAppDAO.buscarInformacaoFinanceiraNomeData(dataInicial, dataFinal)) {
		};
//			

		return Response.status(200).entity(lista).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/enviarEmail")
	public Response enviarEmail(EnvioEmailIF em) {
		if (em.getTitulo().equals("certificados")) {
			em.setTitulo("Recuperação de Senha - Certificados e Declarações");
		}
		em.setMensagem("Olá,\na sua nova senha é: " + em.getMensagem());

		Boolean flag = EnviarEmailIF.enviarEmail(em.getDestinatario(), em.getTitulo(), em.getMensagem());

		if (flag) {
			return Response.status(200).build();
		} else {

			return Response.status(400).build();
		}
	}

	@GET
	@Produces("application/json; charset=UTF-8")
	@Path("/teste-server")
	public String testeNet() {
		return ".";
	}

	/**
	 * Não é mais um endpoint JAX-RS direto — exposto via
	 * {@link ServicoApp#cadastrarColeta(List)}, que exige a chave de API do
	 * app. A lógica de negócio continua toda aqui, inalterada.
	 */
	public Response cadastroColeta(List<ColetaApp> lc) {
		// System.out.println(lc.size());
		int dia = -1, mes = -1, ano = -1;
		Long veiculo = -1L;
		int controle = 0;
		Boolean acessoTotal = false;
		// List<Usuario> lusuario = daoUsuario.listar(Usuario.class,
		// "email='"+lc.get(0).getEmailUsuario()+"'");
		// acessoTotal = lusuario.get(0).getAcessoTotal();
		// LogTarefas logTarefasProblema = new LogTarefas();
		// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - " +
		// lc.size());
		// logTarefasService.inserirAlterar(logTarefasProblema);
		// sysou
		if (lc.size() == 1) {
			ColetaApp a = lc.get(0);
			if (a.getTipo().equals("lista")) {
				// System.out.println("TIPO LISTA");
				controle = 1;
				acessoTotal = a.getAcessoTotal();
				dia = a.getDiaUltimaColeta();
				mes = a.getMesUltimaColeta();
				ano = a.getAnoUltimaColeta();
				veiculo = a.getVeiculo().getId();
				// System.out.println(dia + " " + mes + " " + ano + " " + veiculo);
			}
		}
		if (controle == 0) {

			// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
			// controle 0");
			// logTarefasService.inserirAlterar(logTarefasProblema);
			// System.out.println("TAMANHO: " + lc.size());

			for (ColetaApp co : lc) {
				// System.out.println("TIP: " + co.getTipo());
				List<ColetaApp> lbanco = daoColetaApp.listar(ColetaApp.class,
						"identificadorApp='" + co.getIdentificadorApp().trim() + "'");

				if (co.getPontoColetaSelecionado() != null)
					// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - for:
					// "
					// + co.getPontoColetaSelecionado().getNomeRazaoSocialNomeFantasiaEndereco());
					// logTarefasService.inserirAlterar(logTarefasProblema);
					dia = co.getDiaUltimaColeta();
				mes = co.getMesUltimaColeta();
				ano = co.getAnoUltimaColeta();
				acessoTotal = co.getAcessoTotal();
				veiculo = co.getVeiculo().getId();
				// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - for
				// dados: " + dia + " - " + mes
				// + " - " + ano + " - " + veiculo + " - fim for");
				// logTarefasService.inserirAlterar(logTarefasProblema);
				try {
					SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					Date date1 = formatter1.parse(co.getDataUltimaColetaString());
					co.setDataUltimaColeta(date1);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (co.getAssinatura().length() > 5) {
					byte[] assinaturaByte = Base64.getEncoder().encode(co.getAssinatura().getBytes());
					co.setAssinaturaByte(assinaturaByte);
					co.setAssinatura("0");
				}

				for (ColetaApp cbanco : lbanco) {

					// System.out.println("CCC");
					// System.out.println("Iden "+co.getIdentificadorApp());

					if (co.getIdentificadorApp().trim().equals(cbanco.getIdentificadorApp().trim())) {
						// System.out.println("AQuii");
						co.setSincronizado(true);
						if (!co.getStatus()) {
							// System.out.println("EXCLUindoo");
							// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
							// status false");
							// logTarefasService.inserirAlterar(logTarefasProblema);
							// ColetaApp c = daoColetaApp.buscarPorId(ColetaApp.class, co.getId());

							co.setStatus(false);
							co.setDiaMesAno(co.getDiaMesAno() + "removido" + co.getIdentificadorApp());
							// System.out.println("Coleta EXCLUÍDA:
							// "+c.getPontoColetaSelecionado().getNomeRazaoSocial());
							// System.out.println("antes " + co.getId());
							// daoColetaApp.remover(ColetaApp.class,co.getId());

							// IMPORTANTE: remove usando "cbanco" (o registro encontrado no banco,
							// com id real) e não "co" (o payload desserializado do app, cujo id
							// vem null). Usar co.getId() aqui derrubava a requisição com
							// IllegalArgumentException (EntityManager.find com chave null) e a
							// remoção nunca era persistida, embora o app já tivesse removido do
							// Firebase e considerasse a operação concluída.
							coletaAppService.remover(cbanco);
							// System.out.println("depois " + co.getId());
							// coletaAppService.inserirAlterar(co);

							PontoColeta p = daoPontoColeta.buscarPorId(PontoColeta.class,
									co.getPontoColetaSelecionado().getId());
							co.setPontoColetaSelecionado(p);
							if (p != null) {

								p.setMesUltimaColeta(0);
								p.setAnoUltimaColeta(0);
								p.setDiaUltimaColeta(0);
								if (p.getDataPenultimaColeta() != null) {
									p.setDataUltimaColeta(p.getDataUltimaColeta());

								}

								p.setQuantidadeColetadaMes(p.getQuantidadeColetadaMes() - 1);

								p.setDataUltimaAlteracao(new Date());
								pontoColetaService.inserirAlterar(p);
							}

						}
					}
				}
			}

			// INSERINDO AS COLETAS NO BANCO DE DADOS
			for (ColetaApp co : lc) {
				// System.out.println(co.getIdentificadorApp());
				// System.out.println(co.getSincronizado());
				// System.out.println(co.getStatus());
				// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
				// sincronizado "+co.getSincronizado()+" stats "+co.getStatus());
				// logTarefasService.inserirAlterar(logTarefasProblema);
				if (!co.getSincronizado() && co.getStatus()) {
					// System.out.println("INSERINDO");
					// System.out.println("DIA "+co.getDiaUltimaColeta());

					List<ColetaApp> lcoleta = daoColetaApp.listar(ColetaApp.class,
							"pontoColetaSelecionado.id=" + co.getPontoColetaSelecionado().getId()
									+ " and diaUltimaColeta=" + co.getDiaUltimaColeta() + " and mesUltimaColeta="
									+ co.getMesUltimaColeta() + " and anoUltimaColeta=" + co.getAnoUltimaColeta());
					// System.out.println(lcoleta.size());
					// System.out.println(co.getPontoColetaSelecionado().getNomeRazaoSocialNomeFantasiaEndereco());
					// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
					// lcoleta:" + lcoleta.size()
					// + " p:" +
					// co.getPontoColetaSelecionado().getNomeRazaoSocialNomeFantasiaEndereco());
					int enviarEmail = 1;
					// logTarefasService.inserirAlterar(logTarefasProblema);
					if (lcoleta.size() == 0) {
						// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
						// lcoleta size 0");
						// logTarefasService.inserirAlterar(logTarefasProblema);
						/// System.out.println(co.getDataUltimaColeta());
						// System.out.println(co.getDataUltimaColetaString());
						co.setSincronizado(true);
						//co.setSituacao(null)

						PontoColeta p = daoPontoColeta.buscarPorId(PontoColeta.class,
								co.getPontoColetaSelecionado().getId());
						co.setPontoColetaSelecionado(p);

						if (p == null) {
							// Ponto referenciado não existe mais no banco (id inválido ou removido).
							// Sem esta checagem, "co.setDiaMesAno(co.getPontoColetaSelecionado()...)"
							// mais abaixo (fora do bloco "if (p != null)") sempre lançava
							// NullPointerException e derrubava o processamento desta coleta.
							continue;
						}

						if (p != null) {
							if (p.getDataUltimaColeta() != null) {
								p.setDataPenultimaColeta(co.getDataUltimaColeta());
							}
							p.setMesUltimaColeta(co.getMesUltimaColeta());
							p.setAnoUltimaColeta(co.getAnoUltimaColeta());
							p.setDiaUltimaColeta(co.getDiaUltimaColeta());

							p.setDataUltimaColeta(co.getDataUltimaColeta());
							p.setDataUltimaAlteracao(new Date());
							// System.out.println("DATA Ultima Coleta: " + p.getDataUltimaAlteracao());

							if (co.getEmailNovo() != null && co.getEmailNovo().length() > 5) {
								p.getCliente().setEmail(co.getEmailNovo());
								clienteService.inserirAlterar(p.getCliente());

								if (p.getModeloCadastroPonto().equals("Ponto de Coleta com os Dados do Cliente")) {
									p.setEmail(co.getEmailNovo());
								}
							}

							co.setEmailEnviadoPara(
									"E-mail 1: " + p.getCliente().getEmail() + "<br/>E-mail 2: "
											+ (p.getCliente().getEmail2().trim()
													.equalsIgnoreCase(p.getCliente().getEmail().trim()) ? ""
															: p.getCliente().getEmail2()));

						}

						co.setDiaMesAno(co.getPontoColetaSelecionado().getId() + "-" + co.getDiaUltimaColeta() + "-"
								+ co.getMesUltimaColeta() + "-" + co.getAnoUltimaColeta());
						ColetaAppAssinatura assinatura = new ColetaAppAssinatura();
						try {
							Random rand = new Random();
							int randomNum = 12 + rand.nextInt((10 - 3) + 5);
							Thread.sleep(randomNum);
							// System.out.println("ANTES DO FOI: " + co.getDiaMesAno());
							// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - antes
							// inserir");
							// logTarefasService.inserirAlterar(logTarefasProblema);

							assinatura.setAssinatura(co.getAssinatura());
							assinatura
									.setAssinaturaByte(co.getAssinaturaByte() != null ? co.getAssinaturaByte() : null);
							co.setAssinaturaByte(null);
							co.setAssinatura(null);
							assinatura.setColetaApp(co);

							coletaAppService.inserirAlterar(co);

							// Import automático pra Coleta/ItensColeta oficiais, sem depender mais
							// do clique manual em "Importar Dados". Não pode derrubar a resposta
							// pro app se falhar (ponto sem contrato financeiro ativo, etc.) — fica
							// com importado=false e o botão manual (idempotente) cobre depois.
							try {
								coletaAppService.importar(co);
							} catch (Exception ex) {
								ex.printStackTrace();
							}

							// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " -
							// depois inserir");
							// logTarefasService.inserirAlterar(logTarefasProblema);
							enviarEmail = 0;
							p.setQuantidadeColetadaMes(p.getQuantidadeColetadaMes() + 1);
							// ENVIAR O EMAIL E MUDAR A QUANTIDADE COLETADA SÓ AQUI
							// System.out.println("FOI");

						} catch (Exception e) {
							// e.printStackTrace();

							enviarEmail = 1;
							LogTarefas log = new LogTarefas();
							log.setObservacao("DT " + co.getDataUltimaColetaString() + " - em " + co.getEmailUsuario()
									+ " en " + co.getEmailNovo());
							log.setDataRegistro(new Date());
							log.setTipo("COLETA DUPLICADA APP");
							if (co.getPontoColetaSelecionado() != null) {
								log.setDescricaoCurta(co.getPontoColetaSelecionado().getNomeRazaoSocial());
							}
							if (co.getA() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A: " + co.getA());
							}
							if (co.getE() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " E: " + co.getE());
							}
							if (co.getB() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " B: " + co.getB());
							}
							if (co.getA2() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A2: " + co.getA2());
							}
							if (co.getA3() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A3: " + co.getA3());
							}
							if (e.getMessage().length() < 9000) {
								log.setDescricao(e.getMessage());
							} else {
								log.setDescricao("erro com coleta duplicada. Mensagem maior que 9k caracteres");
							}
							log.setStatus(true);
							// logTarefasService.inserirAlterar(log);

						}

						if (co.getPontoColetaSelecionado().getCliente().getEmail().trim()
								.equalsIgnoreCase("centerimagempvai-TIREI@gmail.com")
								|| co.getPontoColetaSelecionado().getCliente().getEmail2().trim()
										.equalsIgnoreCase("centerimagempvai-TIREI@gmail.com")) {
							enviarEmail = 1;
							LogTarefas log = new LogTarefas();
							log.setObservacao("DT " + co.getDataUltimaColetaString() + " - em " + co.getEmailUsuario()
									+ " en " + co.getEmailNovo());
							log.setDataRegistro(new Date());
							log.setTipo("ERRO EMAIL");
							if (co.getPontoColetaSelecionado() != null) {
								log.setDescricaoCurta(co.getPontoColetaSelecionado().getNomeRazaoSocial());
							}
							if (co.getA() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A: " + co.getA());
							}
							if (co.getE() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " E: " + co.getE());
							}
							if (co.getB() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " B: " + co.getB());
							}
							if (co.getA2() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A2: " + co.getA2());
							}
							if (co.getA3() != null) {
								log.setDescricaoCurta(log.getDescricaoCurta() + " A3: " + co.getA3());
							}

							Gson gson = new Gson(); // conversor
							String json = gson.toJson(co);
							log.setDescricao(json);
							log.setErro(true);

							log.setStatus(true);
							// logTarefasService.inserirAlterar(log);
						}

						if (enviarEmail == 0) {
							try {
								coletaAppAssinaturaService.inserirAlterar(assinatura);
								p.setPrimeiraColeta(false);
								pontoColetaService.inserirAlterar(p);
							} catch (Exception e) {
							}
						}

						// PARA ENVIAR EMAIL É 0
						// 1 É QUANDO DÁ ERRO
						// 2 É PARA PARAR O ENVIO DE E-MAIL
						if (enviarEmail == 0) {

							String mensagem = EmailColetaMensagem.getMensagemComMTR(co);

							String email = "";

							DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

							if (co.getPontoColetaSelecionado().getCliente().getEmail().trim().length() > 5) {
								Thread t = new Thread(new EnviarEmailThread(
										co.getPontoColetaSelecionado().getCliente().getEmail().trim(),
										"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta())
												+ " -"
												+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
										mensagem));
								t.start();
							}

							if (co.getEmailNovo().trim().length() > 5) {
								Thread t = new Thread(new EnviarEmailThread(co.getEmailNovo().trim(),
										"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta())
												+ " -"
												+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
										mensagem));
								t.start();
							}

							if (co.getPontoColetaSelecionado().getCliente().getEmail2().trim().length() > 5
									&& !co.getPontoColetaSelecionado().getCliente().getEmail2().trim().equalsIgnoreCase(
											co.getPontoColetaSelecionado().getCliente().getEmail().trim())) {
								Thread t = new Thread(new EnviarEmailThread(
										co.getPontoColetaSelecionado().getCliente().getEmail2().trim(),
										"<NÃO RESPONDA> Coleta de Resíduos " + format.format(co.getDataUltimaColeta())
												+ " -"
												+ co.getPontoColetaSelecionado().getCliente().getNomeRazaoSocial(),
										mensagem));
								t.start();
							}

							// formatar a mensagem
						}
					}

				}
			}
		}
		// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - antes
		// consulta");
		// logTarefasService.inserirAlterar(logTarefasProblema);
		lc = new ArrayList<>();

		DateFormat formatD = new SimpleDateFormat("d");
		DateFormat formatM = new SimpleDateFormat("M");
		DateFormat formatA = new SimpleDateFormat("yyyy");
		// System.out.println(formatD.format(new Date()));
		// System.out.println(formatM.format(new Date()));
		// System.out.println(formatA.format(new Date()));
		// System.out.println("RETORNANDO..."+ new Date());
		if (acessoTotal) {
			lc = daoColetaApp.listar(ColetaApp.class,
					"diaUltimaColeta=" + formatD.format(new Date()) + " and mesUltimaColeta="
							+ formatM.format(new Date()) + " and anoUltimaColeta=" + formatA.format(new Date())
							+ " order by dataUltimaColeta desc");
		} else {
			// System.out.println("AQU");
			lc = daoColetaApp.listar(ColetaApp.class,
					"diaUltimaColeta=" + formatD.format(new Date()) + " and mesUltimaColeta="
							+ formatM.format(new Date()) + " and anoUltimaColeta=" + formatA.format(new Date())
							+ " and veiculo.id=" + veiculo + " order by dataUltimaColeta desc");

		}
		lc.forEach(ob -> {
			ob.setAssinatura("");
			ob.getPontoColetaSelecionado().setSenha("");
			ob.getPontoColetaSelecionado().getCliente().setSenha("");
			ob.setAssinaturaByte(null);
		});
		GenericEntity<List<ColetaApp>> lista = new GenericEntity<List<ColetaApp>>(lc) {
		};
		// logTarefasProblema.setDescricao(logTarefasProblema.getDescricao() + " - antes
		// retorno: " + lc.size());
		// logTarefasService.inserirAlterar(logTarefasProblema);

		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/pontosColeta")
	public Response buscarPontosColeta(@QueryParam("dataUltimaAtualizacao") String dataUltimaAtualizacao) {
		// Generate a date for Jan. 9, 2013, 10:11:12 AM
		// Calendar cal = Calendar.getInstance();
		// cal.set(2013, Calendar.JANUARY, 9, 10, 11, 12); //Year, month, day of month,
		// hours, minutes and seconds
		// Date date = cal.getTime();

		// System.out.println("Dentro da Lista de Inventáriossss");
		// System.out.println("Data Ultima: "+dataUltimaAtualizacao);
		List<PontoColeta> lp = daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class,
				"(dataUltimaAlteracao BETWEEN '" + dataUltimaAtualizacao
						+ "' and '2070-03-14 22:54:06') or dataUltimaAlteracao is null");
		// List<PontoColeta> lp =
		// daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class, "");

//		for (PontoColeta coleta : lp) {
//			// preencher ponto de coleta
//			List<ItensInformacaoFinanceiraPontoColeta> listaPontos = daoItensInformacaoFinanceiraPontoColeta
//					.listar(ItensInformacaoFinanceiraPontoColeta.class, "pontoColeta.id=" + coleta.getId()
//							+ " and informacoesFinanceiras.situacaoContrato='Ativo' and informacoesFinanceiras.status is true and informacoesFinanceiras.situacaoContrato='Ativo'");
//
//			if (listaPontos.size() > 0) {
//				if (listaPontos.get(0).getInformacoesFinanceiras() != null) {
//					if (listaPontos.get(0).getInformacoesFinanceiras().getSituacaoContrato().equals("Ativo")) {
//						List<ItensInformacaoFinanceiraGrupo> listaGrupos = daoItensInformacaoFinanceiraGrupo
//								.listar(ItensInformacaoFinanceiraGrupo.class,
//										"informacoesFinanceiras.id="
//												+ listaPontos.get(0).getInformacoesFinanceiras().getId()
//												+ " order by grupoResiduos.ordem");
//						for (ItensInformacaoFinanceiraGrupo g : listaGrupos) {
//							if (coleta.getGruposColetados().equals("")) {
//								coleta.setGruposColetados(g.getGrupoResiduos().getDescricao());
//							} else {
//								coleta.setGruposColetados(
//										coleta.getGruposColetados() + ";" + g.getGrupoResiduos().getDescricao());
//							}
//
//						}
//						coleta.setGruposColetados(
//								coleta.getGruposColetados() + ";");
//
//					} else {
//						coleta.setContratoAtivo(false);
//					}
//				}
//			}
//		}

		// fim preencher ponto de coleta

		GenericEntity<List<PontoColeta>> lista = new GenericEntity<List<PontoColeta>>(lp) {
		};
		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/todosPontosColeta")
	public Response buscarTodosPontosColeta() {
		// Generate a date for Jan. 9, 2013, 10:11:12 AM
		// Calendar cal = Calendar.getInstance();
		// cal.set(2013, Calendar.JANUARY, 9, 10, 11, 12); //Year, month, day of month,
		// hours, minutes and seconds
		// Date date = cal.getTime();

		// System.out.println("Dentro da Lista de Inventáriossss");
		// System.out.println("Data Ultima: "+dataUltimaAtualizacao);
		// List<PontoColeta> lp =
		// daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class,
		// "(dataUltimaAlteracao BETWEEN '"+dataUltimaAtualizacao+"' and '2070-03-14
		// 22:54:06') or dataUltimaAlteracao is null");
		List<PontoColeta> lp = daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class, "");

		GenericEntity<List<PontoColeta>> lista = new GenericEntity<List<PontoColeta>>(lp) {
		};
		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/pontosColetaOld")
	public Response buscarPontosColetaOld() {
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
						List<ItensInformacaoFinanceiraGrupo> listaGrupos = daoItensInformacaoFinanceiraGrupo
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

					} else {
						coleta.setContratoAtivo(false);
					}
				}
			}
		}

		// fim preencher ponto de coleta

		GenericEntity<List<PontoColeta>> lista = new GenericEntity<List<PontoColeta>>(lp) {
		};
		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/todasCidades")
	public Response buscarTodasCidades() {
		// System.out.println("Dentro da Lista de Inventáriossss");
		List<Cidade> lr = daoCidade.listaComStatus(Cidade.class);

		GenericEntity<List<Cidade>> lista = new GenericEntity<List<Cidade>>(lr) {

		};
		return Response.status(200).entity(lista).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/inserirMensagemSite")
	public Response inserirMensagemSite(MensagemSite mensagemSite) {
//System.out.println("AWUIIII");
		mensagemSiteService.inserirAlterar(mensagemSite);

		String json = "{\"situacao\":\"feito\"}";// convert entity to json
		return Response.ok(json, MediaType.APPLICATION_JSON).build();

	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/atualizarDados")
	public Response inserirAtualizacaoDados(MensagemSite mensagemSite) {
		String retorno = mensagemSiteService.salvar(mensagemSite).toString();
		return Response.ok(retorno, MediaType.APPLICATION_JSON).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/inserirOcorrencia")
	public Response inserirOcorrencia(List<Ocorrencia> ocorrencias) {
		System.out.println("Quantidade: " + ocorrencias.size());

		for (Ocorrencia o : ocorrencias) {
			o.setStatus(true);
			if (o.getAtividade() != null && o.getAtividade().getId() != null) {
				Atividade a = daoAtividade.buscarPorId(Atividade.class, o.getAtividade().getId());
				o.setAtividade(a);
			} else {
				o.setAtividade(null);
			}
			if (o.getIndicadorRelacionado() != null && o.getIndicadorRelacionado().getId() != null) {
				Indicador a = daoIndicador.buscarPorId(Indicador.class, o.getIndicadorRelacionado().getId());
				o.setIndicadorRelacionado(a);
			} else {
				o.setIndicadorRelacionado(null);
			}
			// o.setAtividade(null);
			// o.setIndicadorRelacionado(null);
			System.out.println(o.getDescricao());
			System.out.println(o.getAtividade());
			ocorrenciaService.inserirAlterar(o);
		}

		GenericEntity<Retorno> lista = new GenericEntity<Retorno>(new Retorno("Sucesso")) {

		};

		// return Response.status(200).entity(new Retorno("Sucesso")).build();
		String json = "{\"situacao\":\"feito\"}";// convert entity to json
		return Response.ok(json, MediaType.APPLICATION_JSON).build();

	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/inserirLancamento")
	public Response inserirLancamento(List<ItensLancamento> itensLancamento) {

		for (ItensLancamento o : itensLancamento) {
			o.setStatus(true);
			Lancamento lan = new Lancamento();
			lan.setStatus(true);
			lan.setDataLancamento(o.getDataLancamento());

			o.setStatus(true);
			if (o.getAtividade() != null && o.getAtividade().getId() != null) {
				Atividade a = daoAtividade.buscarPorId(Atividade.class, o.getAtividade().getId());
				lan.setAtividade(a);
			} else {
				o.setAtividade(null);
			}
			lancamentoService.inserirAlterar(lan);
			if (o.getGrupoLancamento() != null && o.getGrupoLancamento().getId() != null) {
				GrupoLancamento a = daoGrupoLancamento.buscarPorId(GrupoLancamento.class,
						o.getGrupoLancamento().getId());
				o.setGrupoLancamento(a);
			} else {
				o.setGrupoLancamento(null);
			}
			o.setLancamento(lan);
			// o.setAtividade(null);
			// o.setIndicadorRelacionado(null);
			System.out.println(o.getDescricao());
			System.out.println(o.getAtividade());
			itensLancamentoService.inserirAlterar(o);
		}

		GenericEntity<Retorno> lista = new GenericEntity<Retorno>(new Retorno("Sucesso")) {

		};

		// return Response.status(200).entity(new Retorno("Sucesso")).build();
		String json = "{\"situacao\":\"feito\"}";// convert entity to json
		return Response.ok(json, MediaType.APPLICATION_JSON).build();

	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/variaveis")
	public Response variaveisCalculo() {
//		System.out.println("Dentro da Lista de Inventáriossss");
		List<GrupoLancamento> processos = daoGrupoLancamento.listaComStatus(GrupoLancamento.class);

		GenericEntity<List<GrupoLancamento>> lista = new GenericEntity<List<GrupoLancamento>>(processos) {

		};
		return Response.status(200).entity(lista).build();

	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/indicadores")
	public Response indicador() {
		System.out.println("Dentro da Lista de Inventáriossss");
		List<Indicador> lista = daoIndicador.listaComStatus(Indicador.class);

		for (Indicador p : lista) {
			p.setCategoriaIndicador(null);
			p.setProcesso(null);
		}

		GenericEntity<List<Indicador>> retorno = new GenericEntity<List<Indicador>>(lista) {

		};
		System.out.println("TAM: " + lista.size());

		return Response.status(200).entity(retorno).build();

	}

	/* @GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/autenticar")
	public Response autenticar(@QueryParam("email") String email, @QueryParam("senha") String senha) {
//			System.out.println("Dentro da Lista de Inventáriossss");
		List<Usuario> lista = daoUsuario.listar(Usuario.class, "email='" + email + "'");
		Usuario usuario = null;
		if (lista.size() > 0) {
			if (CriptografiaSenha.decript(senha, lista.get(0).getSenha())) {
				usuario = lista.get(0);
			}
		}

		if (usuario != null) {
			GenericEntity<Usuario> retorno = new GenericEntity<Usuario>(usuario) {
			};
			return Response.status(200).entity(retorno).build();
		} else {
			return Response.status(401).entity("falhou").build();
		}
	} */

}
