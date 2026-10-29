package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.Pessoa;
import base.modelo.Atividade;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppAssinatura;
import base.service.AtividadeService;
import base.service.ColetaAppAssinaturaService;
import base.service.ColetaAppService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("updateMB")
public class UpdateMB implements Serializable {

	private static final long serialVersionUID = 1L;


	
	@Inject
	private GenericDAO<ColetaApp> daoColetaApp; // faz as buscas
	
	@Inject
	private GenericDAO<ColetaAppAssinatura> daoColetaAppAssinatura; // faz as buscas

	@Inject
	private ColetaAppService coletaAppService; // inserir no banco
	
	@Inject
	private ColetaAppAssinaturaService coletaAppAssinaturaService; // inserir no banco

	@PostConstruct
	public void inicializar() {


	}
	

	
public void atualizarAssinaturaColeta() {
//	List<ColetaAppAssinatura> lco=daoColetaAppAssinatura.listaComStatus(ColetaAppAssinatura.class);
//	
//	for(ColetaAppAssinatura co:lco) {
//		String decodedString = new String(Base64.getDecoder().decode(co.getAssinaturaByte()));
//		System.out.println(decodedString);
//		//co.setAssinaturaByte();
//		coletaAppAssinaturaService.inserirAlterar(co);
//	}
	
	ExibirMensagem.exibirMensagem("FINALIZADO!!");
}
	
}
