package base.controle;

import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.ColetaApp;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.PontoColeta;
import base.service.CidadeService;
import base.service.ColetaAppService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import util.UsuarioLogado;
import dao.GenericDAO;

@ViewScoped
@Named("coletasPaginaClienteMB")
public class ColetasPaginaClienteMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private ColetaApp coletaApp;
	private List<ColetaApp> listaColetaApp;

	@Inject
	private GenericDAO<ColetaApp> daoColetaApp; // faz as buscas

	@Inject
	private ColetaAppService coletaAppService; // inserir no banco

	@PostConstruct
	public void inicializar() {

		coletaApp = new ColetaApp();

		listaColetaApp = new ArrayList<>();
		// listCidade = daoCidade.listaComStatus(ColetaApp.class);
		String emailLogado = UsuarioLogado.getEmailUsuarioLogado();
		listaColetaApp = new ArrayList<>();
		listaColetaApp = daoColetaApp.listar(ColetaApp.class, "(pontoColetaSelecionado.cliente.email='" + emailLogado
				+ "' or pontoColetaSelecionado.cliente.email2='" + emailLogado + "') order by dataUltimaColeta desc");
	}

	public void filtro(String tipo) {
		if (tipo.equals("novoEmail")) {
			listaColetaApp = new ArrayList<>();
			listaColetaApp = daoColetaApp.listar(ColetaApp.class, "LENGTH(emailNovo)>3 order by dataUltimaColeta desc");
		}
	}

	public ColetaApp getColetaApp() {
		return coletaApp;
	}

	public void setColetaApp(ColetaApp coletaApp) {
		this.coletaApp = coletaApp;
	}

	public List<ColetaApp> getListaColetaApp() {
		return listaColetaApp;
	}

	public void setListaColetaApp(List<ColetaApp> listaColetaApp) {
		this.listaColetaApp = listaColetaApp;
	}

}
