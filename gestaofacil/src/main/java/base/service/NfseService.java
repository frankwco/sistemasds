package base.service;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import base.modelo.Cidade;
import base.modelo.ContasReceberParcelas;
import base.modelo.FechamentoMesItensPontoColeta;
import base.modelo.NfseAliquotaMensal;
import base.modelo.NfseConfiguracao;
import base.modelo.NfseModeloDescricao;
import base.modelo.NotaFiscalServico;
import base.modelo.PontoColeta;
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

	@Inject
	private GenericDAO<NfseAliquotaMensal> daoAliquota;

	@Inject
	private GenericDAO<FechamentoMesItensPontoColeta> daoItensFechamento;

	@Inject
	private GenericDAO<PontoColeta> daoPonto;

	/**
	 * Cidades dos pontos de coleta: os do fechamento, quando informado; senão,
	 * todos os pontos ativos do cliente.
	 */
	public List<Cidade> listarCidadesPontos(Long clienteId, Long fechamentoId) {
		java.util.Map<Long, Cidade> cidades = new java.util.LinkedHashMap<>();
		if (fechamentoId != null) {
			for (FechamentoMesItensPontoColeta i : daoItensFechamento.listar(FechamentoMesItensPontoColeta.class,
					"fechamentoMes.id = " + fechamentoId)) {
				if (i.getItensPontoColeta() != null) {
					incluirCidade(cidades, i.getItensPontoColeta().getPontoColeta());
				}
			}
		}
		if (cidades.isEmpty() && clienteId != null) {
			for (PontoColeta p : daoPonto.listar(PontoColeta.class, "cliente.id = " + clienteId)) {
				incluirCidade(cidades, p);
			}
		}
		return new java.util.ArrayList<>(cidades.values());
	}

	private static void incluirCidade(java.util.Map<Long, Cidade> cidades, PontoColeta p) {
		if (p != null && p.getCidade() != null) {
			cidades.put(p.getCidade().getId(), p.getCidade());
		}
	}

	@Inject
	private GenericDAO<NfseModeloDescricao> daoModelo;

	/** Modelos de descrição; na primeira vez cria os iniciais, tirados de notas reais. */
	@Transacional
	public List<NfseModeloDescricao> listarModelosDescricao() {
		NfseConfiguracao cfg = carregarConfiguracao();
		if (!Boolean.TRUE.equals(cfg.getModelosDescricaoCriados())) {
			String base = "COLETA, TRANSPORTE, TRATAMENTO e DESTIN. FINAL DE RESIDUOS DA SAUDE";
			daoModelo.inserir(new NfseModeloDescricao("Mensal",
					base + " Mês {MES_NOME}/{ANO} Conforme contrato e demonstrativo anexo. BOLETO: {BOLETO}", 1));
			daoModelo.inserir(new NfseModeloDescricao("Coleta única",
					base + " - {MES_NOME}/{ANO} (COLETA UNICA) boleto: {BOLETO}", 2));
			daoModelo.inserir(new NfseModeloDescricao("Prefeitura (contrato)", base
					+ " até Mês {MES_NOME}/{ANO} Conforme contrato e demonstrativo anexo.- solic. fornecimento {PREENCHER}"
					+ " CONTRATO {PREENCHER} (PROC. ADMIN. {PREENCHER}) {PREENCHER} kg x R$ {PREENCHER}/kg ..."
					+ " DEPÓSITO {PREENCHER}", 3));
			cfg.setModelosDescricaoCriados(true);
			daoConfiguracao.alterar(cfg);
		}
		return daoModelo.listaComStatus(NfseModeloDescricao.class, "ordem, nome");
	}

	@Transacional
	public void salvarModeloDescricao(NfseModeloDescricao m) {
		if (m.getId() == null) {
			daoModelo.inserir(m);
		} else {
			daoModelo.alterar(m);
		}
	}

	@Inject
	private GenericDAO<ContasReceberParcelas> daoParcela;

	/**
	 * Vencimento da parcela do fechamento. Só devolve quando há uma única data;
	 * com nenhuma ou várias, o usuário informa.
	 */
	public java.util.Date buscarVencimento(Long fechamentoId) {
		List<ContasReceberParcelas> parcelas = daoParcela.listar(ContasReceberParcelas.class,
				"fechamentoMes.id = " + fechamentoId);
		if (parcelas.isEmpty()) {
			parcelas = daoParcela.listar(ContasReceberParcelas.class, "contasReceber.fechamentoMes.id = " + fechamentoId);
		}
		java.util.Date unica = null;
		for (ContasReceberParcelas p : parcelas) {
			if (p.getDataVencimento() == null || (unica != null && unica.getTime() != p.getDataVencimento().getTime())) {
				return null;
			}
			unica = p.getDataVencimento();
		}
		return unica;
	}

	/** Alíquotas do mês de competência, ou null se ainda não foram cadastradas. */
	public NfseAliquotaMensal buscarAliquota(int mes, int ano) {
		List<NfseAliquotaMensal> l = daoAliquota.listar(NfseAliquotaMensal.class, "mes = " + mes + " and ano = " + ano);
		return l.isEmpty() ? null : l.get(0);
	}

	public List<NfseAliquotaMensal> listarAliquotas() {
		return daoAliquota.listaComStatus(NfseAliquotaMensal.class, "ano desc, mes desc");
	}

	/** Grava as alíquotas do mês (altera se o mês já existir). */
	@Transacional
	public void salvarAliquota(int mes, int ano, Double percentualSimples) {
		NfseAliquotaMensal a = buscarAliquota(mes, ano);
		boolean nova = a == null;
		if (nova) {
			a = new NfseAliquotaMensal();
			a.setMes(mes);
			a.setAno(ano);
		}
		a.setPercentualSimples(percentualSimples);
		if (nova) {
			daoAliquota.inserir(a);
		} else {
			daoAliquota.alterar(a);
		}
	}

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
