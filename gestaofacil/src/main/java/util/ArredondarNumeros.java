package util;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.GrupoLancamento;
import base.modelo.GrupoResiduo;
import base.modelo.ItensColeta;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.Usuario;
import base.modelo.Coleta;
import base.modelo.ContasReceberParcelas;
import base.service.ItensColetaService;
import base.service.ColetaService;

import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

public class ArredondarNumeros {

	public static Double tresCasasDecimais(Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		return Double.parseDouble(new DecimalFormat("###.000").format(valor).replace(",", "."));
	}

	public static Double duasCasasDecimais(Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		return Double.parseDouble(new DecimalFormat("###.00").format(valor).replace(",", "."));
	}

	public static Double umaCasaDecimal(Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		return Double.parseDouble(new DecimalFormat("###.0").format(valor).replace(",", "."));
	}

}
