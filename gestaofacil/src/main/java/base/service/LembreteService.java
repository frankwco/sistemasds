package base.service;

import java.io.Serializable;
import java.util.Date;

import javax.inject.Inject;

import base.modelo.LancamentoVeiculo;
import base.modelo.Lembrete;
import dao.GenericDAO;
import util.Transacional;

public class LembreteService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<Lembrete> dao;

	@Inject
	private LancamentoVeiculoService lancamentoVeiculoService;

	@Transacional
	public void inserirAlterar(Lembrete lembrete) {
		if (lembrete.getId() == null) {
			dao.inserir(lembrete);
		} else {
			dao.alterar(lembrete);
		}
	}

	/**
	 * Conclui o lembrete (RF33) e, se pedido, gera um Serviço em aberto no
	 * veículo do lembrete pra já registrar que a manutenção foi feita (o
	 * usuário completa valor/oficina depois, editando o lançamento gerado).
	 */
	public void concluir(Lembrete lembrete, boolean gerarLancamento, String usuarioLogado) {
		lembrete.setConcluido(true);
		lembrete.setDataConclusao(new Date());
		inserirAlterar(lembrete);

		if (gerarLancamento && lembrete.getVeiculo() != null) {
			LancamentoVeiculo lancamento = new LancamentoVeiculo();
			lancamento.setTipo("Serviço");
			lancamento.setVeiculo(lembrete.getVeiculo());
			lancamento.setMotorista(lembrete.getMotorista());
			lancamento.setData(new Date());
			lancamento.setObservacao("Gerado a partir do lembrete: " + lembrete.getDescricao());
			lancamentoVeiculoService.salvar(lancamento, usuarioLogado);
		}
	}

}
