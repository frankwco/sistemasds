package services;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import base.modelo.AlertaDTO;
import base.modelo.Anexo;
import base.modelo.AvisoDuplicidadeDTO;
import base.modelo.CategoriaDespesa;
import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.CobrancaClienteDTO;
import base.modelo.CobrancaListaDTO;
import base.modelo.CobrancaResumoDTO;
import base.modelo.CobrancaWhatsappRequestDTO;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppPontosSemColeta;
import base.modelo.ColetaAppProximaColeta;
import base.modelo.Fornecedor;
import base.modelo.LancamentoVeiculo;
import base.modelo.LeituraOdometro;
import base.modelo.Lembrete;
import base.modelo.PontoColeta;
import base.modelo.PontoColetaRetorno;
import base.modelo.Processo;
import base.modelo.RelatorioCobranca;
import base.modelo.RelatorioCobrancaItem;
import base.modelo.TipoDocumento;
import base.modelo.Usuario;
import base.modelo.Veiculo;
import base.service.AlertaService;
import base.service.AnexoService;
import base.service.ClienteService;
import base.service.ColetaAppService;
import base.service.FirebaseColetaSyncService;
import base.service.FornecedorService;
import base.service.LancamentoVeiculoService;
import base.service.LeituraOdometroService;
import base.service.LembreteService;
import base.service.PontoColetaService;
import base.service.ProcessoService;
import base.service.RelatorioCobrancaService;
import base.service.UsuarioService;
import dao.ColetaAppDAO;
import dao.GenericDAO;
import util.MensagemRetorno;
import util.cobranca.GrupoConfirmadoCobranca;

/**
 * Endpoints usados exclusivamente pelo appgestaofacil (rastreados a partir
 * do código do app — todo lugar que chama enderecoApi()). Isolados aqui, em
 * vez de junto com o resto do {@link ServicoHello}, para poder exigir a
 * chave de API ({@link ExigeChaveApp}/{@link ChaveApiFilter}) sem afetar
 * outros consumidores do backend (ex.: site institucional, que usa
 * inserirMensagemSite/atualizarDados em ServicoHello).
 *
 * cadastrarColeta é um repasse fino para {@link ServicoHello#cadastroColeta}
 * — a lógica de negócio (e-mail, financeiro, checagem de duplicidade)
 * continua lá, intacta; só o endpoint em si (as anotações JAX-RS) foi
 * removido de lá para não ficar exposto duas vezes.
 */
@Path("/app")
@ExigeChaveApp
public class ServicoApp {

	@Inject
	private ServicoHello servicoHello;

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta;

	@Inject
	private ColetaAppService coletaAppService;

	@Inject
	private ColetaAppDAO coletaAppDAO;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp;

	@Inject
	private PontoColetaService pontoColetaService;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private GenericDAO<Fornecedor> daoFornecedor;

	@Inject
	private GenericDAO<TipoDocumento> daoTipoDocumento;

	@Inject
	private GenericDAO<CategoriaDespesa> daoCategoriaDespesa;

	@Inject
	private GenericDAO<Cidade> daoCidade;

	@Inject
	private GenericDAO<Usuario> daoUsuario;

	@Inject
	private UsuarioService usuarioService;

	@Inject
	private ProcessoService processoService;

	@Inject
	private FirebaseColetaSyncService firebaseColetaSyncService;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamentoVeiculo;

	@Inject
	private LancamentoVeiculoService lancamentoVeiculoService;

	@Inject
	private LeituraOdometroService leituraOdometroService;

	@Inject
	private GenericDAO<Anexo> daoAnexo;

	@Inject
	private AnexoService anexoService;

	@Inject
	private GenericDAO<Lembrete> daoLembrete;

	@Inject
	private LembreteService lembreteService;

	@Inject
	private AlertaService alertaService;

	@Inject
	private FornecedorService fornecedorService;

	@Inject
	private RelatorioCobrancaService relatorioCobrancaService;

	@Inject
	private ClienteService clienteService;

	@Inject
	private GenericDAO<Cliente> daoCliente;

	@GET
	@Produces("application/json;charset=UTF-8")
	@Path("/consumirFirebase")
	public Response consumirFirebase() {
		FirebaseColetaSyncService.Resultado resultado = firebaseColetaSyncService.sincronizar();
		return Response.status(200).entity(resultado).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/proximaColeta")
	public Response buscarProximaColeta(@QueryParam("idPontoColeta") Long idPontoColeta) {
		PontoColeta ponto = daoPontoColeta.buscarPorId(PontoColeta.class, idPontoColeta);

		ColetaAppProximaColeta proxima = coletaAppService.proximaColeta(ponto);

		return Response.status(200).entity(proxima).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/diasSemColetaPontos")
	public Response pontosSemColetas() {
		GenericEntity<List<ColetaAppPontosSemColeta>> lista = new GenericEntity<List<ColetaAppPontosSemColeta>>(
				coletaAppDAO.diasSemColetaPonto()) {
		};

		return Response.status(200).entity(lista).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cadastrarColeta")
	public Response cadastrarColeta(List<ColetaApp> lc) {
		return servicoHello.cadastroColeta(lc);
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/todasColetas")
	public Response buscarTodasColetas(@QueryParam("idPontoColeta") Long idPontoColeta,
			@QueryParam("dataConsulta") String dataConsulta) {
		List<ColetaApp> lc = new ArrayList<>();

		lc = daoColetaApp.listar(ColetaApp.class,
				"pontoColetaSelecionado.id=" + idPontoColeta + " and dataUltimaColeta BETWEEN '" + dataConsulta
						+ "' and '2070-03-14 22:54:06' order by dataUltimaColeta desc");

		GenericEntity<List<ColetaApp>> lista = new GenericEntity<List<ColetaApp>>(lc) {
		};

		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/todosPontosColetaReduzidos")
	public Response buscarTodosPontosColetaReduzido(
			@QueryParam("dataUltimaAtualizacao") String dataUltimaAtualizacao) {
		List<PontoColeta> lp = new ArrayList<>();
		if (dataUltimaAtualizacao.equals("nao")) {
			lp = daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class, "");
		} else {
			lp = daoPontoColeta.listaPontosColetaAtivos(PontoColeta.class, "(pc.dataUltimaAlteracao BETWEEN '"
					+ dataUltimaAtualizacao + "' and '2070-03-14 22:54:06') or pc.dataUltimaAlteracao is null ");
		}

		List<PontoColetaRetorno> lpRetorno = new ArrayList<>();

		DateFormat format = new SimpleDateFormat("MM");
		Integer mes = Integer.parseInt(format.format(new Date()));

		for (PontoColeta p : lp) {
			PontoColetaRetorno r = new PontoColetaRetorno();

			r.setPrimeiraColeta(p.getPrimeiraColeta());
			r.setApelido(p.getApelido() + " " + p.getCliente().getApelido());
			r.setAcessoSistema(p.getAcessoSistema());
			r.setAnoUltimaColeta(p.getAnoUltimaColeta());
			r.setCep(p.getCep());
			r.setDataUltimaColeta(p.getDataUltimaColeta());
			r.setDiaUltimaColeta(p.getDiaUltimaColeta());
			r.setEmail(p.getCliente().getEmail());
			r.setEndereco(p.getEnderecoCompletoSemCidade());
			r.setGruposColetados(p.getGruposColetados());
			r.setHoraUltimaColeta(p.getHoraUltimaColeta());
			r.setId(p.getId());
			r.setIdCidade(p.getCidade().getId());
			r.setMesUltimaColeta(p.getMesUltimaColeta());
			r.setMinutoUltimaColeta(p.getMinutoUltimaColeta());
			r.setNomeCidade(p.getCidade().getNome());
			r.setNomeFantasia(p.getNomeFantasia());
			r.setNomeRazaoSocial(p.getNomeRazaoSocial());
			r.setSegundoUltimaColeta(p.getSegundoUltimaColeta());
			r.setStatus(p.getStatus());
			r.setQuantidadeColetaMes(p.getQuantidadeColetaMes());
			r.setPeriodicidadeColeta(p.getPeriodicidadeColeta());
			r.setSituacaoAviso(p.getSituacaoAviso());
			r.setAviso(p.getAviso().trim());
			r.setNomeRazaoSocialNomeFantasiaEndereco(p.getNomeRazaoSocialNomeFantasiaEndereco());
			r.setTelefone1(p.getTelefone1());
			r.setTelefone2(p.getTelefone2());
			r.setEnderecoCompleto(p.getEnderecoCompleto());
			if (p.getCliente() != null) {
				r.setTelefone1Cliente(
						!p.getTelefone1().equals(p.getCliente().getTelefone1()) ? p.getCliente().getTelefone1() : "");
				r.setTelefone2Cliente(
						!p.getTelefone2().equals(p.getCliente().getTelefone2()) ? p.getCliente().getTelefone2() : "");
			}

			if (p.getMesColeta() != mes) {
				p.setMesColeta(mes);
				p.setQuantidadeColetadaMes(0);
				pontoColetaService.inserirAlterar(p);
			}

			r.setQuantidadeColetadaMes(p.getQuantidadeColetadaMes());

			lpRetorno.add(r);
		}

		GenericEntity<List<PontoColetaRetorno>> lista = new GenericEntity<List<PontoColetaRetorno>>(lpRetorno) {
		};

		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/veiculos")
	public Response buscarVeiculos() {
		List<Veiculo> lr = daoVeiculo.listaComStatus(Veiculo.class);

		GenericEntity<List<Veiculo>> lista = new GenericEntity<List<Veiculo>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cidades")
	public Response buscarCidades() {
		List<Cidade> lr = daoCidade.listar(Cidade.class, "selecionada is true");

		GenericEntity<List<Cidade>> lista = new GenericEntity<List<Cidade>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/usuarioAu")
	public Response usuarioAu(Usuario u) {
		Usuario retorno = new Usuario();
		retorno.setAcessoSistema(false);

		if (u.getEmail() != null && u.getSenha() != null) {
			List<Usuario> lu = daoUsuario.listarPorCampo(Usuario.class, "email", u.getEmail().trim());
			BCryptPasswordEncoder b = new BCryptPasswordEncoder();

			if (!lu.isEmpty() && b.matches(u.getSenha(), lu.get(0).getSenha())) {
				Usuario usu = lu.get(0);
				// o app precisa do id do usuário autenticado pra filtrar "Meus
				// Lançamentos" pelo login de verdade, em vez do motorista
				// selecionável localmente na tela inicial (que qualquer um podia
				// trocar e ver lançamentos de outra pessoa)
				retorno.setId(usu.getId());
				retorno.setAcessoApp(usu.getAcessoApp());
				retorno.setAcessoTotal(usu.getAcessoTotal());
				retorno.setAcessoSistema(Boolean.TRUE.equals(usu.getAcessoSistema()));
				retorno.setEmail(usu.getEmail());
				retorno.setNome(usu.getNome());
			}
		}

		return Response.status(200).entity(retorno).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/recuperarSenha")
	public Response recuperarSenha(Usuario u) {
		MensagemRetorno r = new MensagemRetorno();

		r.setMensagem(usuarioService.recuperarSenhaLogin(u.getEmail()));
		if (r.getMensagem().equals("E-mail enviado")) {
			r.setSucesso(true);
		}

		return Response.status(200).entity(r).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/processos")
	public Response processos() {
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Processo p = new Processo();
		p.setDescricao("" + format.format(new Date()));
		processoService.inserirAlterar(p);
		return Response.status(200).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/motoristas")
	public Response motoristas() {
		List<Usuario> usuarios = daoUsuario.listar(Usuario.class, "motorista is true");

		GenericEntity<List<Usuario>> retorno = new GenericEntity<List<Usuario>>(usuarios) {
		};
		return Response.status(200).entity(retorno).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/fornecedores")
	public Response fornecedores() {
		List<Fornecedor> lr = daoFornecedor.listaComStatus(Fornecedor.class);

		GenericEntity<List<Fornecedor>> lista = new GenericEntity<List<Fornecedor>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Cadastro de fornecedor/posto direto pelo app, quando o motorista não
	 * encontra o que precisa na lista sincronizada — diferente do cadastro
	 * automático da Captura por IA ({@link FornecedorService#cadastrarAutomatico}),
	 * aqui quem escolhe o nome e o tipo é o próprio motorista.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/fornecedores")
	public Response cadastrarFornecedor(Fornecedor f) {
		if (f == null || f.getNome() == null || f.getNome().trim().isEmpty()) {
			return Response.status(400).build();
		}
		Fornecedor criado = fornecedorService.cadastrar(f.getNome().trim(), f.getTipo());
		return Response.status(200).entity(criado).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/tiposDocumento")
	public Response tiposDocumento() {
		List<TipoDocumento> lr = daoTipoDocumento.listaComStatus(TipoDocumento.class);

		GenericEntity<List<TipoDocumento>> lista = new GenericEntity<List<TipoDocumento>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/categoriasDespesa")
	public Response categoriasDespesa() {
		List<CategoriaDespesa> lr = daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class);

		GenericEntity<List<CategoriaDespesa>> lista = new GenericEntity<List<CategoriaDespesa>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Recebe da fila offline do app (RNF04) os lançamentos de Abastecimento e
	 * Despesa/Serviço feitos em rota (RNF01). Resolve as referências (veículo,
	 * motorista, categoria, fornecedor) pelo id recebido — o objeto que chega do
	 * app só tem o id preenchido nesses relacionamentos — antes de delegar pra
	 * {@link LancamentoVeiculoService#salvar}, que já cuida de auditoria,
	 * categoria de combustível e leitura de odômetro (RF29). Se o app mandou
	 * um {@code fornecedorSugerido} (nome que a IA leu num comprovante mas não
	 * bateu com nenhum fornecedor sincronizado no aparelho) e não tem
	 * fornecedor resolvido, cadastra automaticamente — mesma lógica da
	 * Captura por IA administrativa ({@link base.controle.CapturaIaMB}).
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/lancamentos")
	public Response cadastrarLancamentos(List<LancamentoVeiculo> lancamentos) {
		for (LancamentoVeiculo l : lancamentos) {
			if (l.getVeiculo() != null && l.getVeiculo().getId() != null) {
				l.setVeiculo(daoVeiculo.buscarPorId(Veiculo.class, l.getVeiculo().getId()));
			}
			if (l.getMotorista() != null && l.getMotorista().getId() != null) {
				l.setMotorista(daoUsuario.buscarPorId(Usuario.class, l.getMotorista().getId()));
			}
			if (l.getCategoriaDespesa() != null && l.getCategoriaDespesa().getId() != null) {
				l.setCategoriaDespesa(daoCategoriaDespesa.buscarPorId(CategoriaDespesa.class,
						l.getCategoriaDespesa().getId()));
			}
			if (l.getFornecedor() != null && l.getFornecedor().getId() != null) {
				l.setFornecedor(daoFornecedor.buscarPorId(Fornecedor.class, l.getFornecedor().getId()));
			} else if (l.getFornecedorSugerido() != null && !l.getFornecedorSugerido().trim().isEmpty()) {
				l.setFornecedor(fornecedorService.cadastrarAutomatico(l.getFornecedorSugerido().trim(), l.getTipo(),
						l.getCategoriaDespesa()));
			}

			String usuarioLogado = l.getMotorista() != null ? l.getMotorista().getNome() : "App";
			try {
				lancamentoVeiculoService.salvar(l, usuarioLogado);
			} catch (IllegalStateException e) {
				return Response.status(409).entity(e.getMessage()).build();
			}
		}

		// devolve com os ids gerados — o app precisa deles pra poder subir o
		// anexo (RF09) direto pro Firebase Storage no caminho
		// "anexos/{ano}/{mes}/{lancamentoId}/..." logo em seguida
		GenericEntity<List<LancamentoVeiculo>> lista = new GenericEntity<List<LancamentoVeiculo>>(lancamentos) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Checagem best-effort de duplicidade (mesma ideia de
	 * {@link base.controle.CapturaIaMB#verificarDuplicidade}) chamada pelo app
	 * na tela de revisão da Captura por IA, antes de confirmar — só avisa,
	 * nunca bloqueia, e o app ignora silenciosamente se estiver offline (é só
	 * uma tela de reforço, não uma regra de negócio obrigatória). Dois
	 * sinais, nessa ordem: mesmo número de documento já usado, ou mesmo
	 * veículo com o mesmo km do odômetro já registrado.
	 */
	@GET
	@Produces("application/json;charset=UTF-8")
	@Path("/verificarDuplicidade")
	public Response verificarDuplicidade(@QueryParam("veiculoId") Long veiculoId, @QueryParam("km") Long km,
			@QueryParam("numeroDocumento") String numeroDocumento) {
		if (numeroDocumento != null && !numeroDocumento.trim().isEmpty()) {
			String numeroEscapado = numeroDocumento.trim().replace("'", "''");
			List<LancamentoVeiculo> porDocumento = daoLancamentoVeiculo.listar(LancamentoVeiculo.class,
					"numeroDocumento='" + numeroEscapado + "' and status is true");
			if (!porDocumento.isEmpty()) {
				return Response.status(200)
						.entity(new AvisoDuplicidadeDTO("Já existe um lançamento com o número de documento \""
								+ numeroDocumento.trim() + "\": " + descreverLancamento(porDocumento.get(0))
								+ ". Pode ser este comprovante lançado de novo."))
						.build();
			}
		}

		if (veiculoId != null && km != null) {
			List<LancamentoVeiculo> porOdometro = daoLancamentoVeiculo.listar(LancamentoVeiculo.class,
					"veiculo.id=" + veiculoId + " and km=" + km + " and status is true");
			if (!porOdometro.isEmpty()) {
				return Response.status(200)
						.entity(new AvisoDuplicidadeDTO("Já existe um lançamento deste veículo com o mesmo odômetro ("
								+ km + " km): " + descreverLancamento(porOdometro.get(0))
								+ ". Pode ser um lançamento duplicado."))
						.build();
			}
		}

		return Response.status(200).entity(new AvisoDuplicidadeDTO(null)).build();
	}

	private String descreverLancamento(LancamentoVeiculo existente) {
		String data = existente.getData() != null ? new SimpleDateFormat("dd/MM/yyyy").format(existente.getData())
				: "data desconhecida";
		String valor = existente.getValor() != null ? String.format(" (R$ %.2f)", existente.getValor()) : "";
		return existente.getTipo() + " de " + data + valor;
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/extratoVeiculo")
	public Response extratoVeiculo(@QueryParam("veiculoId") Long veiculoId, @QueryParam("desde") String desde) {
		List<LancamentoVeiculo> lr = daoLancamentoVeiculo.listar(LancamentoVeiculo.class,
				"veiculo.id=" + veiculoId + " and data >= '" + desde + "' order by data desc");

		GenericEntity<List<LancamentoVeiculo>> lista = new GenericEntity<List<LancamentoVeiculo>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Lançamentos de um motorista, pra tela "Meus Lançamentos" do app —
	 * mesmo modelo de confiança já usado em {@link #cadastrarLancamentos}: o
	 * motoristaId vem auto-declarado pelo app (não tem sessão/token por
	 * usuário), então isso não é uma barreira de segurança de verdade, só
	 * evita mostrar lançamentos de outros motoristas na tela.
	 */
	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/lancamentosMotorista")
	public Response lancamentosMotorista(@QueryParam("motoristaId") Long motoristaId) {
		if (motoristaId == null) {
			return Response.status(400).build();
		}
		List<LancamentoVeiculo> lr = daoLancamentoVeiculo.listar(LancamentoVeiculo.class,
				"motorista.id=" + motoristaId + " order by data desc");

		GenericEntity<List<LancamentoVeiculo>> lista = new GenericEntity<List<LancamentoVeiculo>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Exclusão (soft-delete, mesmo campo {@code status} usado no resto do
	 * sistema) de um lançamento feito errado — só deixa excluir se o
	 * motoristaId informado bater com o dono do lançamento (mesma checagem
	 * best-effort do comentário de {@link #lancamentosMotorista}).
	 */
	@DELETE
	@Produces("application/json;charset=UTF-8")
	@Path("/lancamentos/{id}")
	public Response excluirLancamento(@PathParam("id") Long id, @QueryParam("motoristaId") Long motoristaId) {
		LancamentoVeiculo lancamento = daoLancamentoVeiculo.buscarPorId(LancamentoVeiculo.class, id);
		if (lancamento == null) {
			return Response.status(404).build();
		}
		if (motoristaId == null || lancamento.getMotorista() == null
				|| !motoristaId.equals(lancamento.getMotorista().getId())) {
			return Response.status(403).build();
		}
		lancamento.setStatus(false);
		lancamentoVeiculoService.inserirAlterar(lancamento);
		return Response.status(204).build();
	}

	/**
	 * Leitura avulsa de odômetro (RF29) feita pelo motorista em rota.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/leituraOdometro")
	public Response cadastrarLeituraOdometro(LeituraOdometro leitura) {
		if (leitura.getVeiculo() == null || leitura.getVeiculo().getId() == null || leitura.getKm() == null) {
			return Response.status(400).build();
		}

		Veiculo veiculo = daoVeiculo.buscarPorId(Veiculo.class, leitura.getVeiculo().getId());
		String usuarioLogado = "App";
		if (leitura.getCriadoPor() != null && !leitura.getCriadoPor().trim().isEmpty()) {
			usuarioLogado = leitura.getCriadoPor();
		}

		try {
			leituraOdometroService.registrar(veiculo, leitura.getKm(), leitura.getData(), "Manual", null,
					usuarioLogado);
		} catch (IllegalStateException e) {
			return Response.status(409).entity(e.getMessage()).build();
		}

		return Response.status(200).build();
	}

	/**
	 * Registra os metadados de um comprovante (RF09) já enviado pelo app
	 * diretamente pro Firebase Storage — o app faz o upload do binário com o
	 * client SDK e só avisa o backend do caminho gerado.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/anexos")
	public Response cadastrarAnexo(Anexo anexo) {
		if (anexo.getLancamento() == null || anexo.getLancamento().getId() == null || anexo.getCaminho() == null) {
			return Response.status(400).build();
		}

		LancamentoVeiculo lancamento = daoLancamentoVeiculo.buscarPorId(LancamentoVeiculo.class,
				anexo.getLancamento().getId());
		anexo.setLancamento(lancamento);
		anexo.setStatus(true);
		anexo.setCriadoEm(new Date());
		anexo.setCriadoPor(lancamento.getMotorista() != null ? lancamento.getMotorista().getNome() : "App");
		anexoService.inserirAlterar(anexo);

		return Response.status(200).build();
	}

	/**
	 * Central de alertas (RF18/RF32/RF34) pro motorista consultar em campo —
	 * mesma lógica agregada usada na tela administrativa.
	 */
	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/alertas")
	public Response alertas() {
		List<AlertaDTO> lr = alertaService.listarAlertas();

		GenericEntity<List<AlertaDTO>> lista = new GenericEntity<List<AlertaDTO>>(lr) {
		};
		return Response.status(200).entity(lista).build();
	}

	/**
	 * Lembrete avulso (RF31) cadastrado em campo pelo motorista.
	 */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/lembretes")
	public Response cadastrarLembrete(Lembrete lembrete) {
		if (lembrete.getVeiculo() != null && lembrete.getVeiculo().getId() != null) {
			lembrete.setVeiculo(daoVeiculo.buscarPorId(Veiculo.class, lembrete.getVeiculo().getId()));
		}
		if (lembrete.getMotorista() != null && lembrete.getMotorista().getId() != null) {
			lembrete.setMotorista(daoUsuario.buscarPorId(Usuario.class, lembrete.getMotorista().getId()));
		}

		lembrete.setStatus(true);
		lembrete.setConcluido(false);
		lembrete.setCriadoEm(new Date());
		lembrete.setCriadoPor(lembrete.getMotorista() != null ? lembrete.getMotorista().getNome() : "App");
		lembreteService.inserirAlterar(lembrete);

		return Response.status(200).entity(lembrete).build();
	}

	// ── Cobrança (WhatsApp) ──────────────────────────────────────────────
	// Porta pro app só a parte de WhatsApp da tela base/cobranca.xhtml (sem importação de
	// CSV/disparo de e-mail em massa, que continuam exclusivos do backoffice web).

	private static final List<String> STATUS_CONFIRMADOS_COBRANCA = Arrays.asList("CONFIRMADO_AUTOMATICO",
			"CONFIRMADO_MANUAL");

	/** Só usuários marcados como acessoSistema (nível admin) podem ver/mexer em cobrança pelo app. */
	private boolean temAcessoSistema(Long usuarioId) {
		if (usuarioId == null) {
			return false;
		}
		Usuario usu = daoUsuario.buscarPorId(Usuario.class, usuarioId);
		return usu != null && Boolean.TRUE.equals(usu.getAcessoSistema());
	}

	private CobrancaClienteDTO paraCobrancaDTO(GrupoConfirmadoCobranca grupo) {
		Cliente cliente = grupo.getCliente();

		CobrancaClienteDTO dto = new CobrancaClienteDTO();
		dto.setClienteId(cliente.getId());
		dto.setNome(cliente.getNomeRazaoSocial());
		dto.setCidade(cliente.getCidade() != null
				? cliente.getCidade().getNome() + "-" + cliente.getCidade().getEstado().getSigla()
				: null);
		dto.setTelefone1(cliente.getTelefone1());
		dto.setTelefone2(cliente.getTelefone2());
		dto.setTelefone1Whatsapp(cliente.getTelefone1Whatsapp());
		dto.setTelefone2Whatsapp(cliente.getTelefone2Whatsapp());
		dto.setValorTotalFormatado(grupo.getValorTotalFormatado());
		dto.setParcelasFormatadas(grupo.getParcelasFormatadas());
		dto.setNomesSacadoRelatorio(grupo.getNomesSacadoRelatorio());
		dto.setTemNomeDivergente(grupo.isTemNomeDivergente());
		dto.setMensagemWhatsapp(grupo.getMensagemWhatsapp());
		dto.setHistoricoAtual(cliente.getHistoricoCobrancaLinhasAtuais());
		dto.setHistoricoAnterior(cliente.getHistoricoCobrancaLinhasAnteriores());
		return dto;
	}

	/** Grupo (0 ou 1 elemento) de um único cliente, pra devolver o DTO atualizado após uma ação. */
	private CobrancaClienteDTO paraCobrancaDTODoCliente(Long clienteId) {
		List<RelatorioCobrancaItem> itens = relatorioCobrancaService.buscarItensEmAberto(clienteId);
		List<GrupoConfirmadoCobranca> grupos = relatorioCobrancaService.agruparPorCliente(itens);
		return grupos.isEmpty() ? null : paraCobrancaDTO(grupos.get(0));
	}

	@GET
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cobrancas")
	public Response buscarCobrancas(@QueryParam("usuarioId") Long usuarioId) {
		if (!temAcessoSistema(usuarioId)) {
			return Response.status(403).build();
		}

		List<RelatorioCobrancaItem> itens = relatorioCobrancaService
				.buscarItensAbertosPorStatus(STATUS_CONFIRMADOS_COBRANCA);
		List<GrupoConfirmadoCobranca> grupos = relatorioCobrancaService.agruparPorCliente(itens);

		List<CobrancaClienteDTO> dtos = new ArrayList<>();
		double valorTotalEmAberto = 0.;
		for (GrupoConfirmadoCobranca grupo : grupos) {
			dtos.add(paraCobrancaDTO(grupo));
			valorTotalEmAberto += grupo.getValorTotal();
		}

		CobrancaResumoDTO resumo = new CobrancaResumoDTO();
		RelatorioCobranca loteAtual = relatorioCobrancaService.buscarUltimoLote();
		if (loteAtual != null) {
			resumo.setNomeArquivo(loteAtual.getNomeArquivoOriginal());
			resumo.setDataArquivoFormatada(loteAtual.getDataCriacaoArquivo() != null
					? loteAtual.getDataCriacaoArquivoFormatada() : loteAtual.getDataImportacaoFormatada());
		}
		resumo.setTotalClientesEmAberto(dtos.size());
		resumo.setValorTotalEmAbertoFormatado(
				NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(valorTotalEmAberto));

		CobrancaListaDTO resposta = new CobrancaListaDTO();
		resposta.setResumo(resumo);
		resposta.setClientes(dtos);

		return Response.status(200).entity(resposta).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cobrancas/whatsapp/confirmar")
	public Response confirmarWhatsapp(CobrancaWhatsappRequestDTO requisicao) {
		if (!temAcessoSistema(requisicao.getUsuarioId())) {
			return Response.status(403).build();
		}
		Cliente cliente = daoCliente.buscarPorId(Cliente.class, requisicao.getClienteId());
		if (cliente == null || requisicao.getNumeroTelefone() == null) {
			return Response.status(404).build();
		}

		if (requisicao.getNumeroTelefone() == 1) {
			cliente.setTelefone1Whatsapp(true);
		} else {
			cliente.setTelefone2Whatsapp(true);
		}
		cliente.registrarEnvioCobranca(new Date(), "WHATSAPP");
		clienteService.inserirAlterar(cliente);

		return Response.status(200).entity(paraCobrancaDTODoCliente(cliente.getId())).build();
	}

	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cobrancas/whatsapp/desmarcar")
	public Response desmarcarWhatsapp(CobrancaWhatsappRequestDTO requisicao) {
		if (!temAcessoSistema(requisicao.getUsuarioId())) {
			return Response.status(403).build();
		}
		Cliente cliente = daoCliente.buscarPorId(Cliente.class, requisicao.getClienteId());
		if (cliente == null || requisicao.getNumeroTelefone() == null) {
			return Response.status(404).build();
		}

		if (requisicao.getNumeroTelefone() == 1) {
			cliente.setTelefone1Whatsapp(false);
		} else {
			cliente.setTelefone2Whatsapp(false);
		}
		cliente.removerUltimoRegistroCobranca();
		clienteService.inserirAlterar(cliente);

		return Response.status(200).entity(paraCobrancaDTODoCliente(cliente.getId())).build();
	}

	/** Chamado ao abrir o WhatsApp de fato — só grava histórico se aquele telefone já estava confirmado. */
	@POST
	@Produces("application/json;charset=UTF-8")
	@Consumes("application/json;charset=UTF-8")
	@Path("/cobrancas/whatsapp/registrarEnvio")
	public Response registrarEnvioWhatsapp(CobrancaWhatsappRequestDTO requisicao) {
		if (!temAcessoSistema(requisicao.getUsuarioId())) {
			return Response.status(403).build();
		}
		Cliente cliente = daoCliente.buscarPorId(Cliente.class, requisicao.getClienteId());
		if (cliente == null || requisicao.getNumeroTelefone() == null) {
			return Response.status(404).build();
		}

		boolean confirmado = requisicao.getNumeroTelefone() == 1 ? Boolean.TRUE.equals(cliente.getTelefone1Whatsapp())
				: Boolean.TRUE.equals(cliente.getTelefone2Whatsapp());
		if (confirmado) {
			cliente.registrarEnvioCobranca(new Date(), "WHATSAPP");
			clienteService.inserirAlterar(cliente);
		}

		return Response.status(200).entity(paraCobrancaDTODoCliente(cliente.getId())).build();
	}
}
