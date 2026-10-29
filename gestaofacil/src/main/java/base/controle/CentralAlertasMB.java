package base.controle;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.AlertaDTO;
import base.modelo.DocumentoMotorista;
import base.modelo.DocumentoVeiculo;
import base.modelo.LancamentoVeiculo;
import base.modelo.Lembrete;
import base.modelo.Veiculo;
import base.service.AlertaService;
import base.service.DocumentoMotoristaService;
import base.service.DocumentoVeiculoService;
import base.service.LancamentoVeiculoService;
import base.service.LembreteService;
import dao.GenericDAO;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;

/**
 * Central de alertas unificada (RF34) — agrega documentos a vencer,
 * manutenção programada e lembretes via {@link AlertaService}. A resolução
 * (renovar documento, concluir lembrete, agendar nova revisão) acontece
 * direto nesta tela, num dialog único cujo conteúdo muda conforme o tipo do
 * alerta selecionado — evita mandar o usuário pra outra tela só pra achar o
 * registro de novo.
 */
@ViewScoped
@Named("centralAlertasMB")
public class CentralAlertasMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<AlertaDTO> listAlerta;

	private AlertaDTO alertaSelecionado;

	// campos transientes do dialog de resolução
	private Date novaDataVencimento;
	private Date novaData;
	private Long novoKmProximaRevisao;
	private String novoTipoManutencao;

	@Inject
	private AlertaService alertaService;

	@Inject
	private GenericDAO<DocumentoVeiculo> daoDocumentoVeiculo;

	@Inject
	private GenericDAO<DocumentoMotorista> daoDocumentoMotorista;

	@Inject
	private GenericDAO<Lembrete> daoLembrete;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamento;

	@Inject
	private DocumentoVeiculoService documentoVeiculoService;

	@Inject
	private DocumentoMotoristaService documentoMotoristaService;

	@Inject
	private LembreteService lembreteService;

	@Inject
	private LancamentoVeiculoService lancamentoVeiculoService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	@PostConstruct
	public void inicializar() {
		carregarLista();
	}

	public void carregarLista() {
		listAlerta = alertaService.listarAlertas();
	}

	public void abrirResolver(AlertaDTO alerta) {
		this.alertaSelecionado = alerta;
		this.novaDataVencimento = null;
		this.novaData = new Date();
		this.novoKmProximaRevisao = null;
		this.novoTipoManutencao = null;
	}

	public void resolverDocumento() {
		try {
			if ("Documento Veículo".equals(alertaSelecionado.getTipo())) {
				DocumentoVeiculo documento = daoDocumentoVeiculo.buscarPorId(DocumentoVeiculo.class,
						alertaSelecionado.getDocumentoVeiculoId());
				documento.setDataVencimento(novaDataVencimento);
				documentoVeiculoService.inserirAlterar(documento);
			} else {
				DocumentoMotorista documento = daoDocumentoMotorista.buscarPorId(DocumentoMotorista.class,
						alertaSelecionado.getDocumentoMotoristaId());
				documento.setDataVencimento(novaDataVencimento);
				documentoMotoristaService.inserirAlterar(documento);
			}
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogResolverAlerta();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	/**
	 * Baixa "não aplicável" (ex.: veículo vendido, motorista desligado, taxa
	 * deixou de ser obrigatória): inativa o documento em si, igual à ação
	 * Excluir das telas de Documentos do Veículo/Motorista — some da central
	 * sem precisar renovar nem mexer no cadastro do veículo/motorista.
	 */
	public void ignorarDocumento() {
		try {
			if ("Documento Veículo".equals(alertaSelecionado.getTipo())) {
				DocumentoVeiculo documento = daoDocumentoVeiculo.buscarPorId(DocumentoVeiculo.class,
						alertaSelecionado.getDocumentoVeiculoId());
				documento.setStatus(false);
				documentoVeiculoService.inserirAlterar(documento);
			} else {
				DocumentoMotorista documento = daoDocumentoMotorista.buscarPorId(DocumentoMotorista.class,
						alertaSelecionado.getDocumentoMotoristaId());
				documento.setStatus(false);
				documentoMotoristaService.inserirAlterar(documento);
			}
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogResolverAlerta();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	/**
	 * Baixa "não aplicável" da manutenção programada: limpa o km da próxima
	 * revisão do último serviço — o histórico do serviço continua no extrato,
	 * só some o alvo que estava gerando o alerta.
	 */
	public void ignorarManutencao() {
		try {
			LancamentoVeiculo lancamento = daoLancamento.buscarPorId(LancamentoVeiculo.class,
					alertaSelecionado.getLancamentoVeiculoId());
			lancamento.setKmProximaRevisao(null);
			lancamentoVeiculoService.inserirAlterar(lancamento);
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogResolverAlerta();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void concluirLembrete() {
		concluirLembrete(false);
	}

	public void concluirLembreteEGerarServico() {
		concluirLembrete(true);
	}

	private void concluirLembrete(boolean gerarLancamento) {
		try {
			Lembrete lembrete = daoLembrete.buscarPorId(Lembrete.class, alertaSelecionado.getLembreteId());
			lembreteService.concluir(lembrete, gerarLancamento, usuarioSessaoMB.getUsuario().getNome());
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogResolverAlerta();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void resolverManutencao() {
		try {
			Veiculo veiculo = daoVeiculo.buscarPorId(Veiculo.class, alertaSelecionado.getVeiculoId());
			LancamentoVeiculo lancamento = new LancamentoVeiculo();
			lancamento.setTipo("Serviço");
			lancamento.setVeiculo(veiculo);
			lancamento.setData(novaData);
			lancamento.setTipoManutencao(novoTipoManutencao);
			lancamento.setKmProximaRevisao(novoKmProximaRevisao);
			lancamentoVeiculoService.salvar(lancamento, usuarioSessaoMB.getUsuario().getNome());
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogResolverAlerta();
			carregarLista();
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<AlertaDTO> getListAlerta() {
		return listAlerta;
	}

	public void setListAlerta(List<AlertaDTO> listAlerta) {
		this.listAlerta = listAlerta;
	}

	public AlertaDTO getAlertaSelecionado() {
		return alertaSelecionado;
	}

	public void setAlertaSelecionado(AlertaDTO alertaSelecionado) {
		this.alertaSelecionado = alertaSelecionado;
	}

	public Date getNovaDataVencimento() {
		return novaDataVencimento;
	}

	public void setNovaDataVencimento(Date novaDataVencimento) {
		this.novaDataVencimento = novaDataVencimento;
	}

	public Date getNovaData() {
		return novaData;
	}

	public void setNovaData(Date novaData) {
		this.novaData = novaData;
	}

	public Long getNovoKmProximaRevisao() {
		return novoKmProximaRevisao;
	}

	public void setNovoKmProximaRevisao(Long novoKmProximaRevisao) {
		this.novoKmProximaRevisao = novoKmProximaRevisao;
	}

	public String getNovoTipoManutencao() {
		return novoTipoManutencao;
	}

	public void setNovoTipoManutencao(String novoTipoManutencao) {
		this.novoTipoManutencao = novoTipoManutencao;
	}

}
