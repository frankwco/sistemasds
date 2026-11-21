package base.service;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import base.modelo.Cliente;
import base.modelo.NfseConfiguracao;
import base.modelo.NotaFiscalServico;
import util.nfse.CriptografiaSenhaCertificado;
import util.nfse.NfseNacionalClient;
import util.nfse.NfseNacionalClient.DadosDps;
import util.nfse.NfseNacionalClient.NfseException;
import util.nfse.NfseNacionalClient.NfseFalhaComunicacaoException;
import util.nfse.NfseNacionalClient.RetornoEvento;
import util.nfse.NfseNacionalClient.RetornoNfse;

/**
 * Orquestra a emissão da NFS-e: reserva o número da DPS, monta e assina o XML,
 * grava a tentativa e envia para a Sefin Nacional, atualizando a situação
 * conforme o retorno.
 */
public class NfseEmissaoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private NfseService nfseService;

	/**
	 * Emite a nota. Em caso de rejeição, a nota fica gravada como REJEITADA com
	 * as mensagens da Sefin; em caso de falha de comunicação, fica PENDENTE (e
	 * pode ser consultada depois com {@link #consultarSituacao}).
	 */
	public NotaFiscalServico emitir(NotaFiscalServico nota) throws NfseException {
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		NfseNacionalClient client = criarCliente(cfg);

		// valida/monta antes de consumir um número de DPS
		DadosDps dados = montarDados(cfg, nota, 1);
		NfseNacionalClient.montarDps(dados);

		long numero = nfseService.reservarNumeroDps();
		dados = montarDados(cfg, nota, numero);
		String xml = client.assinar(NfseNacionalClient.montarDps(dados));

		nota.setAmbiente(cfg.getAmbiente());
		nota.setSerieDps(cfg.getSerieDps());
		nota.setNumeroDps(numero);
		nota.setIdDps(NfseNacionalClient.gerarIdDps(cfg.getCodigoMunicipioEmissor(), cfg.getCnpjPrestador(),
				cfg.getSerieDps(), numero));
		nota.setDataEmissao(new Date());
		nota.setXmlDps(xml);
		nota.setSituacao(NotaFiscalServico.SITUACAO_PENDENTE);
		nota.setMensagemRetorno("Enviando para a Sefin Nacional...");
		nota = nfseService.salvarNota(nota);

		RetornoNfse ret;
		try {
			ret = client.emitir(xml);
		} catch (NfseFalhaComunicacaoException e) {
			nota.setMensagemRetorno(e.getMessage()
					+ " Use \"Consultar situação\" para verificar se a nota foi gerada.");
			nfseService.salvarNota(nota);
			throw e;
		}
		aplicarRetorno(nota, ret);
		return nfseService.salvarNota(nota);
	}

	/** Consulta na Sefin se a DPS de uma nota pendente gerou NFS-e. */
	public NotaFiscalServico consultarSituacao(NotaFiscalServico nota) throws NfseException {
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		NfseNacionalClient client = criarCliente(cfg);
		RetornoNfse ret = nota.getChaveAcesso() != null ? client.consultarPorChave(nota.getChaveAcesso())
				: client.consultarPorDps(nota.getIdDps());
		if (ret.naoEncontrada && NotaFiscalServico.SITUACAO_PENDENTE.equals(nota.getSituacao())) {
			nota.setSituacao(NotaFiscalServico.SITUACAO_REJEITADA);
			nota.setMensagemRetorno("Nenhuma NFS-e foi gerada para esta DPS. Pode emitir novamente.");
		} else if (ret.sucesso) {
			aplicarRetorno(nota, ret);
		} else {
			throw new NfseException("Consulta não concluída: " + ret.mensagensTexto());
		}
		return nfseService.salvarNota(nota);
	}

	/**
	 * Cancela a NFS-e na Sefin Nacional (evento 101101). Se a Sefin recusar
	 * (prazo do município vencido, nota já cancelada etc.), a nota continua
	 * EMITIDA e a mensagem da Sefin é devolvida na exceção.
	 */
	public NotaFiscalServico cancelar(NotaFiscalServico nota, int motivo, String justificativa, String usuario)
			throws NfseException {
		if (!nota.isEmitida() || nota.getChaveAcesso() == null) {
			throw new NfseException("Só é possível cancelar uma NFS-e emitida.");
		}
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		if (nota.getAmbiente() != null && !nota.getAmbiente().equals(cfg.getAmbiente())) {
			throw new NfseException("Esta nota foi emitida em outro ambiente (" + (nota.getAmbiente() == 1 ? "produção"
					: "testes") + "). Troque o ambiente na Configuração NFS-e para cancelá-la.");
		}
		NfseNacionalClient client = criarCliente(cfg);
		String xml = client.assinar(NfseNacionalClient.montarPedidoCancelamento(cfg.getAmbiente(),
				cfg.getCnpjPrestador(), nota.getChaveAcesso(), motivo, justificativa,
				new Date(System.currentTimeMillis() - 60000)));

		RetornoEvento ret = client.registrarEvento(nota.getChaveAcesso(), xml);
		if (!ret.sucesso) {
			// pode já ter sido cancelada antes (ex.: pelo portal ou numa tentativa com timeout)
			RetornoEvento existente = client.consultarCancelamento(nota.getChaveAcesso());
			if (!existente.sucesso) {
				throw new NfseException("Cancelamento recusado pela Sefin Nacional: " + ret.mensagensTexto());
			}
			ret = existente;
		}
		nota.setSituacao(NotaFiscalServico.SITUACAO_CANCELADA);
		nota.setDataCancelamento(new Date());
		nota.setMotivoCancelamento(motivo);
		nota.setJustificativaCancelamento(justificativa == null ? null : justificativa.trim());
		nota.setUsuarioCancelamento(usuario);
		nota.setXmlEventoCancelamento(ret.xmlEvento != null ? ret.xmlEvento : xml);
		StringBuilder msg = new StringBuilder(nota.getMensagemRetorno() == null ? "" : nota.getMensagemRetorno());
		msg.append("\nNFS-e cancelada (").append(nota.getMotivoCancelamentoDescricao()).append(").");
		for (String alerta : ret.alertas) {
			msg.append("\nAlerta: ").append(alerta);
		}
		nota.setMensagemRetorno(msg.toString().trim());
		return nfseService.salvarNota(nota);
	}

	public byte[] baixarDanfse(NotaFiscalServico nota) throws NfseException {
		if (nota.getChaveAcesso() == null) {
			throw new NfseException("Esta nota não possui chave de acesso (não foi emitida).");
		}
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		return criarCliente(cfg).baixarDanfse(nota.getChaveAcesso());
	}

	/** Testa se o certificado abre com a senha informada; devolve o titular. */
	public String testarCertificado(NfseConfiguracao cfg) throws NfseException {
		NfseNacionalClient client = criarCliente(cfg);
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return client.getCertificado().getSubjectX500Principal().getName() + " - válido até "
				+ sdf.format(client.getCertificado().getNotAfter());
	}

	private void aplicarRetorno(NotaFiscalServico nota, RetornoNfse ret) {
		if (ret.sucesso) {
			nota.setSituacao(NotaFiscalServico.SITUACAO_EMITIDA);
			nota.setChaveAcesso(ret.chaveAcesso);
			if (ret.xmlNfse != null) {
				nota.setXmlNfse(ret.xmlNfse);
			}
			if (ret.numeroNfse != null) {
				nota.setNumeroNfse(ret.numeroNfse);
			}
			StringBuilder msg = new StringBuilder("NFS-e emitida com sucesso.");
			for (String alerta : ret.alertas) {
				msg.append("\nAlerta: ").append(alerta);
			}
			nota.setMensagemRetorno(msg.toString());
		} else {
			nota.setSituacao(NotaFiscalServico.SITUACAO_REJEITADA);
			nota.setMensagemRetorno(ret.mensagensTexto());
		}
	}

	private NfseNacionalClient criarCliente(NfseConfiguracao cfg) throws NfseException {
		String senha = CriptografiaSenhaCertificado.descriptografar(cfg.getSenhaCertificado());
		return new NfseNacionalClient(cfg.getCertificado(), senha, cfg.isProducao(),
				cfg.getAlgoritmoAssinatura());
	}

	private DadosDps montarDados(NfseConfiguracao cfg, NotaFiscalServico nota, long numero) {
		DadosDps d = new DadosDps();
		d.ambiente = cfg.getAmbiente() == null ? NfseConfiguracao.AMBIENTE_HOMOLOGACAO : cfg.getAmbiente();
		// um minuto antes, para não ser recusada por "data de emissão no futuro"
		// caso o relógio do servidor esteja um pouco adiantado
		d.dataHoraEmissao = new Date(System.currentTimeMillis() - 60000);
		d.competencia = nota.getCompetencia();
		d.serie = cfg.getSerieDps();
		d.numero = numero;
		d.codigoMunicipioEmissor = cfg.getCodigoMunicipioEmissor();

		d.cnpjPrestador = cfg.getCnpjPrestador();
		d.inscricaoMunicipalPrestador = cfg.getInscricaoMunicipal();
		d.telefonePrestador = cfg.getTelefonePrestador();
		d.emailPrestador = cfg.getEmailPrestador();
		d.opcaoSimplesNacional = valor(cfg.getOpcaoSimplesNacional(), 1);
		d.regimeApuracaoSimplesNacional = cfg.getRegimeApuracaoSimplesNacional();
		d.regimeEspecialTributacao = valor(cfg.getRegimeEspecialTributacao(), 0);

		Cliente c = nota.getCliente();
		if (c != null) {
			d.tomadorCpfCnpj = c.getCpfCnpj();
			d.tomadorNome = c.getNomeRazaoSocial();
			d.tomadorCodigoMunicipio = c.getCidade() != null ? c.getCidade().getCodigoIbge() : null;
			d.tomadorCep = c.getCep();
			d.tomadorLogradouro = c.getEndereco();
			d.tomadorNumero = c.getNumero();
			d.tomadorComplemento = c.getComplemento();
			d.tomadorBairro = c.getBairro();
			d.tomadorTelefone = c.getTelefone1();
			d.tomadorEmail = c.getEmail();
		}

		d.codigoMunicipioPrestacao = nota.getCodigoMunicipioPrestacao();
		d.codigoTributacaoNacional = nota.getCodigoTributacaoNacional();
		d.codigoTributacaoMunicipal = nota.getCodigoTributacaoMunicipal();
		d.codigoNbs = cfg.getCodigoNbs();
		d.descricaoServico = nota.getDescricaoServico();
		d.informacoesComplementares = cfg.getInformacoesComplementares();

		d.valorServico = nota.getValor();
		d.tributacaoIssqn = valor(cfg.getTributacaoIssqn(), 1);
		d.retencaoIssqn = valor(nota.getRetencaoIssqn(), 1);
		d.aliquotaIss = nota.getAliquotaIss();
		d.percentualTributosSimples = cfg.getPercentualTributosSimples();
		return d;
	}

	/** Preenche uma nota nova com os padrões da configuração. */
	public NotaFiscalServico novaNota(Cliente cliente, Date competencia, Double valor) {
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		NotaFiscalServico n = new NotaFiscalServico();
		n.setCliente(cliente);
		n.setCompetencia(competencia == null ? primeiroDiaMesAnterior() : competencia);
		n.setValor(valor == null ? 0. : valor);
		n.setCodigoTributacaoNacional(cfg.getCodigoTributacaoNacional());
		n.setCodigoTributacaoMunicipal(cfg.getCodigoTributacaoMunicipal());
		n.setRetencaoIssqn(cfg.getRetencaoIssqnPadrao());
		n.setAliquotaIss(cfg.getAliquotaIss());
		atualizarPadroesCliente(n);
		return n;
	}

	/** Recalcula descrição e local de prestação quando cliente/competência mudam. */
	public void atualizarPadroesCliente(NotaFiscalServico n) {
		NfseConfiguracao cfg = nfseService.carregarConfiguracao();
		String local = cfg.getCodigoMunicipioEmissor();
		Cliente c = n.getCliente();
		if ("TOMADOR".equals(cfg.getLocalPrestacaoPadrao()) && c != null && c.getCidade() != null
				&& c.getCidade().getCodigoIbge() != null && !c.getCidade().getCodigoIbge().isEmpty()) {
			local = c.getCidade().getCodigoIbge();
		}
		n.setCodigoMunicipioPrestacao(local);
		n.setDescricaoServico(aplicarModelo(cfg.getDescricaoServicoPadrao(), n));
	}

	private String aplicarModelo(String modelo, NotaFiscalServico n) {
		if (modelo == null) {
			return "";
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(n.getCompetencia() == null ? new Date() : n.getCompetencia());
		String mesNome = new SimpleDateFormat("MMMM", new Locale("pt", "BR")).format(cal.getTime());
		mesNome = mesNome.substring(0, 1).toUpperCase() + mesNome.substring(1);
		String cliente = n.getCliente() == null || n.getCliente().getNomeRazaoSocial() == null ? ""
				: n.getCliente().getNomeRazaoSocial();
		return modelo.replace("{MES_NOME}", mesNome)
				.replace("{MES}", String.format("%02d", cal.get(Calendar.MONTH) + 1))
				.replace("{ANO}", String.valueOf(cal.get(Calendar.YEAR))).replace("{CLIENTE}", cliente);
	}

	private static Date primeiroDiaMesAnterior() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return cal.getTime();
	}

	private static int valor(Integer v, int padrao) {
		return v == null ? padrao : v;
	}
}
