package base.service;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import base.modelo.NfseConfiguracao;
import base.modelo.NotaFiscalServico;
import dao.GenericDAO;
import util.Transacional;
import util.nfse.CriptografiaSenhaCertificado;
import util.nfse.NfseNacionalClient.NfseException;

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
			NfseConfiguracao cfg = lista.get(0);
			migrarSenhaTextoPuro(cfg);
			return cfg;
		}
		NfseConfiguracao cfg = new NfseConfiguracao();
		daoConfiguracao.inserir(cfg);
		return cfg;
	}

	/** Senha gravada antes da criptografia: criptografa e grava de novo. */
	private void migrarSenhaTextoPuro(NfseConfiguracao cfg) {
		String senha = cfg.getSenhaCertificado();
		if (senha == null || senha.isEmpty() || CriptografiaSenhaCertificado.estaCriptografada(senha)) {
			return;
		}
		try {
			cfg.setSenhaCertificado(CriptografiaSenhaCertificado.criptografar(senha));
			daoConfiguracao.alterar(cfg);
		} catch (NfseException e) {
			// mantém como está; a emissão continua funcionando com a senha antiga
			System.err.println("[NFS-e] Senha do certificado não foi criptografada: " + e.getMessage());
		}
	}

	/** Grava a configuração; a senha do certificado vai criptografada (AES). */
	@Transacional
	public void salvarConfiguracao(NfseConfiguracao cfg) throws NfseException {
		cfg.setSenhaCertificado(CriptografiaSenhaCertificado.criptografar(cfg.getSenhaCertificado()));
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
