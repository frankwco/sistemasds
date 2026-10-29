package util;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import base.modelo.ColetaApp;
import base.service.ColetaAppService;

public class ListaColetaArmazenar {
	private List<ColetaApp> listaColetaApp;
	

	private static ListaColetaArmazenar listaColetaArmazenar;

	private ListaColetaArmazenar() {

	}
	
	public void adicionarLista(ColetaApp col) {
		this.listaColetaApp.add(col);
		
	}

	public static synchronized ListaColetaArmazenar getInstance() {
		if (listaColetaArmazenar == null) {
			listaColetaArmazenar = new ListaColetaArmazenar();
		}
		return listaColetaArmazenar;
	}

}
