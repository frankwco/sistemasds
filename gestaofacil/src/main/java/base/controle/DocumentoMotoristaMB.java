package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.DocumentoMotorista;
import base.modelo.TipoDocumento;
import base.service.DocumentoMotoristaService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("documentoMotoristaMB")
public class DocumentoMotoristaMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private DocumentoMotorista documento;
	private List<DocumentoMotorista> listDocumento;

	@Inject
	private GenericDAO<DocumentoMotorista> dao;

	@Inject
	private GenericDAO<TipoDocumento> daoTipoDocumento;

	@Inject
	private DocumentoMotoristaService documentoMotoristaService;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		carregarLista();
	}

	public void preencherLista(DocumentoMotorista t) {
		this.documento = t;
	}

	public void inativar(DocumentoMotorista t) {
		t.setStatus(false);
		documentoMotoristaService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			documento.setStatus(true);
			documentoMotoristaService.inserirAlterar(documento);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogDocumentoMotorista();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<TipoDocumento> completarTipoDocumento(String str) {
		List<TipoDocumento> list = daoTipoDocumento.listar(TipoDocumento.class,
				"aplicavelA in ('Motorista','Ambos')");
		List<TipoDocumento> selecionados = new ArrayList<>();
		for (TipoDocumento cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		documento = new DocumentoMotorista();
	}

	public void carregarLista() {
		listDocumento = dao.listaComStatus(DocumentoMotorista.class, "dataVencimento");
	}

	public DocumentoMotorista getDocumento() {
		return documento;
	}

	public void setDocumento(DocumentoMotorista documento) {
		this.documento = documento;
	}

	public List<DocumentoMotorista> getListDocumento() {
		return listDocumento;
	}

	public void setListDocumento(List<DocumentoMotorista> listDocumento) {
		this.listDocumento = listDocumento;
	}

}
