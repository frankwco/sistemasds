package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import base.modelo.Cliente;
import base.modelo.RelatorioCobranca;
import base.modelo.RelatorioCobrancaItem;
import base.service.ClienteService;
import base.service.RelatorioCobrancaService;
import util.ExibirMensagem;
import util.Mensagem;
import util.cobranca.CanalCobranca;
import util.cobranca.CanalCobrancaEmail;
import util.cobranca.GrupoConfirmadoCobranca;
import util.cobranca.LinhaRelatorioCobranca;
import util.cobranca.ParserRelatorioCobranca;
import util.cobranca.ResultadoParseRelatorioCobranca;

@ViewScoped
@Named("cobrancaMB")
public class CobrancaMB implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private RelatorioCobrancaService relatorioCobrancaService;

	@Inject
	private ClienteService clienteService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	private final CanalCobranca canalEmail = new CanalCobrancaEmail();

	private RelatorioCobranca loteAtual;
	private List<RelatorioCobrancaItem> itensConfirmados = new ArrayList<>();
	private List<RelatorioCobrancaItem> itensPendentes = new ArrayList<>();
	private List<GrupoConfirmadoCobranca> gruposConfirmados = new ArrayList<>();
	private List<GrupoConfirmadoCobranca> gruposSelecionados = new ArrayList<>();

	// ── Fila de disparo de cobrança (processada aos poucos via p:poll, não tudo de uma vez) ──
	private List<GrupoConfirmadoCobranca> filaEnvioCobranca = new ArrayList<>();
	private int totalFilaEnvioCobranca = 0;
	private int enviadosFilaCobranca = 0;
	private int sucessosFilaCobranca = 0;
	private List<String> falhasFilaCobranca = new ArrayList<>();
	private boolean envioCobrancaEmAndamento = false;
	private boolean envioCobrancaConcluido = false;

	// item pendente que está sendo resolvido na tela (dialog de revisão)
	private RelatorioCobrancaItem itemEmRevisao;
	private List<Cliente> candidatosDoItemEmRevisao = new ArrayList<>();
	private Long clienteEscolhidoManualmente;
	private String buscaClienteManual;
	private List<Cliente> resultadoBuscaClienteManual = new ArrayList<>();

	@PostConstruct
	public void init() {
		// não busca nada aqui — a tela abre vazia e o próprio JS dispara carregarDadosIniciais()
		// assim que a página renderiza, pra mostrar o loading em vez de travar a abertura da tela.
	}

	public void carregarDadosIniciais() {
		loteAtual = relatorioCobrancaService.buscarUltimoLote();
		carregarListasDoLote();
	}

	public void uploadRelatorio(FileUploadEvent evento) {
		try {
			UploadedFile arquivo = evento.getFile();
			if (arquivo == null || arquivo.getFileName() == null || arquivo.getFileName().trim().isEmpty()) {
				return;
			}

			ResultadoParseRelatorioCobranca resultado = ParserRelatorioCobranca.parsear(arquivo.getInputstream());
			List<LinhaRelatorioCobranca> linhas = resultado.getLinhas();

			if (linhas.isEmpty()) {
				ExibirMensagem.exibirMensagem(
						"Não foi possível reconhecer nenhuma linha de título neste arquivo. Confira se é o relatório 'Vencido' exportado do sicoobnet.");
				return;
			}

			RelatorioCobranca lote = new RelatorioCobranca();
			lote.setNomeArquivoOriginal(arquivo.getFileName());
			lote.setTotalLinhasIlegiveis(resultado.getTotalLinhasIlegiveis());
			lote.setDataCriacaoArquivo(resultado.getDataGeracaoArquivo());
			lote.setUsuario(usuarioSessaoMB.getUsuario() != null ? usuarioSessaoMB.getUsuario().getNome() : "");

			loteAtual = relatorioCobrancaService.processarLote(lote, linhas);
			carregarListasDoLote();
			marcarNovoLoteCobrancaNosClientes();

			String aviso = resultado.getTotalLinhasIlegiveis() > 0
					? " (" + resultado.getTotalLinhasIlegiveis() + " linha(s) do arquivo não puderam ser lidas)"
					: "";
			ExibirMensagem.exibirMensagem(loteAtual.getTotalLinhas() + " título(s) processado(s): "
					+ loteAtual.getTotalConfirmados() + " confirmado(s) automaticamente, "
					+ loteAtual.getTotalPendentes() + " pendente(s) de revisão." + aviso);

		} catch (Exception e) {
			e.printStackTrace();
			ExibirMensagem.exibirMensagem(Mensagem.ERRO + " " + e.getMessage());
		}
	}

	private void carregarListasDoLote() {
		if (loteAtual == null) {
			return;
		}
		itensConfirmados = relatorioCobrancaService
				.buscarItensAbertosPorStatus(Arrays.asList("CONFIRMADO_AUTOMATICO", "CONFIRMADO_MANUAL"));
		itensPendentes = relatorioCobrancaService.buscarItensAbertosPorStatus(Arrays.asList("PENDENTE_ESCOLHA"));
		gruposConfirmados = relatorioCobrancaService.agruparPorCliente(itensConfirmados);
		gruposSelecionados = new ArrayList<>();
	}

	/** Marca, no histórico de cobrança dos clientes deste novo lote, o início de uma nova rodada
	 * de cobrança — o histórico de rodadas anteriores é mantido, mas fica separado (e escondido
	 * por padrão na tela) das notificações desta rodada. */
	private void marcarNovoLoteCobrancaNosClientes() {
		Date dataLote = new Date();
		for (GrupoConfirmadoCobranca grupo : gruposConfirmados) {
			Cliente cliente = grupo.getCliente();
			cliente.marcarNovoLoteCobranca(dataLote);
			clienteService.inserirAlterar(cliente);
		}
	}

	// ── Revisão de pendentes ──────────────────────────────────────────────

	/** Nomes dos clientes candidatos de um pendente, pra mostrar direto na tabela (sem abrir o dialog). */
	public String buscarNomesCandidatos(RelatorioCobrancaItem item) {
		List<Cliente> candidatos = relatorioCobrancaService
				.buscarClientesPorIds(relatorioCobrancaService.parseIds(item.getCandidatosClienteIds()));
		if (candidatos.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < candidatos.size(); i++) {
			if (i > 0) {
				sb.append(" | ");
			}
			sb.append(candidatos.get(i).getNomeRazaoSocialCpfCnpj());
		}
		return sb.toString();
	}

	public void abrirRevisao(RelatorioCobrancaItem item) {
		itemEmRevisao = item;
		clienteEscolhidoManualmente = null;
		buscaClienteManual = "";
		resultadoBuscaClienteManual = new ArrayList<>();
		candidatosDoItemEmRevisao = relatorioCobrancaService
				.buscarClientesPorIds(relatorioCobrancaService.parseIds(item.getCandidatosClienteIds()));
	}

	public void buscarClienteManualmente() {
		resultadoBuscaClienteManual = relatorioCobrancaService.buscarClientesPorNome(buscaClienteManual);
	}

	public void confirmarResolucaoManual() {
		if (itemEmRevisao == null || clienteEscolhidoManualmente == null) {
			ExibirMensagem.exibirMensagem("Selecione um cliente antes de confirmar.");
			return;
		}
		relatorioCobrancaService.resolverManualmente(itemEmRevisao.getId(), clienteEscolhidoManualmente);
		carregarListasDoLote();
		itemEmRevisao = null;
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
	}

	public void ignorar(RelatorioCobrancaItem item) {
		relatorioCobrancaService.ignorarItem(item.getId());
		carregarListasDoLote();
	}

	// ── Disparo de cobrança ───────────────────────────────────────────────
	// Processado aos poucos (um cliente por "tick" do p:poll na tela), não tudo de uma vez numa
	// única requisição — assim dá pra mostrar "X de Y" de verdade e não estourar limite de envio
	// do provedor de email disparando tudo junto.

	public void iniciarDisparoCobranca() {
		if (gruposSelecionados == null || gruposSelecionados.isEmpty()) {
			ExibirMensagem.exibirMensagem("Selecione ao menos um cliente para cobrar.");
			return;
		}
		filaEnvioCobranca = new ArrayList<>(gruposSelecionados);
		totalFilaEnvioCobranca = filaEnvioCobranca.size();
		enviadosFilaCobranca = 0;
		sucessosFilaCobranca = 0;
		falhasFilaCobranca = new ArrayList<>();
		envioCobrancaEmAndamento = true;
		envioCobrancaConcluido = false;
	}

	/** Chamado a cada tick do p:poll — processa só UM cliente da fila por vez. */
	public void processarProximoDaFilaCobranca() {
		if (!envioCobrancaEmAndamento || filaEnvioCobranca.isEmpty()) {
			envioCobrancaEmAndamento = false;
			return;
		}

		GrupoConfirmadoCobranca grupo = filaEnvioCobranca.remove(0);

		// manda todos os títulos em aberto do cliente (de qualquer importação), não só os deste lote
		List<RelatorioCobrancaItem> itensEmAbertoDoCliente = relatorioCobrancaService
				.buscarItensEmAberto(grupo.getCliente().getId());
		boolean enviado = canalEmail.enviar(grupo.getCliente(), itensEmAbertoDoCliente);
		if (enviado) {
			sucessosFilaCobranca++;
			registrarHistoricoCobranca(grupo.getCliente(), canalEmail.getNome());
			List<Long> idsDesteCliente = new ArrayList<>();
			for (RelatorioCobrancaItem item : grupo.getItens()) {
				idsDesteCliente.add(item.getId());
			}
			relatorioCobrancaService.marcarCobrancaEnviada(idsDesteCliente, canalEmail.getNome());
		} else {
			falhasFilaCobranca.add(grupo.getCliente().getNomeRazaoSocial());
		}
		enviadosFilaCobranca++;

		if (filaEnvioCobranca.isEmpty()) {
			envioCobrancaEmAndamento = false;
			envioCobrancaConcluido = true;
			carregarListasDoLote();
		}
	}

	/** Fecha o modal de resultado do envio (usuário clicou em "Fechar"). */
	public void fecharDialogEnvioCobranca() {
		envioCobrancaConcluido = false;
	}

	// ── Confirmação de WhatsApp ──────────────────────────────────────────
	// Marca no cadastro do cliente que aquele telefone é WhatsApp, pra próxima vez (nesta
	// tela ou em qualquer outra que use o cadastro) já vir marcado, sem precisar confirmar de novo.
	// Confirmar (ou reenviar pra um número já confirmado) conta como uma cobrança feita por
	// WhatsApp, então entra no histórico junto com os envios por email.

	private static final String CANAL_WHATSAPP = "WHATSAPP";

	public void confirmarTelefone1Whatsapp(GrupoConfirmadoCobranca grupo) {
		grupo.getCliente().setTelefone1Whatsapp(true);
		registrarHistoricoCobranca(grupo.getCliente(), CANAL_WHATSAPP);
	}

	public void confirmarTelefone2Whatsapp(GrupoConfirmadoCobranca grupo) {
		grupo.getCliente().setTelefone2Whatsapp(true);
		registrarHistoricoCobranca(grupo.getCliente(), CANAL_WHATSAPP);
	}

	/** Desfaz a confirmação, caso o usuário tenha marcado errado — remove também o último
	 * registro do histórico, que tinha sido criado pela confirmação. */
	public void desmarcarTelefone1Whatsapp(GrupoConfirmadoCobranca grupo) {
		grupo.getCliente().setTelefone1Whatsapp(false);
		grupo.getCliente().removerUltimoRegistroCobranca();
		clienteService.inserirAlterar(grupo.getCliente());
	}

	public void desmarcarTelefone2Whatsapp(GrupoConfirmadoCobranca grupo) {
		grupo.getCliente().setTelefone2Whatsapp(false);
		grupo.getCliente().removerUltimoRegistroCobranca();
		clienteService.inserirAlterar(grupo.getCliente());
	}

	/** Chamado (via JS) ao clicar no botão verde de mandar mensagem — só registra no
	 * histórico se o número já estava confirmado como WhatsApp (senão quem registra é a
	 * própria confirmação, em confirmarTelefone1/2Whatsapp). */
	public void registrarEnvioWhatsapp(GrupoConfirmadoCobranca grupo, int numeroTelefone) {
		Cliente cliente = grupo.getCliente();
		boolean confirmado = numeroTelefone == 1 ? Boolean.TRUE.equals(cliente.getTelefone1Whatsapp())
				: Boolean.TRUE.equals(cliente.getTelefone2Whatsapp());
		if (confirmado) {
			registrarHistoricoCobranca(cliente, CANAL_WHATSAPP);
		}
	}

	private void registrarHistoricoCobranca(Cliente cliente, String canal) {
		cliente.registrarEnvioCobranca(new Date(), canal);
		clienteService.inserirAlterar(cliente);
	}

	// ── Getters/Setters ───────────────────────────────────────────────────

	public RelatorioCobranca getLoteAtual() {
		return loteAtual;
	}

	public List<RelatorioCobrancaItem> getItensConfirmados() {
		return itensConfirmados;
	}

	public List<RelatorioCobrancaItem> getItensPendentes() {
		return itensPendentes;
	}

	public List<GrupoConfirmadoCobranca> getGruposConfirmados() {
		return gruposConfirmados;
	}

	public List<GrupoConfirmadoCobranca> getGruposSelecionados() {
		return gruposSelecionados;
	}

	public void setGruposSelecionados(List<GrupoConfirmadoCobranca> gruposSelecionados) {
		this.gruposSelecionados = gruposSelecionados;
	}

	public boolean isEnvioCobrancaEmAndamento() {
		return envioCobrancaEmAndamento;
	}

	public int getTotalFilaEnvioCobranca() {
		return totalFilaEnvioCobranca;
	}

	public int getEnviadosFilaCobranca() {
		return enviadosFilaCobranca;
	}

	public boolean isEnvioCobrancaConcluido() {
		return envioCobrancaConcluido;
	}

	public int getSucessosFilaCobranca() {
		return sucessosFilaCobranca;
	}

	public List<String> getFalhasFilaCobranca() {
		return falhasFilaCobranca;
	}

	public String getFalhasFilaCobrancaTexto() {
		return String.join(", ", falhasFilaCobranca);
	}

	public RelatorioCobrancaItem getItemEmRevisao() {
		return itemEmRevisao;
	}

	public List<Cliente> getCandidatosDoItemEmRevisao() {
		return candidatosDoItemEmRevisao;
	}

	public Long getClienteEscolhidoManualmente() {
		return clienteEscolhidoManualmente;
	}

	public void setClienteEscolhidoManualmente(Long clienteEscolhidoManualmente) {
		this.clienteEscolhidoManualmente = clienteEscolhidoManualmente;
	}

	public String getBuscaClienteManual() {
		return buscaClienteManual;
	}

	public void setBuscaClienteManual(String buscaClienteManual) {
		this.buscaClienteManual = buscaClienteManual;
	}

	public List<Cliente> getResultadoBuscaClienteManual() {
		return resultadoBuscaClienteManual;
	}

}
