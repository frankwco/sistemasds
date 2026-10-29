package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.Fornecedor;
import base.service.FornecedorService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("fornecedorMB")
public class FornecedorMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Fornecedor fornecedor;
	private List<Fornecedor> listFornecedor;

	@Inject
	private GenericDAO<Fornecedor> daoFornecedor; // faz as buscas

	@Inject
	private FornecedorService fornecedorService; // inserir no banco

	@PostConstruct
	public void inicializar() {
		fornecedor = new Fornecedor();
		carregarLista();
	}

	public void preencherLista(Fornecedor t) {
		this.fornecedor = t;
	}

	public void inativar(Fornecedor t) {
		t.setStatus(false);
		fornecedorService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			fornecedor.setStatus(true);
			fornecedorService.inserirAlterar(fornecedor);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogFornecedor();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<Fornecedor> completarFornecedor(String str) {
		List<Fornecedor> list = daoFornecedor.listaComStatus(Fornecedor.class);
		List<Fornecedor> selecionados = new ArrayList<>();
		for (Fornecedor cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		fornecedor = new Fornecedor();
	}

	public void carregarLista() {
		listFornecedor = daoFornecedor.listaComStatus(Fornecedor.class);
	}

	public Fornecedor getFornecedor() {
		return fornecedor;
	}

	public void setFornecedor(Fornecedor fornecedor) {
		this.fornecedor = fornecedor;
	}

	public List<Fornecedor> getListFornecedor() {
		return listFornecedor;
	}

	public void setListFornecedor(List<Fornecedor> listFornecedor) {
		this.listFornecedor = listFornecedor;
	}

}
