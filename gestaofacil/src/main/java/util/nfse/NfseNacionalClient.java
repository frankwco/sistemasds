package util.nfse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Cliente da API do Sistema Nacional NFS-e (Sefin Nacional + ADN/DANFSe).
 *
 * Fluxo de emissão: monta o XML da DPS (leiaute v1.01), assina com o
 * certificado A1 do prestador, compacta (GZip) + Base64 e envia via POST
 * /nfse em JSON ({"dpsXmlGZipB64": "..."}) usando TLS mútuo com o mesmo
 * certificado. A resposta traz a chave de acesso e o XML da NFS-e gerada.
 *
 * Classe sem dependências de JSF/CDI para poder ser testada isoladamente.
 */
public class NfseNacionalClient {

	public static final String NAMESPACE = "http://www.sped.fazenda.gov.br/nfse";
	public static final String VERSAO_DPS = "1.01";
	public static final String VERSAO_APLICATIVO = "GestaoFacil-1.0";

	private static final String URL_SEFIN_PRODUCAO = "https://sefin.nfse.gov.br/SefinNacional";
	private static final String URL_SEFIN_HOMOLOGACAO = "https://sefin.producaorestrita.nfse.gov.br/API/SefinNacional";
	private static final String URL_ADN_PRODUCAO = "https://adn.nfse.gov.br";
	private static final String URL_ADN_HOMOLOGACAO = "https://adn.producaorestrita.nfse.gov.br";

	private static final int TIMEOUT_CONEXAO_MS = 30000;
	private static final int TIMEOUT_LEITURA_MS = 90000;

	private final boolean producao;
	private final PrivateKey chavePrivada;
	private final X509Certificate certificado;
	private final SSLContext sslContext;
	private final boolean assinaturaSha1;
	private List<X509Certificate> cadeia = new ArrayList<>();
	private boolean cadeiaCompletadaAutomaticamente;

	/** ACs da ICP-Brasil (vigentes, fonte: ITI) usadas para completar a cadeia. */
	private static final String RECURSO_ACS_ICP = "/nfse/icp-brasil-acs.pem";
	private static volatile List<X509Certificate> acsIcpCache;

	/**
	 * @param certificadoPfx bytes do arquivo .pfx/.p12 (certificado A1)
	 * @param senha          senha do certificado
	 * @param producao       true = produção; false = produção restrita
	 * @param algoritmo      "SHA256" (padrão) ou "SHA1"
	 */
	public NfseNacionalClient(byte[] certificadoPfx, String senha, boolean producao, String algoritmo)
			throws NfseException {
		this.producao = producao;
		this.assinaturaSha1 = "SHA1".equalsIgnoreCase(algoritmo);
		if (certificadoPfx == null || certificadoPfx.length == 0) {
			throw new NfseException("Certificado digital A1 não foi carregado na configuração da NFS-e.");
		}
		char[] senhaChars = senha == null ? new char[0] : senha.toCharArray();
		try {
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(new ByteArrayInputStream(certificadoPfx), senhaChars);
			String alias = null;
			Enumeration<String> aliases = ks.aliases();
			while (aliases.hasMoreElements()) {
				String a = aliases.nextElement();
				if (ks.isKeyEntry(a)) {
					alias = a;
					break;
				}
			}
			if (alias == null) {
				throw new NfseException("O arquivo do certificado não contém chave privada (use o certificado A1 .pfx).");
			}
			this.chavePrivada = (PrivateKey) ks.getKey(alias, senhaChars);
			this.certificado = (X509Certificate) ks.getCertificate(alias);
			if (certificado.getNotAfter().before(new Date())) {
				throw new NfseException("O certificado digital venceu em "
						+ new SimpleDateFormat("dd/MM/yyyy").format(certificado.getNotAfter())
						+ ". Envie o certificado renovado na Configuração NFS-e.");
			}

			// Muitos .pfx vêm só com o certificado da empresa, sem as ACs
			// intermediárias; o servidor da Sefin então não consegue montar a
			// cadeia até a raiz ICP-Brasil e responde 403. Completa a cadeia com
			// as ACs oficiais da ICP-Brasil que vão junto com o sistema.
			java.security.cert.Certificate[] original = ks.getCertificateChain(alias);
			List<X509Certificate> completa = completarCadeia(original);
			cadeiaCompletadaAutomaticamente = completa.size() > (original == null ? 0 : original.length);
			if (cadeiaCompletadaAutomaticamente) {
				ks.setKeyEntry(alias, chavePrivada, senhaChars,
						completa.toArray(new java.security.cert.Certificate[0]));
			}
			this.cadeia = completa;

			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, senhaChars);
			this.sslContext = SSLContext.getInstance("TLSv1.2");
			this.sslContext.init(kmf.getKeyManagers(), criarTrustManagers(), null);
		} catch (NfseException e) {
			throw e;
		} catch (IOException e) {
			throw new NfseException("Não foi possível abrir o certificado. Verifique se a senha está correta.", e);
		} catch (Exception e) {
			throw new NfseException("Erro ao carregar o certificado digital: " + e.getMessage(), e);
		}
	}

	/** Raízes extras que o Java 8 não traz (servidores da Sefin usam GlobalSign Root R46). */
	private static final String[] RAIZES_EXTRAS = { "/nfse/globalsign-root-r46.pem" };

	/**
	 * Confia nas autoridades padrão do Java MAIS as raízes extras acima. A
	 * verificação do certificado do servidor continua ativa normalmente.
	 */
	private static javax.net.ssl.TrustManager[] criarTrustManagers() throws Exception {
		KeyStore confiaveis = KeyStore.getInstance(KeyStore.getDefaultType());
		confiaveis.load(null, null);
		javax.net.ssl.TrustManagerFactory padrao = javax.net.ssl.TrustManagerFactory
				.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
		padrao.init((KeyStore) null);
		int i = 0;
		for (javax.net.ssl.TrustManager tm : padrao.getTrustManagers()) {
			if (tm instanceof javax.net.ssl.X509TrustManager) {
				for (X509Certificate c : ((javax.net.ssl.X509TrustManager) tm).getAcceptedIssuers()) {
					confiaveis.setCertificateEntry("padrao-" + (i++), c);
				}
			}
		}
		java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
		for (String recurso : RAIZES_EXTRAS) {
			try (InputStream is = NfseNacionalClient.class.getResourceAsStream(recurso)) {
				if (is != null) {
					confiaveis.setCertificateEntry("extra-" + (i++), cf.generateCertificate(is));
				}
			}
		}
		javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory
				.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(confiaveis);
		return tmf.getTrustManagers();
	}

	public X509Certificate getCertificado() {
		return certificado;
	}

	public List<X509Certificate> getCadeia() {
		return cadeia;
	}

	public boolean isCadeiaCompletadaAutomaticamente() {
		return cadeiaCompletadaAutomaticamente;
	}

	/** A cadeia termina numa raiz ICP-Brasil ("Autoridade Certificadora Raiz Brasileira")? */
	public boolean isCadeiaIcpBrasil() {
		if (cadeia.isEmpty()) {
			return false;
		}
		X509Certificate ultimo = cadeia.get(cadeia.size() - 1);
		return ultimo.getIssuerX500Principal().getName().contains("Raiz Brasileira");
	}

	/**
	 * Acrescenta à cadeia do .pfx as ACs que faltam até a raiz, procurando cada
	 * emissor na lista oficial da ICP-Brasil (conferindo a assinatura).
	 */
	static List<X509Certificate> completarCadeia(java.security.cert.Certificate[] original) {
		List<X509Certificate> lista = new ArrayList<>();
		if (original != null) {
			for (java.security.cert.Certificate c : original) {
				lista.add((X509Certificate) c);
			}
		}
		if (lista.isEmpty()) {
			return lista;
		}
		List<X509Certificate> acs = acsIcpBrasil();
		for (int i = 0; i < 6; i++) {
			X509Certificate ultimo = lista.get(lista.size() - 1);
			if (ultimo.getSubjectX500Principal().equals(ultimo.getIssuerX500Principal())) {
				break; // chegou na raiz
			}
			X509Certificate emissor = null;
			for (X509Certificate ac : acs) {
				if (ac.getSubjectX500Principal().equals(ultimo.getIssuerX500Principal())) {
					try {
						ultimo.verify(ac.getPublicKey());
						emissor = ac;
						break;
					} catch (Exception e) {
						// mesma AC com outra chave (versão antiga): tenta a próxima
					}
				}
			}
			if (emissor == null) {
				break;
			}
			lista.add(emissor);
		}
		return lista;
	}

	private static List<X509Certificate> acsIcpBrasil() {
		List<X509Certificate> acs = acsIcpCache;
		if (acs != null) {
			return acs;
		}
		acs = new ArrayList<>();
		try (InputStream is = NfseNacionalClient.class.getResourceAsStream(RECURSO_ACS_ICP)) {
			if (is != null) {
				for (java.security.cert.Certificate c : java.security.cert.CertificateFactory.getInstance("X.509")
						.generateCertificates(is)) {
					acs.add((X509Certificate) c);
				}
			}
		} catch (Exception e) {
			System.err.println("[NFS-e] Não foi possível ler " + RECURSO_ACS_ICP + ": " + e.getMessage());
		}
		acsIcpCache = acs;
		return acs;
	}

	/** Descrição da cadeia, do titular até a raiz, para a tela de diagnóstico. */
	public String descreverCadeia() {
		StringBuilder sb = new StringBuilder();
		for (X509Certificate c : cadeia) {
			if (sb.length() > 0) {
				sb.append(" → ");
			}
			sb.append(nomeComum(c));
		}
		return sb.toString();
	}

	private static String nomeComum(X509Certificate c) {
		Matcher m = Pattern.compile("CN=([^,]+)").matcher(c.getSubjectX500Principal().getName());
		return m.find() ? m.group(1) : c.getSubjectX500Principal().getName();
	}

	/**
	 * Faz uma consulta só de leitura (parâmetros do convênio do município) para
	 * saber se a Sefin aceita o certificado. Devolve null se aceitou, ou a
	 * mensagem de erro.
	 */
	public String testarAcesso(String codigoMunicipio) {
		return testarAcessoEm(urlSefin(), codigoMunicipio);
	}

	/** Endereços da Sefin testados no diagnóstico (rótulo, URL base). */
	public static String[][] enderecosDiagnostico() {
		return new String[][] { { "Testes /API/SefinNacional", URL_SEFIN_HOMOLOGACAO },
				{ "Testes /SefinNacional", "https://sefin.producaorestrita.nfse.gov.br/SefinNacional" },
				{ "Produção", URL_SEFIN_PRODUCAO } };
	}

	/** Igual a {@link #testarAcesso}, mas num endereço base informado. */
	public String testarAcessoEm(String urlBase, String codigoMunicipio) {
		try {
			RespostaHttp r = requisitar("GET", urlBase + "/parametros_municipais/" + codigoMunicipio + "/convenio",
					null, "application/json");
			if (r.status == 403) {
				return "o servidor da Receita recusou o certificado (HTTP 403)";
			}
			if (r.status >= 500) {
				return "o servidor da Receita está com problema agora (HTTP " + r.status + ")";
			}
			return null; // 200, 404 etc.: passou pela autenticação
		} catch (NfseException e) {
			return e.getMessage();
		}
	}

	// =====================================================================
	// Montagem da DPS
	// =====================================================================

	/**
	 * Identificador da DPS (45 posições): "DPS" + cMun(7) + tpInsc(1: 1=CPF,
	 * 2=CNPJ) + inscrição federal(14) + série(5) + nDPS(15).
	 */
	public static String gerarIdDps(String codigoMunicipio, String cpfCnpjPrestador, String serie, long numero) {
		String doc = somenteDigitos(cpfCnpjPrestador);
		String tpInsc = doc.length() == 11 ? "1" : "2";
		return "DPS" + codigoMunicipio + tpInsc + zeros(doc, 14) + zeros(somenteDigitos(serie), 5)
				+ zeros(String.valueOf(numero), 15);
	}

	/** Gera o XML (ainda sem assinatura) da DPS. */
	public static Document montarDps(DadosDps d) throws NfseException {
		validar(d);
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().newDocument();

			Element dps = doc.createElementNS(NAMESPACE, "DPS");
			dps.setAttribute("versao", VERSAO_DPS);
			doc.appendChild(dps);

			Element inf = el(doc, dps, "infDPS", null);
			String id = gerarIdDps(d.codigoMunicipioEmissor, d.cnpjPrestador, d.serie, d.numero);
			inf.setAttribute("Id", id);
			inf.setIdAttribute("Id", true);

			el(doc, inf, "tpAmb", String.valueOf(d.ambiente));
			el(doc, inf, "dhEmi", formatarDataHora(d.dataHoraEmissao));
			el(doc, inf, "verAplic", VERSAO_APLICATIVO);
			el(doc, inf, "serie", somenteDigitos(d.serie));
			el(doc, inf, "nDPS", String.valueOf(d.numero));
			el(doc, inf, "dCompet", formatarData(d.competencia));
			el(doc, inf, "tpEmit", "1");
			el(doc, inf, "cLocEmi", d.codigoMunicipioEmissor);

			// ---- Prestador (o próprio emitente: nome/endereço vêm do cadastro nacional)
			Element prest = el(doc, inf, "prest", null);
			el(doc, prest, documentoTag(d.cnpjPrestador), somenteDigitos(d.cnpjPrestador));
			if (temTexto(d.inscricaoMunicipalPrestador)) {
				el(doc, prest, "IM", d.inscricaoMunicipalPrestador.trim());
			}
			String fonePrest = somenteDigitos(d.telefonePrestador);
			if (fonePrest.length() >= 6) {
				el(doc, prest, "fone", fonePrest);
			}
			if (temTexto(d.emailPrestador)) {
				el(doc, prest, "email", d.emailPrestador.trim());
			}
			Element regTrib = el(doc, prest, "regTrib", null);
			el(doc, regTrib, "opSimpNac", String.valueOf(d.opcaoSimplesNacional));
			if (d.opcaoSimplesNacional == 3 && d.regimeApuracaoSimplesNacional != null) {
				el(doc, regTrib, "regApTribSN", String.valueOf(d.regimeApuracaoSimplesNacional));
			}
			el(doc, regTrib, "regEspTrib", String.valueOf(d.regimeEspecialTributacao));

			// ---- Tomador
			Element toma = el(doc, inf, "toma", null);
			el(doc, toma, documentoTag(d.tomadorCpfCnpj), somenteDigitos(d.tomadorCpfCnpj));
			el(doc, toma, "xNome", limitar(limpar(d.tomadorNome), 300));
			if (d.temEnderecoTomador()) {
				Element end = el(doc, toma, "end", null);
				Element endNac = el(doc, end, "endNac", null);
				el(doc, endNac, "cMun", d.tomadorCodigoMunicipio);
				el(doc, endNac, "CEP", somenteDigitos(d.tomadorCep));
				el(doc, end, "xLgr", limitar(limpar(d.tomadorLogradouro), 255));
				el(doc, end, "nro", limitar(limpar(d.tomadorNumero), 60));
				if (temTexto(d.tomadorComplemento)) {
					el(doc, end, "xCpl", limitar(limpar(d.tomadorComplemento), 156));
				}
				el(doc, end, "xBairro", limitar(limpar(d.tomadorBairro), 60));
			}
			String foneToma = somenteDigitos(d.tomadorTelefone);
			if (foneToma.length() >= 6 && foneToma.length() <= 20) {
				el(doc, toma, "fone", foneToma);
			}
			if (temTexto(d.tomadorEmail) && d.tomadorEmail.contains("@")) {
				el(doc, toma, "email", limitar(d.tomadorEmail.trim(), 80));
			}

			// ---- Serviço
			Element serv = el(doc, inf, "serv", null);
			Element locPrest = el(doc, serv, "locPrest", null);
			el(doc, locPrest, "cLocPrestacao", d.codigoMunicipioPrestacao);
			Element cServ = el(doc, serv, "cServ", null);
			el(doc, cServ, "cTribNac", somenteDigitos(d.codigoTributacaoNacional));
			if (temTexto(d.codigoTributacaoMunicipal)) {
				el(doc, cServ, "cTribMun", somenteDigitos(d.codigoTributacaoMunicipal));
			}
			el(doc, cServ, "xDescServ", limitar(limpar(d.descricaoServico), 2000));
			if (temTexto(d.codigoNbs)) {
				el(doc, cServ, "cNBS", somenteDigitos(d.codigoNbs));
			}
			if (temTexto(d.informacoesComplementares)) {
				Element infoCompl = el(doc, serv, "infoCompl", null);
				el(doc, infoCompl, "xInfComp", limitar(limpar(d.informacoesComplementares), 2000));
			}

			// ---- Valores
			Element valores = el(doc, inf, "valores", null);
			Element vServPrest = el(doc, valores, "vServPrest", null);
			el(doc, vServPrest, "vServ", decimal(d.valorServico));
			Element trib = el(doc, valores, "trib", null);
			Element tribMun = el(doc, trib, "tribMun", null);
			el(doc, tribMun, "tribISSQN", String.valueOf(d.tributacaoIssqn));
			el(doc, tribMun, "tpRetISSQN", String.valueOf(d.retencaoIssqn));
			if (d.aliquotaIss != null && d.aliquotaIss > 0 && !d.aliquotaProibida()) {
				el(doc, tribMun, "pAliq", decimal(d.aliquotaIss));
			}
			if (d.opcaoSimplesNacional == 3) {
				// igual ao Emissor Nacional para ME/EPP: PIS/COFINS sem destaque nem retenção
				Element piscofins = el(doc, el(doc, trib, "tribFed", null), "piscofins", null);
				el(doc, piscofins, "CST", "00");
				el(doc, piscofins, "tpRetPisCofins", "0");
			}
			Element totTrib = el(doc, trib, "totTrib", null);
			if (d.opcaoSimplesNacional == 3 && d.percentualTributosSimples != null
					&& d.percentualTributosSimples > 0) {
				el(doc, totTrib, "pTotTribSN", decimal(d.percentualTributosSimples));
			} else {
				el(doc, totTrib, "indTotTrib", "0");
			}
			return doc;
		} catch (Exception e) {
			throw new NfseException("Erro ao montar o XML da DPS: " + e.getMessage(), e);
		}
	}

	private static void validar(DadosDps d) throws NfseException {
		List<String> erros = new ArrayList<>();
		if (somenteDigitos(d.cnpjPrestador).length() != 14 && somenteDigitos(d.cnpjPrestador).length() != 11) {
			erros.add("CNPJ do prestador inválido na configuração");
		}
		if (!ibge(d.codigoMunicipioEmissor)) {
			erros.add("Código IBGE do município emissor inválido na configuração (7 dígitos)");
		}
		if (!ibge(d.codigoMunicipioPrestacao)) {
			erros.add("Código IBGE do município de prestação inválido (7 dígitos)");
		}
		if (!somenteDigitos(d.serie).matches("\\d{1,5}")) {
			erros.add("Série da DPS inválida (até 5 dígitos)");
		}
		if (d.numero <= 0) {
			erros.add("Número da DPS inválido");
		}
		int docToma = somenteDigitos(d.tomadorCpfCnpj).length();
		if (docToma != 11 && docToma != 14) {
			erros.add("CPF/CNPJ do cliente inválido ou não cadastrado");
		}
		if (!temTexto(d.tomadorNome)) {
			erros.add("Nome/razão social do cliente não informado");
		}
		if (!somenteDigitos(d.codigoTributacaoNacional).matches("\\d{6}")) {
			erros.add("Código de tributação nacional deve ter 6 dígitos");
		}
		if (temTexto(d.codigoTributacaoMunicipal) && !somenteDigitos(d.codigoTributacaoMunicipal).matches("\\d{3}")) {
			erros.add("Código de tributação municipal deve ter 3 dígitos");
		}
		if (temTexto(d.codigoNbs) && !somenteDigitos(d.codigoNbs).matches("\\d{9}")) {
			erros.add("Código NBS deve ter 9 dígitos");
		}
		if (!temTexto(d.descricaoServico)) {
			erros.add("Descrição do serviço não informada");
		} else if (Pattern.compile("\\{[A-Z_]+\\}").matcher(d.descricaoServico).find()) {
			erros.add("A descrição ainda tem campo a preencher (ex.: {BOLETO}). Informe o vencimento do boleto e o mês de"
					+ " referência, ou corrija o texto");
		}
		if (d.valorServico == null || d.valorServico <= 0) {
			erros.add("Valor do serviço deve ser maior que zero");
		}
		if (d.aliquotaIss != null && d.aliquotaIss >= 10) {
			erros.add("Alíquota do ISS inválida");
		}
		if (d.competencia == null) {
			erros.add("Competência não informada");
		}
		// Anexo I: ME/EPP nunca pode informar indTotTrib; Não Optante não pode
		// informar indTotTrib nem pTotTribSN. Só o MEI pode usar indTotTrib.
		if (d.opcaoSimplesNacional == 3
				&& (d.percentualTributosSimples == null || d.percentualTributosSimples <= 0)) {
			erros.add("Informe o % de tributos do Simples do mês da competência (cadastre em Configuração NFS-e > Alíquotas do mês)");
		}
		if (d.opcaoSimplesNacional == 3 && d.regimeApuracaoSimplesNacional == null) {
			erros.add("Selecione o regime de apuração do Simples na Configuração NFS-e");
		}
		// E0621/E0628: ISSQN pelo Simples e retido exige alíquota de no mínimo 1,8%
		if (d.issPeloSimples() && d.retencaoIssqn != 1 && (d.aliquotaIss == null || d.aliquotaIss < 1.8)) {
			erros.add("Com ISS retido é obrigatório informar a alíquota do ISS (mínimo 1,80%)");
		}
		if (d.opcaoSimplesNacional == 1) {
			erros.add("Emissão para empresa não optante do Simples ainda não é suportada"
					+ " (falta informar os tributos federais, estaduais e municipais)");
		}
		if (!erros.isEmpty()) {
			throw new NfseException(String.join("; ", erros) + ".");
		}
	}

	// =====================================================================
	// Assinatura
	// =====================================================================

	/**
	 * Assina (XMLDSig enveloped) o primeiro elemento filho da raiz (infDPS na
	 * DPS, infPedReg no pedido de evento) e devolve o XML em texto.
	 */
	public String assinar(Document doc) throws NfseException {
		try {
			Element inf = primeiroFilho(doc.getDocumentElement());
			String id = inf.getAttribute("Id");
			inf.setIdAttribute("Id", true);

			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			List<Transform> transforms = new ArrayList<>();
			transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
			transforms.add(fac.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null));

			String digest = assinaturaSha1 ? DigestMethod.SHA1 : "http://www.w3.org/2001/04/xmlenc#sha256";
			String metodo = assinaturaSha1 ? SignatureMethod.RSA_SHA1
					: "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

			Reference ref = fac.newReference("#" + id, fac.newDigestMethod(digest, null), transforms, null, null);
			SignedInfo si = fac.newSignedInfo(
					fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
					fac.newSignatureMethod(metodo, null), Collections.singletonList(ref));

			KeyInfoFactory kif = fac.getKeyInfoFactory();
			X509Data x509 = kif.newX509Data(Collections.singletonList(certificado));
			KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509));

			DOMSignContext ctx = new DOMSignContext(chavePrivada, doc.getDocumentElement());
			XMLSignature sig = fac.newXMLSignature(si, ki);
			sig.sign(ctx);
			// O Java quebra o Base64 em linhas de 76 caracteres (com &#13;). Esses
			// elementos não fazem parte do conteúdo assinado (SignedInfo), então
			// remover as quebras não invalida a assinatura e evita rejeição.
			for (String tag : new String[] { "SignatureValue", "X509Certificate" }) {
				org.w3c.dom.NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, tag);
				for (int i = 0; i < nl.getLength(); i++) {
					nl.item(i).setTextContent(nl.item(i).getTextContent().replaceAll("\\s", ""));
				}
			}
			return paraTexto(doc);
		} catch (Exception e) {
			throw new NfseException("Erro ao assinar a DPS: " + e.getMessage(), e);
		}
	}

	// =====================================================================
	// Comunicação com a API
	// =====================================================================

	// =====================================================================
	// Cancelamento (evento 101101)
	// =====================================================================

	public static final String EVENTO_CANCELAMENTO = "101101";

	/** Identificador do pedido: "PRE" + chave de acesso (50) + tipo do evento (6). */
	public static String gerarIdPedidoEvento(String chaveAcesso, String tipoEvento) {
		return "PRE" + chaveAcesso + tipoEvento;
	}

	/**
	 * Monta (sem assinatura) o Pedido de Registro de Evento de cancelamento.
	 *
	 * @param motivo 1 - Erro na emissão; 2 - Serviço não prestado; 9 - Outros
	 * @param justificativa texto de 15 a 255 caracteres
	 */
	public static Document montarPedidoCancelamento(int ambiente, String cpfCnpjAutor, String chaveAcesso,
			int motivo, String justificativa, Date dataHora) throws NfseException {
		String chave = somenteDigitos(chaveAcesso);
		String texto = limpar(justificativa).replace('\n', ' ');
		List<String> erros = new ArrayList<>();
		if (chave.length() != 50) {
			erros.add("Chave de acesso da NFS-e inválida");
		}
		if (motivo != 1 && motivo != 2 && motivo != 9) {
			erros.add("Motivo do cancelamento inválido");
		}
		if (texto.length() < 15 || texto.length() > 255) {
			erros.add("A justificativa do cancelamento deve ter de 15 a 255 caracteres");
		}
		int doc = somenteDigitos(cpfCnpjAutor).length();
		if (doc != 11 && doc != 14) {
			erros.add("CNPJ do prestador inválido na configuração");
		}
		if (!erros.isEmpty()) {
			throw new NfseException(String.join("; ", erros) + ".");
		}
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document d = dbf.newDocumentBuilder().newDocument();
			Element ped = d.createElementNS(NAMESPACE, "pedRegEvento");
			ped.setAttribute("versao", VERSAO_DPS);
			d.appendChild(ped);
			Element inf = el(d, ped, "infPedReg", null);
			inf.setAttribute("Id", gerarIdPedidoEvento(chave, EVENTO_CANCELAMENTO));
			inf.setIdAttribute("Id", true);
			el(d, inf, "tpAmb", String.valueOf(ambiente));
			el(d, inf, "verAplic", VERSAO_APLICATIVO);
			el(d, inf, "dhEvento", formatarDataHora(dataHora));
			el(d, inf, doc == 11 ? "CPFAutor" : "CNPJAutor", somenteDigitos(cpfCnpjAutor));
			el(d, inf, "chNFSe", chave);
			Element ev = el(d, inf, "e" + EVENTO_CANCELAMENTO, null);
			el(d, ev, "xDesc", "Cancelamento de NFS-e");
			el(d, ev, "cMotivo", String.valueOf(motivo));
			el(d, ev, "xMotivo", texto);
			return d;
		} catch (Exception e) {
			throw new NfseException("Erro ao montar o pedido de cancelamento: " + e.getMessage(), e);
		}
	}

	/** Envia o pedido de evento assinado (POST /nfse/{chave}/eventos). */
	public RetornoEvento registrarEvento(String chaveAcesso, String xmlPedidoAssinado) throws NfseException {
		JsonObject corpo = new JsonObject();
		corpo.addProperty("pedidoRegistroEventoXmlGZipB64", gzipBase64(xmlPedidoAssinado));
		RespostaHttp r = requisitar("POST", urlSefin() + "/nfse/" + somenteDigitos(chaveAcesso) + "/eventos",
				corpo.toString(), "application/json");
		RetornoEvento ret = new RetornoEvento();
		ret.httpStatus = r.status;
		String texto = r.textoUtf8();
		JsonObject json = json(texto);
		if (json != null) {
			// o nome do campo do XML do evento varia entre versões da API
			for (Map.Entry<String, JsonElement> e : json.entrySet()) {
				if (e.getKey().toLowerCase().contains("eventoxmlgzipb64") && e.getValue().isJsonPrimitive()) {
					try {
						ret.xmlEvento = gunzipBase64(e.getValue().getAsString());
					} catch (IOException ex) {
						ret.mensagens.add("Evento registrado, mas não foi possível ler o XML retornado.");
					}
				}
			}
			ret.alertas.addAll(extrairLista(json, "alertas"));
		}
		ret.sucesso = r.status >= 200 && r.status < 300 && (ret.xmlEvento != null || extrairMensagens(json, texto).isEmpty());
		if (!ret.sucesso) {
			ret.mensagens.addAll(extrairMensagens(json, texto));
			if (ret.mensagens.isEmpty()) {
				ret.mensagens.add("Retorno inesperado da Sefin Nacional (HTTP " + r.status + ").");
			}
		}
		return ret;
	}

	/** Consulta se já existe evento de cancelamento registrado para a nota. */
	public RetornoEvento consultarCancelamento(String chaveAcesso) throws NfseException {
		RespostaHttp r = requisitar("GET", urlSefin() + "/nfse/" + somenteDigitos(chaveAcesso) + "/eventos/"
				+ EVENTO_CANCELAMENTO + "/1", null, "application/json");
		RetornoEvento ret = new RetornoEvento();
		ret.httpStatus = r.status;
		if (r.status == 404) {
			return ret;
		}
		String texto = r.textoUtf8();
		JsonObject json = json(texto);
		if (r.status >= 200 && r.status < 300 && json != null) {
			for (Map.Entry<String, JsonElement> e : json.entrySet()) {
				if (e.getKey().toLowerCase().contains("xmlgzipb64") && e.getValue().isJsonPrimitive()) {
					try {
						ret.xmlEvento = gunzipBase64(e.getValue().getAsString());
					} catch (IOException ex) {
						// ignora
					}
				}
			}
			ret.sucesso = ret.xmlEvento != null || texto.contains(EVENTO_CANCELAMENTO);
		} else {
			ret.mensagens.addAll(extrairMensagens(json, texto));
		}
		return ret;
	}

	private static Element primeiroFilho(Element pai) {
		for (org.w3c.dom.Node n = pai.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n instanceof Element) {
				return (Element) n;
			}
		}
		throw new IllegalStateException("XML sem elemento a assinar");
	}

	/** Envia a DPS assinada e devolve o resultado (emitida ou rejeitada). */
	public RetornoNfse emitir(String xmlDpsAssinado) throws NfseException {
		JsonObject corpo = new JsonObject();
		corpo.addProperty("dpsXmlGZipB64", gzipBase64(xmlDpsAssinado));
		RespostaHttp r = requisitar("POST", urlSefin() + "/nfse", corpo.toString(), "application/json");
		return interpretarRetornoNfse(r);
	}

	/** Consulta a NFS-e gerada a partir do identificador da DPS. */
	public RetornoNfse consultarPorDps(String idDps) throws NfseException {
		RespostaHttp r = requisitar("GET", urlSefin() + "/dps/" + idDps, null, "application/json");
		RetornoNfse ret = new RetornoNfse();
		ret.httpStatus = r.status;
		if (r.status == 404) {
			ret.naoEncontrada = true;
			ret.mensagens.add("Nenhuma NFS-e foi gerada para esta DPS.");
			return ret;
		}
		JsonObject json = json(r.textoUtf8());
		String chave = json == null ? null : texto(json, "chaveAcesso");
		if (r.status >= 300 || chave == null) {
			ret.mensagens.addAll(extrairMensagens(json, r.textoUtf8()));
			return ret;
		}
		return consultarPorChave(chave);
	}

	/** Consulta a NFS-e pela chave de acesso (devolve o XML da nota). */
	public RetornoNfse consultarPorChave(String chaveAcesso) throws NfseException {
		RespostaHttp r = requisitar("GET", urlSefin() + "/nfse/" + chaveAcesso, null, "application/json");
		RetornoNfse ret = interpretarRetornoNfse(r);
		if (ret.chaveAcesso == null && ret.sucesso) {
			ret.chaveAcesso = chaveAcesso;
		}
		return ret;
	}

	/** Baixa o PDF (DANFSe) da nota pela chave de acesso. */
	public byte[] baixarDanfse(String chaveAcesso) throws NfseException {
		RespostaHttp r = requisitar("GET", urlAdn() + "/danfse/" + chaveAcesso, null, "application/pdf");
		if (r.status != 200 || r.corpo.length < 5 || !new String(r.corpo, 0, 4, StandardCharsets.ISO_8859_1).equals("%PDF")) {
			throw new NfseException("Não foi possível obter o PDF da NFS-e (HTTP " + r.status + "): "
					+ String.join("; ", extrairMensagens(json(r.textoUtf8()), r.textoUtf8())));
		}
		return r.corpo;
	}

	private RetornoNfse interpretarRetornoNfse(RespostaHttp r) {
		RetornoNfse ret = new RetornoNfse();
		ret.httpStatus = r.status;
		String texto = r.textoUtf8();
		JsonObject json = json(texto);
		if (json != null) {
			ret.chaveAcesso = texto(json, "chaveAcesso");
			ret.idDps = texto(json, "idDps");
			String xmlB64 = texto(json, "nfseXmlGZipB64");
			if (xmlB64 != null) {
				try {
					ret.xmlNfse = gunzipBase64(xmlB64);
					ret.numeroNfse = extrairTag(ret.xmlNfse, "nNFSe");
				} catch (IOException e) {
					ret.mensagens.add("NFS-e gerada, mas não foi possível ler o XML retornado: " + e.getMessage());
				}
			}
			for (String alerta : extrairLista(json, "alertas")) {
				ret.alertas.add(alerta);
			}
		}
		ret.sucesso = r.status >= 200 && r.status < 300 && ret.chaveAcesso != null;
		if (!ret.sucesso) {
			ret.mensagens.addAll(extrairMensagens(json, texto));
			if (ret.mensagens.isEmpty()) {
				ret.mensagens.add("Retorno inesperado da Sefin Nacional (HTTP " + r.status + ").");
			}
		}
		return ret;
	}

	private String urlSefin() {
		return producao ? URL_SEFIN_PRODUCAO : URL_SEFIN_HOMOLOGACAO;
	}

	private String urlAdn() {
		return producao ? URL_ADN_PRODUCAO : URL_ADN_HOMOLOGACAO;
	}

	private RespostaHttp requisitar(String metodo, String url, String corpo, String accept) throws NfseException {
		HttpsURLConnection con = null;
		try {
			con = (HttpsURLConnection) new URL(url).openConnection();
			con.setSSLSocketFactory(sslContext.getSocketFactory());
			con.setRequestMethod(metodo);
			con.setConnectTimeout(TIMEOUT_CONEXAO_MS);
			con.setReadTimeout(TIMEOUT_LEITURA_MS);
			con.setRequestProperty("Accept", accept);
			if (corpo != null) {
				con.setDoOutput(true);
				con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
				try (OutputStream os = con.getOutputStream()) {
					os.write(corpo.getBytes(StandardCharsets.UTF_8));
				}
			}
			RespostaHttp r = new RespostaHttp();
			r.status = con.getResponseCode();
			InputStream is = r.status >= 400 ? con.getErrorStream() : con.getInputStream();
			r.corpo = is == null ? new byte[0] : lerTudo(is);
			return r;
		} catch (IOException e) {
			throw new NfseFalhaComunicacaoException(
					"Falha de comunicação com o Sistema Nacional NFS-e (" + e.getClass().getSimpleName() + ": "
							+ e.getMessage() + ").",
					e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	// =====================================================================
	// Utilitários
	// =====================================================================

	private static List<String> extrairMensagens(JsonObject json, String textoBruto) {
		List<String> msgs = new ArrayList<>();
		if (json != null) {
			msgs.addAll(extrairLista(json, "erros"));
			msgs.addAll(extrairLista(json, "erro"));
			if (msgs.isEmpty()) {
				for (String campo : new String[] { "mensagem", "message", "detail", "title" }) {
					String t = texto(json, campo);
					if (t != null) {
						msgs.add(t);
					}
				}
			}
		} else if (textoBruto != null && !textoBruto.trim().isEmpty()) {
			String t = textoBruto.trim();
			if (t.startsWith("<")) {
				// página de erro HTML (ex.: 403 do servidor): mostra só o título
				Matcher m = Pattern.compile("(?is)<title>(.*?)</title>").matcher(t);
				String titulo = m.find() ? m.group(1).trim() : "erro HTTP";
				if (titulo.startsWith("403")) {
					titulo += " - o servidor recusou o certificado digital (confira se é o e-CNPJ A1 do"
							+ " prestador, dentro da validade, e se o CNPJ da configuração é o do certificado)";
				}
				msgs.add(titulo);
			} else {
				msgs.add(limitar(t, 500));
			}
		}
		return msgs;
	}

	/** Lê um campo (objeto ou lista de objetos {codigo, descricao, complemento}). */
	private static List<String> extrairLista(JsonObject json, String campo) {
		List<String> out = new ArrayList<>();
		JsonElement el = campoIgnorandoCaixa(json, campo);
		if (el == null || el.isJsonNull()) {
			return out;
		}
		JsonArray arr;
		if (el.isJsonArray()) {
			arr = el.getAsJsonArray();
		} else {
			arr = new JsonArray();
			arr.add(el);
		}
		for (JsonElement item : arr) {
			if (item.isJsonObject()) {
				JsonObject o = item.getAsJsonObject();
				String codigo = texto(o, "codigo");
				String descricao = texto(o, "descricao");
				String complemento = texto(o, "complemento");
				StringBuilder sb = new StringBuilder();
				if (codigo != null) {
					sb.append(codigo).append(" - ");
				}
				sb.append(descricao != null ? descricao : o.toString());
				if (complemento != null && !complemento.isEmpty()) {
					sb.append(" (").append(complemento).append(")");
				}
				out.add(sb.toString());
			} else if (item.isJsonPrimitive()) {
				out.add(item.getAsString());
			}
		}
		return out;
	}

	private static JsonElement campoIgnorandoCaixa(JsonObject json, String campo) {
		for (Map.Entry<String, JsonElement> e : json.entrySet()) {
			if (e.getKey().equalsIgnoreCase(campo)) {
				return e.getValue();
			}
		}
		return null;
	}

	private static String texto(JsonObject json, String campo) {
		JsonElement el = campoIgnorandoCaixa(json, campo);
		if (el == null || el.isJsonNull()) {
			return null;
		}
		return el.isJsonPrimitive() ? el.getAsString() : el.toString();
	}

	private static JsonObject json(String texto) {
		try {
			if (texto == null || texto.trim().isEmpty()) {
				return null;
			}
			JsonElement el = new JsonParser().parse(texto);
			return el.isJsonObject() ? el.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static String extrairTag(String xml, String tag) {
		if (xml == null) {
			return null;
		}
		Matcher m = Pattern.compile("<(?:\\w+:)?" + tag + ">([^<]*)</(?:\\w+:)?" + tag + ">").matcher(xml);
		return m.find() ? m.group(1) : null;
	}

	public static String gzipBase64(String texto) throws NfseException {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
				gz.write(texto.getBytes(StandardCharsets.UTF_8));
			}
			return Base64.getEncoder().encodeToString(bos.toByteArray());
		} catch (IOException e) {
			throw new NfseException("Erro ao compactar a DPS: " + e.getMessage(), e);
		}
	}

	public static String gunzipBase64(String b64) throws IOException {
		byte[] dados = Base64.getDecoder().decode(b64);
		try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(dados))) {
			return new String(lerTudo(gz), StandardCharsets.UTF_8);
		}
	}

	private static byte[] lerTudo(InputStream is) throws IOException {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = is.read(buf)) != -1) {
				bos.write(buf, 0, n);
			}
			return bos.toByteArray();
		} finally {
			is.close();
		}
	}

	public static String paraTexto(Document doc) throws Exception {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.INDENT, "no");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}

	private static Element el(Document doc, Element pai, String nome, String valor) {
		Element e = doc.createElementNS(NAMESPACE, nome);
		if (valor != null) {
			e.setTextContent(valor);
		}
		pai.appendChild(e);
		return e;
	}

	private static String documentoTag(String doc) {
		return somenteDigitos(doc).length() == 11 ? "CPF" : "CNPJ";
	}

	/** Data/hora no formato AAAA-MM-DDThh:mm:ss-03:00 (horário de Brasília). */
	public static String formatarDataHora(Date data) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
		return sdf.format(data);
	}

	public static String formatarData(Date data) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
		return sdf.format(data);
	}

	public static String decimal(Double valor) {
		return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	public static String somenteDigitos(String s) {
		return s == null ? "" : s.replaceAll("\\D", "");
	}

	private static boolean ibge(String s) {
		return s != null && s.matches("\\d{7}");
	}

	private static boolean temTexto(String s) {
		return s != null && !s.trim().isEmpty();
	}

	private static String zeros(String s, int tamanho) {
		StringBuilder sb = new StringBuilder();
		for (int i = s.length(); i < tamanho; i++) {
			sb.append('0');
		}
		return sb.append(s).toString();
	}

	/** Remove caracteres de controle (exceto quebra de linha) e espaços extras. */
	private static String limpar(String s) {
		if (s == null) {
			return "";
		}
		String t = Normalizer.normalize(s, Normalizer.Form.NFC).replace("\r", "");
		t = t.replaceAll("[\\x00-\\x09\\x0B-\\x1F\\x7F]", " ");
		return t.trim();
	}

	private static String limitar(String s, int max) {
		return s.length() > max ? s.substring(0, max) : s;
	}

	private static class RespostaHttp {
		int status;
		byte[] corpo = new byte[0];

		String textoUtf8() {
			return new String(corpo, StandardCharsets.UTF_8);
		}
	}

	// =====================================================================
	// Tipos auxiliares
	// =====================================================================

	/** Dados necessários para montar a DPS. */
	public static class DadosDps {
		public int ambiente = 2;
		public Date dataHoraEmissao;
		public Date competencia;
		public String serie;
		public long numero;
		public String codigoMunicipioEmissor;

		public String cnpjPrestador;
		public String inscricaoMunicipalPrestador;
		public String telefonePrestador;
		public String emailPrestador;
		public int opcaoSimplesNacional = 3;
		public Integer regimeApuracaoSimplesNacional;
		public int regimeEspecialTributacao = 0;

		public String tomadorCpfCnpj;
		public String tomadorNome;
		public String tomadorCodigoMunicipio;
		public String tomadorCep;
		public String tomadorLogradouro;
		public String tomadorNumero;
		public String tomadorComplemento;
		public String tomadorBairro;
		public String tomadorTelefone;
		public String tomadorEmail;

		public String codigoMunicipioPrestacao;
		public String codigoTributacaoNacional;
		public String codigoTributacaoMunicipal;
		public String codigoNbs;
		public String descricaoServico;
		public String informacoesComplementares;

		public Double valorServico;
		public int tributacaoIssqn = 1;
		public int retencaoIssqn = 1;
		public Double aliquotaIss;
		public Double percentualTributosSimples;

		/** ME/EPP com tributos federais e municipal apurados pelo Simples. */
		boolean issPeloSimples() {
			return opcaoSimplesNacional == 3 && regimeApuracaoSimplesNacional != null
					&& regimeApuracaoSimplesNacional == 1;
		}

		/** E0625/E0631: ISSQN pelo Simples e sem retenção não pode levar alíquota. */
		boolean aliquotaProibida() {
			return issPeloSimples() && retencaoIssqn == 1;
		}

		/** Só envia endereço quando estiver completo (todos os campos obrigatórios). */
		public boolean temEnderecoTomador() {
			return ibge(tomadorCodigoMunicipio) && somenteDigitos(tomadorCep).length() == 8
					&& temTexto(tomadorLogradouro) && temTexto(tomadorNumero) && temTexto(tomadorBairro);
		}
	}

	/** Resultado do registro/consulta de um evento (ex.: cancelamento). */
	public static class RetornoEvento {
		public boolean sucesso;
		public int httpStatus;
		public String xmlEvento;
		public List<String> mensagens = new ArrayList<>();
		public List<String> alertas = new ArrayList<>();

		public String mensagensTexto() {
			return String.join("\n", mensagens);
		}
	}

	/** Resultado de uma emissão ou consulta. */
	public static class RetornoNfse {
		public boolean sucesso;
		public boolean naoEncontrada;
		public int httpStatus;
		public String chaveAcesso;
		public String idDps;
		public String numeroNfse;
		public String xmlNfse;
		public List<String> mensagens = new ArrayList<>();
		public List<String> alertas = new ArrayList<>();

		public String mensagensTexto() {
			return String.join("\n", mensagens);
		}
	}

	public static class NfseException extends Exception {
		private static final long serialVersionUID = 1L;

		public NfseException(String msg) {
			super(msg);
		}

		public NfseException(String msg, Throwable causa) {
			super(msg, causa);
		}
	}

	/** Erro de rede/timeout: não se sabe se a nota foi gerada. */
	public static class NfseFalhaComunicacaoException extends NfseException {
		private static final long serialVersionUID = 1L;

		public NfseFalhaComunicacaoException(String msg, Throwable causa) {
			super(msg, causa);
		}
	}
}
