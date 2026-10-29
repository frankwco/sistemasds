package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.TipoDocumento;
import base.service.TipoDocumentoService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("tipoDocumentoMB")
public class TipoDocumentoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	// nome, aplicavelA (Veiculo, Motorista ou Ambos)
	private static final String[][] TIPOS_PADRAO = { { "Licenciamento", "Veiculo" },
			{ "Seguro Obrigatório", "Veiculo" }, { "IPVA", "Veiculo" }, { "Vistoria", "Veiculo" },
			{ "Licença Ambiental", "Veiculo" }, { "NBR 12810", "Veiculo" }, { "CNH", "Motorista" },
			{ "NR-32", "Motorista" } };

	private TipoDocumento tipoDocumento;
	private List<TipoDocumento> listTipoDocumento;

	@Inject
	private GenericDAO<TipoDocumento> daoTipoDocumento; // faz as buscas

	@Inject
	private TipoDocumentoService tipoDocumentoService; // inserir no banco

	@PostConstruct
	public void inicializar() {
		tipoDocumento = new TipoDocumento();
		carregarPadrao();
		carregarLista();
	}

	private void carregarPadrao() {
		if (daoTipoDocumento.listaComStatus(TipoDocumento.class).isEmpty()) {
			for (String[] padrao : TIPOS_PADRAO) {
				TipoDocumento tipo = new TipoDocumento();
				tipo.setNome(padrao[0]);
				tipo.setAplicavelA(padrao[1]);
				tipo.setStatus(true);
				tipoDocumentoService.inserirAlterar(tipo);
			}
		}
	}

	public void preencherLista(TipoDocumento t) {
		this.tipoDocumento = t;
	}

	public void inativar(TipoDocumento t) {
		t.setStatus(false);
		tipoDocumentoService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			tipoDocumento.setStatus(true);
			tipoDocumentoService.inserirAlterar(tipoDocumento);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogTipoDocumento();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<TipoDocumento> completarTipoDocumento(String str) {
		List<TipoDocumento> list = daoTipoDocumento.listaComStatus(TipoDocumento.class);
		List<TipoDocumento> selecionados = new ArrayList<>();
		for (TipoDocumento cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		tipoDocumento = new TipoDocumento();
	}

	public void carregarLista() {
		listTipoDocumento = daoTipoDocumento.listaComStatus(TipoDocumento.class);
	}

	public TipoDocumento getTipoDocumento() {
		return tipoDocumento;
	}

	public void setTipoDocumento(TipoDocumento tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}

	public List<TipoDocumento> getListTipoDocumento() {
		return listTipoDocumento;
	}

	public void setListTipoDocumento(List<TipoDocumento> listTipoDocumento) {
		this.listTipoDocumento = listTipoDocumento;
	}

}
