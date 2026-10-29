package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.ItemSelectEvent;
import org.primefaces.model.chart.MeterGaugeChartModel;

import base.modelo.Pessoa;
import base.modelo.Atividade;
import base.service.AtividadeService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

@ViewScoped
@Named("loginMB")
public class LoginMB implements Serializable {

	private static final long serialVersionUID = 1102175500213815790L;
	
	private Boolean cpf = true;

	public Boolean getCpf() {
		return cpf;
	}

	public void setCpf(Boolean cpf) {
		this.cpf = cpf;
	}

	
	
	
	
	
	

}
