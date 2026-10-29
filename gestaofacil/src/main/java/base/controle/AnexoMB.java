package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import base.modelo.Anexo;
import base.modelo.LancamentoVeiculo;
import base.service.AnexoService;
import util.ExibirMensagem;
import util.FirebaseStorageUtil;
import util.Mensagem;
import dao.GenericDAO;

/**
 * Comprovantes/notas fiscais anexados a um lançamento (RF09). O binário vai
 * pro Firebase Storage ({@link FirebaseStorageUtil}); aqui só ficam os
 * metadados. Compartilhado pelos dialogs de Abastecimento e
 * Despesa/Manutenção — cada um chama {@link #prepararParaLancamento(Long)}
 * ao abrir o dialog de anexos de uma linha.
 */
@ViewScoped
@Named("anexoMB")
public class AnexoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long lancamentoId;
	private List<Anexo> listAnexo = new ArrayList<>();

	@Inject
	private GenericDAO<Anexo> daoAnexo;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamento;

	@Inject
	private AnexoService anexoService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	public void prepararParaLancamento(LancamentoVeiculo lancamento) {
		this.lancamentoId = lancamento == null ? null : lancamento.getId();
		carregarLista();
	}

	public void carregarLista() {
		if (lancamentoId == null) {
			listAnexo = new ArrayList<>();
			return;
		}
		listAnexo = daoAnexo.listar(Anexo.class, "lancamento.id=" + lancamentoId);
	}

	public void upload(FileUploadEvent evento) {
		try {
			UploadedFile arquivo = evento.getFile();
			if (arquivo == null || arquivo.getFileName() == null || arquivo.getFileName().trim().isEmpty()
					|| lancamentoId == null) {
				return;
			}

			LancamentoVeiculo lancamento = daoLancamento.buscarPorId(LancamentoVeiculo.class, lancamentoId);
			String caminho = FirebaseStorageUtil.upload(arquivo.getContents(), arquivo.getFileName(),
					arquivo.getContentType(), lancamentoId);

			Anexo anexo = new Anexo();
			anexo.setLancamento(lancamento);
			anexo.setNomeArquivo(arquivo.getFileName());
			anexo.setCaminho(caminho);
			anexo.setContentType(arquivo.getContentType());
			anexo.setTamanho((long) arquivo.getContents().length);
			anexo.setCriadoPor(usuarioSessaoMB.getUsuario().getNome());
			anexo.setCriadoEm(new Date());
			anexo.setStatus(true);
			anexoService.inserirAlterar(anexo);

			carregarLista();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void excluir(Anexo anexo) {
		try {
			FirebaseStorageUtil.excluir(anexo.getCaminho());
			anexo.setStatus(false);
			anexoService.inserirAlterar(anexo);
			carregarLista();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public String urlDownload(Anexo anexo) {
		try {
			return FirebaseStorageUtil.gerarUrlAssinada(anexo.getCaminho(), 60);
		} catch (Exception e) {
			e.printStackTrace();
			return "#";
		}
	}

	public Long getLancamentoId() {
		return lancamentoId;
	}

	public List<Anexo> getListAnexo() {
		return listAnexo;
	}

}
