package base.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import base.modelo.CategoriaDespesa;
import base.modelo.LancamentoVeiculo;
import dao.GenericDAO;
import util.Transacional;

public class LancamentoVeiculoService implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String CATEGORIA_COMBUSTIVEL = "Combustível";

	@Inject
	private GenericDAO<LancamentoVeiculo> dao;

	@Inject
	private GenericDAO<CategoriaDespesa> daoCategoriaDespesa;

	@Inject
	private LeituraOdometroService leituraOdometroService;

	@Transacional
	public void inserirAlterar(LancamentoVeiculo lancamento) {
		if (lancamento.getId() == null) {
			dao.inserir(lancamento);
		} else {
			dao.alterar(lancamento);
		}
	}

	/**
	 * Ponto único de gravação usado pela tela administrativa e pelo endpoint
	 * REST do app: cuida da auditoria (RF10), do preenchimento automático da
	 * categoria "Combustível" em abastecimentos e da geração da leitura de
	 * odômetro (RF29) quando o lançamento vem com km.
	 *
	 * Precisa do próprio @Transacional aqui: como o método chama
	 * {@link #inserirAlterar(LancamentoVeiculo)} internamente (via "this"),
	 * essa chamada não passa pelo proxy do CDI e o interceptor de
	 * {@link Transacional} não entraria em ação — sem isso, o insert nunca é
	 * commitado (a tela mostra sucesso mas nada é gravado).
	 */
	@Transacional
	public void salvar(LancamentoVeiculo lancamento, String usuarioLogado) {
		Date agora = new Date();
		// só gera leitura de odômetro na criação — reeditar um lançamento (ex.:
		// corrigir a observação) não deve empilhar uma nova leitura a cada save
		boolean novo = lancamento.getId() == null;

		if (novo) {
			lancamento.setCriadoPor(usuarioLogado);
			lancamento.setCriadoEm(agora);
		} else {
			lancamento.setAlteradoPor(usuarioLogado);
			lancamento.setAlteradoEm(agora);
		}
		if (lancamento.getStatus() == null) {
			lancamento.setStatus(true);
		}

		if ("Abastecimento".equals(lancamento.getTipo()) && lancamento.getCategoriaDespesa() == null) {
			List<CategoriaDespesa> combustivel = daoCategoriaDespesa.listarPorCampo(CategoriaDespesa.class, "nome",
					CATEGORIA_COMBUSTIVEL);
			if (!combustivel.isEmpty()) {
				lancamento.setCategoriaDespesa(combustivel.get(0));
			}
		}

		inserirAlterar(lancamento);

		if (novo && lancamento.getKm() != null && lancamento.getVeiculo() != null) {
			String origem = "Abastecimento".equals(lancamento.getTipo()) ? "Abastecimento"
					: "Serviço".equals(lancamento.getTipo()) ? "Serviço" : null;
			if (origem != null) {
				leituraOdometroService.registrar(lancamento.getVeiculo(), lancamento.getKm(), lancamento.getData(),
						origem, lancamento, usuarioLogado);
			}
		}
	}

}
