package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import base.modelo.CategoriaIndicador;
import base.modelo.Coleta;
import base.modelo.ColetaApp;
import base.modelo.ColetaAppProximaColeta;
import base.modelo.ItensColeta;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.PontoColeta;
import base.modelo.Tipo;
import dao.GenericDAO;
import util.Transacional;

public class ColetaAppService implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<ColetaApp> dao;

	@Inject
	private GenericDAO<PontoColeta> daoPontoColeta;

	@Inject
	private ColetaAppAssinaturaService coletaAppAssinaturaService;

	@Inject
	private AvaliacaoNPSService avaliacaoNPSService;

	@Inject
	private ColetaService coletaService;

	@Inject
	private ItensColetaService itensColetaService;

	@Inject
	private GenericDAO<Coleta> daoColeta;

	@Inject
	private GenericDAO<ItensColeta> daoItensColeta;

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraPontoColeta> daoItensInformacaoFinanceiraPontoColeta;

	@Inject
	private GenericDAO<ItensInformacaoFinanceiraGrupo> daoItensInformacaoFinanceiraGrupo;

	@Transacional
	public void inserirAlterar(ColetaApp tipo) {
		if (tipo.getId() == null) {
			dao.inserir(tipo);
		} else {
			dao.alterar(tipo);
		}
	}

	@Transacional
	public void remover(ColetaApp tipo) {
		coletaAppAssinaturaService.removerAssinaturaPorColeta(tipo.getId());
		avaliacaoNPSService.desvincularColetaApp(tipo.getId());

		// Se essa ColetaApp já tinha gerado uma Coleta oficial (import
		// automático), desativa ela junto — nunca remove de verdade, pois já
		// pode estar referenciada em fechamento mensal/relatório financeiro.
		if (tipo.getColetaId() != null) {
			Coleta coleta = daoColeta.buscarPorId(Coleta.class, tipo.getColetaId());
			if (coleta != null && Boolean.TRUE.equals(coleta.getStatus())) {
				coleta.setStatus(false);
				coletaService.inserirAlterar(coleta);

				List<ItensColeta> itens = daoItensColeta.listar(ItensColeta.class, "coleta.id=" + coleta.getId());
				for (ItensColeta it : itens) {
					it.setStatus(false);
					itensColetaService.inserirAlterar(it);
				}
			}
		}

		dao.remover(ColetaApp.class, tipo.getId());
	}

	/**
	 * Gera a Coleta/ItensColeta oficiais a partir de uma ColetaApp — mesma
	 * lógica que antes só rodava manualmente (botão "Importar Dados"), agora
	 * chamada tanto ali (como rede de segurança) quanto automaticamente logo
	 * após o cadastroColeta inserir uma ColetaApp nova.
	 *
	 * Idempotente: se a ColetaApp já estiver importada, não faz nada — seguro
	 * chamar de mais de um lugar sem gerar Coleta duplicada.
	 */
	@Transacional
	public void importar(ColetaApp ca) {
		if (Boolean.TRUE.equals(ca.getImportado())) {
			return;
		}

		Coleta coleta = new Coleta();
		coleta.setPontoColeta(ca.getPontoColetaSelecionado());
		coleta.setDataColeta(ca.getDataUltimaColeta());
		coleta.setObservacao("Coleta realizada pelo APP");
		coleta.setStatus(true);
		coleta.setColetaAppId(ca.getId());

		List<ItensColeta> listItensColeta = carregarListaGrupos(coleta);
		for (ItensColeta li : listItensColeta) {
			li.setColeta(coleta);
			li.setStatus(true);
			preencherQuantidade(li, ca);
		}

		finalizarLancamentos(coleta, listItensColeta);

		ca.setImportado(true);
		ca.setColetaId(coleta.getId());
		inserirAlterar(ca);
	}

	/**
	 * Propaga pra Coleta/ItensColeta uma alteração feita na tela "Coleta
	 * APP" (diálogo "Alterar Coleta") depois que a ColetaApp já foi
	 * importada. Se ainda não foi importada (caiu no fallback manual), não
	 * há Coleta pra atualizar — inserirAlterar(ca) já basta.
	 */
	public void propagarAlteracao(ColetaApp ca) {
		if (ca.getColetaId() == null) {
			return;
		}

		Coleta coleta = daoColeta.buscarPorId(Coleta.class, ca.getColetaId());
		if (coleta == null) {
			return;
		}

		if (coleta.getFechamentoMes() != null && Boolean.TRUE.equals(coleta.getFechamentoMes().getFaturado())) {
			throw new IllegalStateException("Esta coleta já foi faturada e não pode mais ser alterada.");
		}

		List<ItensColeta> itens = daoItensColeta.listar(ItensColeta.class, "coleta.id=" + coleta.getId());
		for (ItensColeta it : itens) {
			preencherQuantidade(it, ca);
			itensColetaService.inserirAlterar(it);
		}
	}

	private void preencherQuantidade(ItensColeta li, ColetaApp ca) {
		String descricao = li.getGrupoResiduo().getDescricao().toLowerCase();
		if (descricao.equals("a")) {
			li.setQuantidadeColetada(ca.getA());
		}
		if (descricao.equals("e")) {
			li.setQuantidadeColetada(ca.getE());
		}
		if (descricao.equals("b")) {
			li.setQuantidadeColetada(ca.getB());
		}
		if (descricao.equals("a2")) {
			li.setQuantidadeColetada(ca.getA2());
		}
		if (descricao.equals("a3")) {
			li.setQuantidadeColetada(ca.getA3());
		}
		if (descricao.equals("a4")) {
			li.setQuantidadeColetada(ca.getA4());
		}
	}

	private List<ItensColeta> carregarListaGrupos(Coleta coleta) {
		List<ItensColeta> listItensColeta = new ArrayList<>();
		if (coleta != null && coleta.getPontoColeta() != null) {
			List<ItensInformacaoFinanceiraPontoColeta> listaPontos = daoItensInformacaoFinanceiraPontoColeta.listar(
					ItensInformacaoFinanceiraPontoColeta.class, "pontoColeta.id=" + coleta.getPontoColeta().getId()
							+ " and informacoesFinanceiras.situacaoContrato='Ativo' and informacoesFinanceiras.status is true and informacoesFinanceiras.situacaoContrato='Ativo'");
			if (listaPontos.size() > 0 && listaPontos.get(0).getInformacoesFinanceiras() != null
					&& listaPontos.get(0).getInformacoesFinanceiras().getSituacaoContrato().equals("Ativo")) {
				List<ItensInformacaoFinanceiraGrupo> listaGrupos = daoItensInformacaoFinanceiraGrupo.listar(
						ItensInformacaoFinanceiraGrupo.class,
						"informacoesFinanceiras.id=" + listaPontos.get(0).getInformacoesFinanceiras().getId()
								+ " order by grupoResiduos.ordem");
				for (ItensInformacaoFinanceiraGrupo g : listaGrupos) {
					ItensColeta i = new ItensColeta();
					i.setGrupoResiduo(g.getGrupoResiduos());
					i.setStatus(true);
					listItensColeta.add(i);
				}
			}
		}
		return listItensColeta;
	}

	private void finalizarLancamentos(Coleta coleta, List<ItensColeta> listItensColeta) {
		if (coleta.getPontoColeta() != null && coleta.getDataColeta() != null) {
			if (coleta.getId() == null) {
				coleta.setStatus(true);
				coletaService.inserirAlterar(coleta);
				for (ItensColeta it : listItensColeta) {
					it.setStatus(true);
					it.setColeta(coleta);
					itensColetaService.inserirAlterar(it);
				}
			} else {
				coletaService.inserirAlterar(coleta);
				for (ItensColeta it : listItensColeta) {
					itensColetaService.inserirAlterar(it);
				}
			}
		}
	}

	public ColetaAppProximaColeta proximaColeta(PontoColeta ponto) {
		ColetaAppProximaColeta proximaColeta = new ColetaAppProximaColeta();
		proximaColeta.setPontoColeta(ponto);
		if (ponto.getRota() != null) {

			String periodicidadeColeta = ponto.getPeriodicidadeColeta();
			Integer incrementoDias = 0;

			switch (periodicidadeColeta) {
			case "Semanal":
				incrementoDias = 7;
				break;
			case "Quinzenal":
				incrementoDias = 14;
				break;
			case "Mensal":
				incrementoDias = 28;
				break;
			case "Bimestral":
				incrementoDias = 56;
				break;
			case "Semestral":
				incrementoDias = 168;

			}
			this.dataProximaColeta(ponto, incrementoDias, proximaColeta);
			if(proximaColeta.getDataProximaColeta().before(new Date())) {
				this.dataProximaColeta(ponto, incrementoDias*2, proximaColeta);
			}
			if(proximaColeta.getDataProximaColeta().before(new Date())) {
				this.dataProximaColeta(ponto, incrementoDias*3, proximaColeta);
			}
	
	
				//proximaColeta.setDataProximaColeta(dataProximaColeta(ponto, incrementoDias, proximaColeta));
				
		}
		return proximaColeta;
	} 
	
	private void dataProximaColeta(PontoColeta ponto, int incrementoDias, ColetaAppProximaColeta proximaColeta) {
		Date dataProximaColeta = null;
		if (ponto.getDataUltimaColeta() != null) {
			dataProximaColeta = ponto.getDataUltimaColeta();
		}else {
			List<PontoColeta> lpc = daoPontoColeta.listar(PontoColeta.class, "rota.id="+ponto.getRota().getId()+" and dataUltimaColeta<>null");
			if(lpc.size()>0) {
				dataProximaColeta=  lpc.get(0).getDataUltimaColeta();
				proximaColeta.setMensagem("Ponto novo. A data informada é aproximada.");
			}
		}
			
			if(dataProximaColeta!=null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dataProximaColeta);
			cal.add(Calendar.DATE, incrementoDias);
			dataProximaColeta = cal.getTime();
			if(dataProximaColeta.getTime()<new Date().getTime()) {
				Calendar call = Calendar.getInstance();
				call.setTime(dataProximaColeta);
				//call.add(Calendar.DATE, incrementoDias);
				dataProximaColeta = call.getTime();
			}
			}else {
				proximaColeta.setMensagem("Ponto novo. Sem estimativa.");
			}
		
		
		proximaColeta.setDataProximaColeta(dataProximaColeta);
		//return dataProximaColeta;
	}
	
	public void pontoSemColeta() {
		
		
	}

}
