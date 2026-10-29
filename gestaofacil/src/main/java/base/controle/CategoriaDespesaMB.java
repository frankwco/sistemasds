package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.CategoriaDespesa;
import base.service.CategoriaDespesaService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("categoriaDespesaMB")
public class CategoriaDespesaMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final List<String> CATEGORIAS_PADRAO = Arrays.asList("Combustível", "Manutenção Preventiva",
			"Manutenção Corretiva", "Seguro", "Multa", "Pedágio", "IPVA", "Lavagem", "Insumos de Biossegurança",
			"Outros");

	private CategoriaDespesa categoriaDespesa;
	private List<CategoriaDespesa> listCategoriaDespesa;

	@Inject
	private GenericDAO<CategoriaDespesa> daoCategoriaDespesa; // faz as buscas

	@Inject
	private CategoriaDespesaService categoriaDespesaService; // inserir no banco

	@PostConstruct
	public void inicializar() {
		categoriaDespesa = new CategoriaDespesa();
		carregarPadrao();
		carregarLista();
	}

	private void carregarPadrao() {
		if (daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class).isEmpty()) {
			for (String nome : CATEGORIAS_PADRAO) {
				CategoriaDespesa cat = new CategoriaDespesa();
				cat.setNome(nome);
				cat.setStatus(true);
				categoriaDespesaService.inserirAlterar(cat);
			}
		}
	}

	public void preencherLista(CategoriaDespesa t) {
		this.categoriaDespesa = t;
	}

	public void inativar(CategoriaDespesa t) {
		t.setStatus(false);
		categoriaDespesaService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			categoriaDespesa.setStatus(true);
			categoriaDespesaService.inserirAlterar(categoriaDespesa);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogCategoriaDespesa();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<CategoriaDespesa> completarCategoriaDespesa(String str) {
		List<CategoriaDespesa> list = daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class);
		List<CategoriaDespesa> selecionados = new ArrayList<>();
		for (CategoriaDespesa cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		categoriaDespesa = new CategoriaDespesa();
	}

	public void carregarLista() {
		listCategoriaDespesa = daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class);
	}

	public CategoriaDespesa getCategoriaDespesa() {
		return categoriaDespesa;
	}

	public void setCategoriaDespesa(CategoriaDespesa categoriaDespesa) {
		this.categoriaDespesa = categoriaDespesa;
	}

	public List<CategoriaDespesa> getListCategoriaDespesa() {
		return listCategoriaDespesa;
	}

	public void setListCategoriaDespesa(List<CategoriaDespesa> listCategoriaDespesa) {
		this.listCategoriaDespesa = listCategoriaDespesa;
	}

}
