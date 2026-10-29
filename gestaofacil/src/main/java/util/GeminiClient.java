package util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import base.modelo.InterpretacaoIADTO;

/**
 * Chama a API do Gemini pra extrair os campos de uma despesa de veículo, a
 * partir de uma foto de nota fiscal (RF12, multimodal — manda a imagem) ou
 * de um texto já transcrito por reconhecimento de voz do navegador (RF11 —
 * manda só texto, bem mais rápido que mandar áudio pra IA entender: sem
 * processamento de mídia, a resposta costuma sair em 1-3s em vez de dezenas
 * de segundos). {@link #interpretarAudio} continua existindo pra quem
 * quiser mandar áudio bruto (ex.: app mobile, se não tiver reconhecimento
 * de voz nativo disponível). Não usa SDK — é só um POST com
 * {@code HttpURLConnection} (projeto é Java 8, sem `java.net.http`) e Gson
 * (já é dependência do projeto), pra não introduzir biblioteca nova só por
 * causa disso.
 *
 * A chave nunca fica no código nem em nada versionado no git — {@link
 * #obterApiKey()} tenta, nesta ordem: (1) {@code -DgeminiApiKey=...} na JVM
 * do Tomcat (mesmo padrão de {@code -DappApiKey} em
 * {@link services.ChaveApiFilter}), (2) variável de ambiente
 * {@code GEMINI_API_KEY}, (3) um arquivo de propriedades FORA da pasta do
 * projeto, em {@code ~/.gestaofacil/secrets.properties} (chave
 * {@code geminiApiKey=...}) — esse arquivo nunca é commitado porque nem
 * está dentro do repositório.
 */
public class GeminiClient {

	private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

	// a API do Gemini falha esporadicamente (erro 5xx ou timeout), principalmente
	// com áudio/imagem — 1 nova tentativa costuma resolver sem punir demais o
	// usuário com espera
	private static final int MAX_TENTATIVAS = 2;
	private static final long ESPERA_ENTRE_TENTATIVAS_MS = 2000;

	private GeminiClient() {
	}

	public static InterpretacaoIADTO interpretarAudio(byte[] audio, String mimeType) throws Exception {
		return interpretar(audio, mimeType, "um áudio gravado por um motorista descrevendo uma despesa do veículo");
	}

	public static InterpretacaoIADTO interpretarFoto(byte[] imagem, String mimeType) throws Exception {
		return interpretar(imagem, mimeType, "uma foto de uma nota fiscal ou comprovante de despesa de veículo");
	}

	/**
	 * Foto ou PDF (nota fiscal, cupom, anotação ou documento) complementado,
	 * opcionalmente, por um texto falado/digitado pelo motorista — por
	 * exemplo, quando a imagem não é nítida o bastante pra IA extrair tudo
	 * sozinha, ou quando o motorista quer acrescentar algo que não está no
	 * anexo (ex.: "isso foi manutenção corretiva"). O Gemini entende PDF
	 * nativamente via {@code inline_data} igual uma imagem, então o mesmo
	 * método serve pros dois formatos — só muda o {@code mimeType}. Sem texto
	 * complementar, funciona igual a {@link #interpretarFoto}.
	 */
	public static InterpretacaoIADTO interpretarFotoComTexto(byte[] imagem, String mimeType, String textoComplementar)
			throws Exception {
		String descricaoOrigem = "uma foto ou um PDF de uma nota fiscal, cupom, anotação ou documento de despesa de "
				+ "veículo";
		if (textoComplementar != null && !textoComplementar.trim().isEmpty()) {
			descricaoOrigem += ", complementada pelo seguinte texto falado ou digitado pelo motorista (pode ter "
					+ "pequenos erros de reconhecimento, se foi por voz): \"" + textoComplementar.trim() + "\". Se a "
					+ "foto e o texto tiverem informações conflitantes sobre o mesmo campo, priorize o texto, que é "
					+ "mais explícito";
		}
		return interpretar(imagem, mimeType, descricaoOrigem);
	}

	/**
	 * Só texto (já transcrito pelo reconhecimento de voz do navegador) — sem
	 * inline_data nenhum, então não tem processamento de mídia do lado do
	 * Gemini, o que torna a resposta muito mais rápida que mandar áudio.
	 */
	public static InterpretacaoIADTO interpretarTexto(String texto) throws Exception {
		String apiKey = obterApiKey();
		if (apiKey == null) {
			throw new IllegalStateException("Chave da API do Gemini não configurada. Configure via -DgeminiApiKey=..., "
					+ "variável de ambiente GEMINI_API_KEY, ou crie ~/.gestaofacil/secrets.properties com geminiApiKey=...");
		}

		String corpo = montarCorpoRequisicaoTexto(texto);
		String respostaBruta = chamarApi(corpo, apiKey);
		String textoModelo = extrairTextoDaResposta(respostaBruta);

		return new Gson().fromJson(textoModelo, InterpretacaoIADTO.class);
	}

	private static String obterApiKey() {
		String daJvm = System.getProperty("geminiApiKey");
		if (daJvm != null && !daJvm.trim().isEmpty()) {
			return daJvm.trim();
		}

		String doAmbiente = System.getenv("GEMINI_API_KEY");
		if (doAmbiente != null && !doAmbiente.trim().isEmpty()) {
			return doAmbiente.trim();
		}

		Path arquivoSecrets = Paths.get(System.getProperty("user.home"), ".gestaofacil", "secrets.properties");
		if (Files.isRegularFile(arquivoSecrets)) {
			try (InputStream entrada = new FileInputStream(arquivoSecrets.toFile())) {
				Properties propriedades = new Properties();
				propriedades.load(entrada);
				String doArquivo = propriedades.getProperty("geminiApiKey");
				if (doArquivo != null && !doArquivo.trim().isEmpty()) {
					return doArquivo.trim();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private static InterpretacaoIADTO interpretar(byte[] midia, String mimeType, String descricaoOrigem)
			throws Exception {
		String apiKey = obterApiKey();
		if (apiKey == null) {
			throw new IllegalStateException("Chave da API do Gemini não configurada. Configure via -DgeminiApiKey=..., "
					+ "variável de ambiente GEMINI_API_KEY, ou crie ~/.gestaofacil/secrets.properties com geminiApiKey=...");
		}

		String corpo = montarCorpoRequisicaoMidia(midia, mimeType, descricaoOrigem);
		String respostaBruta = chamarApi(corpo, apiKey);
		String textoModelo = extrairTextoDaResposta(respostaBruta);

		InterpretacaoIADTO resultado = new Gson().fromJson(textoModelo, InterpretacaoIADTO.class);
		if (resultado.getTextoBruto() == null) {
			// garante que sempre sobra algo pra auditoria (RF15), mesmo se o
			// modelo não preencher o campo textoBruto no JSON de resposta
			resultado.setTextoBruto(textoModelo);
		}
		return resultado;
	}

	private static String montarCorpoRequisicaoMidia(byte[] midia, String mimeType, String descricaoOrigem) {
		String prompt = montarPrompt(descricaoOrigem);
		String base64 = Base64.getEncoder().encodeToString(midia);

		JsonObject partTexto = new JsonObject();
		partTexto.addProperty("text", prompt);

		JsonObject inlineData = new JsonObject();
		inlineData.addProperty("mime_type", mimeType);
		inlineData.addProperty("data", base64);
		JsonObject partMidia = new JsonObject();
		partMidia.add("inline_data", inlineData);

		JsonArray parts = new JsonArray();
		parts.add(partTexto);
		parts.add(partMidia);

		return montarCorpo(parts);
	}

	private static String montarCorpoRequisicaoTexto(String texto) {
		String descricaoOrigem = "a transcrição de um áudio gravado por um motorista descrevendo uma despesa do "
				+ "veículo (transcrita ao vivo pelo reconhecimento de voz do navegador). O texto transcrito, que "
				+ "pode ter pequenos erros de reconhecimento, é:\n\"" + texto + "\"";
		String prompt = montarPrompt(descricaoOrigem);

		JsonObject partTexto = new JsonObject();
		partTexto.addProperty("text", prompt);

		JsonArray parts = new JsonArray();
		parts.add(partTexto);

		return montarCorpo(parts);
	}

	private static String montarCorpo(JsonArray parts) {
		JsonObject conteudo = new JsonObject();
		conteudo.add("parts", parts);
		JsonArray conteudos = new JsonArray();
		conteudos.add(conteudo);

		JsonObject generationConfig = new JsonObject();
		generationConfig.addProperty("response_mime_type", "application/json");
		generationConfig.addProperty("temperature", 0.1);

		JsonObject corpo = new JsonObject();
		corpo.add("contents", conteudos);
		corpo.add("generationConfig", generationConfig);

		return corpo.toString();
	}

	private static String montarPrompt(String origem) {
		return "Você é um assistente que extrai dados estruturados de despesas de frota de veículos a partir de "
				+ origem + ". Categorias de despesa possíveis: Combustível, Manutenção Preventiva, Manutenção "
				+ "Corretiva, Seguro, Multa, Pedágio, IPVA, Lavagem, Insumos de Biossegurança, Outros. "
				+ "Responda SOMENTE com um JSON válido (sem markdown, sem texto antes ou depois) no formato:\n"
				+ "{\n"
				+ "  \"tipo\": \"Abastecimento\" | \"Despesa\" | \"Serviço\",\n"
				+ "  \"veiculo\": string ou null (placa, parte da placa, ou apelido/modelo do veículo, se algum for "
				+ "mencionado — ex.: \"BCW\", \"a Sprinter branca\", \"carro 3\"),\n"
				+ "  \"motorista\": string ou null (nome do motorista, se mencionado — pode ser só o primeiro nome),\n"
				+ "  \"categoria\": string ou null,\n"
				+ "  \"valor\": number ou null (valor total em reais — se não for dito mas der pra calcular de "
				+ "litros x valor/litro, calcule),\n"
				+ "  \"litros\": number ou null (só se for abastecimento),\n"
				+ "  \"valorLitro\": number ou null (só se for abastecimento — se não for dito mas der pra calcular "
				+ "de valor total / litros, calcule),\n"
				+ "  \"km\": number ou null (km do odômetro, se mencionado/visível),\n"
				+ "  \"combustivel\": string ou null — use EXATAMENTE uma destas opções, a mais parecida com o que "
				+ "foi dito: \"Diesel S10\", \"Diesel S500\", \"Gasolina\", \"Etanol\", \"GNV\", \"Arla 32\", "
				+ "\"Outro\" (se o tipo não for especificado, prefira \"Diesel S10\" por ser o mais comum em frotas),\n"
				+ "  \"fornecedor\": string ou null (nome do posto/oficina/loja),\n"
				+ "  \"numeroDocumento\": string ou null (número do cupom fiscal, nota fiscal ou nota de serviço, "
				+ "se estiver visível/mencionado — não confundir com CNPJ, placa ou valor),\n"
				+ "  \"observacao\": string ou null (algo relevante que não caiba nos outros campos),\n"
				+ "  \"confianca\": number de 0 a 1 (o quanto você tem certeza dos valores extraídos),\n"
				+ "  \"textoBruto\": string (a transcrição do áudio, ou o texto lido na imagem, na íntegra)\n"
				+ "}\n"
				+ "Se não conseguir identificar algum campo com segurança, retorne null nele — não invente valores.";
	}

	private static String chamarApi(String corpoJson, String apiKey) throws IOException {
		IOException ultimaFalha = null;
		for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
			try {
				return chamarApiUmaVez(corpoJson, apiKey);
			} catch (IOException e) {
				ultimaFalha = e;
				boolean vaiTentarDeNovo = tentativa < MAX_TENTATIVAS;
				System.err.println("Falha ao chamar a API do Gemini (tentativa " + tentativa + "/" + MAX_TENTATIVAS
						+ (vaiTentarDeNovo ? ", tentando de novo" : ", desistindo") + "): " + e.getMessage());
				if (vaiTentarDeNovo) {
					try {
						Thread.sleep(ESPERA_ENTRE_TENTATIVAS_MS);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw ultimaFalha;
					}
				}
			}
		}
		throw ultimaFalha;
	}

	private static String chamarApiUmaVez(String corpoJson, String apiKey) throws IOException {
		URL url = new URL(ENDPOINT + "?key=" + apiKey);
		HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
		conexao.setRequestMethod("POST");
		conexao.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conexao.setDoOutput(true);
		conexao.setConnectTimeout(30000);
		// com retry (2 tentativas), preferimos falhar mais rápido e tentar de novo
		// a deixar o usuário esperando muito tempo numa única tentativa
		conexao.setReadTimeout(75000);

		try (OutputStream saida = conexao.getOutputStream()) {
			saida.write(corpoJson.getBytes(StandardCharsets.UTF_8));
		}

		int status = conexao.getResponseCode();
		InputStream entrada = status >= 200 && status < 300 ? conexao.getInputStream() : conexao.getErrorStream();
		String resposta = lerStream(entrada);

		if (status < 200 || status >= 300) {
			throw new IOException("Gemini API retornou status " + status + ": " + resposta);
		}
		return resposta;
	}

	private static String extrairTextoDaResposta(String respostaJson) {
		JsonObject raiz = new Gson().fromJson(respostaJson, JsonObject.class);
		JsonArray candidatos = raiz.getAsJsonArray("candidates");
		JsonObject primeiroCandidato = candidatos.get(0).getAsJsonObject();
		JsonObject conteudo = primeiroCandidato.getAsJsonObject("content");
		JsonArray parts = conteudo.getAsJsonArray("parts");
		return parts.get(0).getAsJsonObject().get("text").getAsString();
	}

	private static String lerStream(InputStream entrada) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] pedaco = new byte[4096];
		int lidos;
		while ((lidos = entrada.read(pedaco)) != -1) {
			buffer.write(pedaco, 0, lidos);
		}
		return buffer.toString("UTF-8");
	}

}
