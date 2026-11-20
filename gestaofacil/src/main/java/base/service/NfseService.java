package base.service;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import base.modelo.NfseConfiguracao;
import base.modelo.NotaFiscalServico;
import dao.GenericDAO;
import util.Transacional;

/**
 * Persistência da configuração e das notas de NFS-e. Cada método é uma
 * transação curta, para que a chamada HTTP à Sefin Nacional (que pode
 * demorar) fique fora de transação de banco.
 */
public class NfseService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private GenericDAO<NfseConfiguracao> daoConfiguracao;

	@Inject
	private GenericDAO<NotaFiscalServico> daoNota;

	/** Devolve a configuração (registro único), criando uma vazia se não existir. */
	@Transacional
	public NfseConfiguracao carregarConfiguracao() {
		List<NfseConfiguracao> lista = daoConfiguracao.listaComStatus(NfseConfiguracao.class);
		if (!lista.isEmpty()) {
			return lista.get(0);
		}
		NfseConfiguracao cfg = new NfseConfiguracao();
		daoConfiguracao.inserir(cfg);
		return cfg;
	}

	@Transacional
	public void salvarConfiguracao(NfseConfiguracao cfg) {
		if (cfg.getId() == null) {
			daoConfiguracao.inserir(cfg);
		} else {
			daoConfiguracao.alterar(cfg);
		}
	}

	/**
	 * Reserva o próximo número de DPS (incrementa e grava antes do envio, para
	 * nunca repetir um número já enviado).
	 */
	@Transacional
	public long reservarNumeroDps() {
		NfseConfiguracao cfg = carregarConfiguracao();
		long proximo = (cfg.getUltimoNumeroDps() == null ? 0L : cfg.getUltimoNumeroDps()) + 1;
		cfg.setUltimoNumeroDps(proximo);
		daoConfiguracao.alterar(cfg);
		return proximo;
	}

	@Transacional
	public NotaFiscalServico salvarNota(NotaFiscalServico nota) {
		if (nota.getId() == null) {
			daoNota.inserir(nota);
			return nota;
		}
		daoNota.alterar(nota);
		return nota;
	}

	public NotaFiscalServico buscarNota(Long id) {
		return daoNota.buscarPorId(NotaFiscalServico.class, id);
	}

	public List<NotaFiscalServico> listarNotas(String condicao) {
		return daoNota.listar(NotaFiscalServico.class, condicao + " order by dataEmissao desc, id desc");
	}
}
