package base.service;

import java.io.Serializable;
import java.util.Date;

import javax.inject.Inject;

import base.modelo.LancamentoVeiculo;
import base.modelo.LeituraOdometro;
import base.modelo.Veiculo;
import dao.GenericDAO;
import util.Transacional;

public class LeituraOdometroService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<LeituraOdometro> dao;

	@Inject
	private VeiculoService veiculoService;

	@Transacional
	public void inserirAlterar(LeituraOdometro leitura) {
		if (leitura.getId() == null) {
			dao.inserir(leitura);
		} else {
			dao.alterar(leitura);
		}
	}

	/**
	 * Registra uma leitura de odômetro e atualiza o km atual do veículo quando a
	 * leitura é mais recente que a que ele já tinha — usado tanto pelo cadastro
	 * avulso (RF29) quanto pelos lançamentos de Abastecimento/Serviço que vêm
	 * com km preenchido.
	 *
	 * Bloqueia (sem gravar nada) se o km vier menor que o último registrado
	 * para o veículo, pra evitar odômetro andando pra trás por erro de
	 * digitação.
	 *
	 * Precisa do próprio @Transacional: chamado direto pela tela de Odômetro
	 * (RF29), e como chama {@link #inserirAlterar(LeituraOdometro)} via "this",
	 * essa chamada interna não passa pelo proxy do CDI — sem a anotação aqui, o
	 * interceptor nunca entra em ação e o insert não é commitado.
	 */
	@Transacional
	public void registrar(Veiculo veiculo, Long km, Date data, String origem, LancamentoVeiculo lancamento,
			String usuarioLogado) {
		if (veiculo.getKmAtual() != null && km < veiculo.getKmAtual()) {
			throw new IllegalStateException("O km informado (" + km
					+ ") é menor que o último odômetro registrado para este veículo (" + veiculo.getKmAtual() + ").");
		}

		LeituraOdometro leitura = new LeituraOdometro();
		leitura.setVeiculo(veiculo);
		leitura.setKm(km);
		leitura.setData(data);
		leitura.setOrigem(origem);
		leitura.setLancamento(lancamento);
		leitura.setCriadoPor(usuarioLogado);
		leitura.setCriadoEm(new Date());
		leitura.setStatus(true);
		inserirAlterar(leitura);

		if (veiculo.getKmAtual() == null || km > veiculo.getKmAtual()) {
			veiculo.setKmAtual(km);
			veiculoService.inserirAlterar(veiculo);
		}
	}

}
