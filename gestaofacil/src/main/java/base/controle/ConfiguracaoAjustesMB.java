package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.ConfiguracaoAjustes;
import base.service.ConfiguracaoAjusteService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("configuracaoAjustesMB")
public class ConfiguracaoAjustesMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private ConfiguracaoAjustes configuracaoAjustes;
	private List<ConfiguracaoAjustes> listaConfiguracaoAjustes;

	@Inject
	private GenericDAO<ConfiguracaoAjustes> daoEstado; // faz as buscas

	@Inject
	private ConfiguracaoAjusteService configuracaoAjustesService; // inserir no banco

	@PostConstruct
	public void inicializar() {

		configuracaoAjustes = new ConfiguracaoAjustes();

		listaConfiguracaoAjustes = new ArrayList<>();
		listaConfiguracaoAjustes = daoEstado.listaComStatus(ConfiguracaoAjustes.class);

	}

	public void preencherListaEstado(ConfiguracaoAjustes t) {
		this.configuracaoAjustes = t;

	}
	


	public void inativar(ConfiguracaoAjustes t) {
		t.setStatus(false);
		configuracaoAjustesService.inserirAlterar(t);
		// configuracaoAjustesService.update(" ConfiguracaoAjustes set status = false where id = " +
		// t.getId());
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {

		try {
			if (configuracaoAjustes.getId() == null) {
				configuracaoAjustes.setStatus(true);
				configuracaoAjustesService.inserirAlterar(configuracaoAjustes);

			} else {
				configuracaoAjustes.setStatus(true);
				configuracaoAjustesService.inserirAlterar(configuracaoAjustes);
			}

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogConfiguracaoAjustes();;
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}

	}

	public void criarNovoObjeto() {
		configuracaoAjustes = new ConfiguracaoAjustes();
	}

	public void carregarLista() {
		listaConfiguracaoAjustes = daoEstado.listaComStatus(ConfiguracaoAjustes.class);
	}

	public ConfiguracaoAjustes getConfiguracaoAjustes() {
		return configuracaoAjustes;
	}

	public void setConfiguracaoAjustes(ConfiguracaoAjustes configuracaoAjustes) {
		this.configuracaoAjustes = configuracaoAjustes;
	}

	public List<ConfiguracaoAjustes> getListaConfiguracaoAjustes() {
		return listaConfiguracaoAjustes;
	}

	public void setListaConfiguracaoAjustes(List<ConfiguracaoAjustes> listaConfiguracaoAjustes) {
		this.listaConfiguracaoAjustes = listaConfiguracaoAjustes;
	}

	
	
}
