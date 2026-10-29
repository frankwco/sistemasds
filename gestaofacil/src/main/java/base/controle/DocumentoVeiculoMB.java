package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.DocumentoVeiculo;
import base.modelo.TipoDocumento;
import base.modelo.Veiculo;
import base.service.DocumentoVeiculoService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("documentoVeiculoMB")
public class DocumentoVeiculoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private DocumentoVeiculo documento;
	private List<DocumentoVeiculo> listDocumento;

	@Inject
	private GenericDAO<DocumentoVeiculo> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private GenericDAO<TipoDocumento> daoTipoDocumento;

	@Inject
	private DocumentoVeiculoService documentoVeiculoService;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		carregarLista();
	}

	public void preencherLista(DocumentoVeiculo t) {
		this.documento = t;
	}

	public void inativar(DocumentoVeiculo t) {
		t.setStatus(false);
		documentoVeiculoService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			documento.setStatus(true);
			documentoVeiculoService.inserirAlterar(documento);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogDocumentoVeiculo();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<Veiculo> completarVeiculo(String str) {
		List<Veiculo> list = daoVeiculo.listaComStatus(Veiculo.class);
		List<Veiculo> selecionados = new ArrayList<>();
		for (Veiculo cur : list) {
			if (cur.getDescricao().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public List<TipoDocumento> completarTipoDocumento(String str) {
		List<TipoDocumento> list = daoTipoDocumento.listar(TipoDocumento.class,
				"aplicavelA in ('Veiculo','Ambos')");
		List<TipoDocumento> selecionados = new ArrayList<>();
		for (TipoDocumento cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		documento = new DocumentoVeiculo();
	}

	public void carregarLista() {
		listDocumento = dao.listaComStatus(DocumentoVeiculo.class, "dataVencimento");
	}

	public DocumentoVeiculo getDocumento() {
		return documento;
	}

	public void setDocumento(DocumentoVeiculo documento) {
		this.documento = documento;
	}

	public List<DocumentoVeiculo> getListDocumento() {
		return listDocumento;
	}

	public void setListDocumento(List<DocumentoVeiculo> listDocumento) {
		this.listDocumento = listDocumento;
	}

}
