package util.cobranca;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o relatório "Vencido" exportado do internet banking do Sicoob (sicoobnet).
 *
 * O arquivo não é um CSV bem formado: cada linha de título vem inteira como um único campo
 * entre aspas (o Excel do banco escapa as aspas internas dobrando-as), com colunas separadas
 * por vírgulas vazias no meio. Por isso usamos uma expressão regular específica em vez de um
 * parser de CSV genérico. Também repete cabeçalho/rodapé a cada página do relatório — essas
 * linhas simplesmente não casam com o padrão e são ignoradas.
 */
public class ParserRelatorioCobranca {

	// Exportações do sicoobnet costumam vir em Windows-1252 (não UTF-8).
	private static final Charset CHARSET_RELATORIO = Charset.forName("windows-1252");

	private static final Pattern LINHA_DADO = Pattern.compile(
			"^\"?(?<nome>.+?),,,,,(?<nosso>\\d{5,7}-\\d{1,2}),,,(?<seu>[^,]*),,,"
					+ "(?<entrada>\\d{2}/\\d{2}/\\d{4}),,?(?<venc>\\d{2}/\\d{2}/\\d{4}),,,,,?"
					+ "\"\"(?<valor>[\\d.,]+)\"\",?\"?;?$");

	private static final Pattern PARECE_LINHA_DE_DADO = Pattern.compile("^\"?[A-Za-zÀ-ÿ]");

	// Captura a data (e hora, se vier) da linha "Gerado em ..." que o sicoobnet imprime no
	// cabeçalho/rodapé do relatório — é a data real de geração do arquivo pelo banco, não a
	// data em que alguém baixou/importou o CSV no sistema.
	private static final Pattern DATA_GERACAO = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})(?:[^\\d]+(\\d{2}:\\d{2}))?");

	public static ResultadoParseRelatorioCobranca parsear(InputStream inputStream) throws IOException {
		ResultadoParseRelatorioCobranca resultado = new ResultadoParseRelatorioCobranca();

		SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy");
		formatoData.setLenient(false);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET_RELATORIO))) {
			String linha;
			while ((linha = reader.readLine()) != null) {
				linha = linha.trim();
				if (linha.isEmpty()) {
					continue;
				}
				resultado.setTotalLinhasArquivo(resultado.getTotalLinhasArquivo() + 1);

				if (resultado.getDataGeracaoArquivo() == null && linha.startsWith("Gerado em")) {
					resultado.setDataGeracaoArquivo(extrairDataGeracao(linha));
				}

				Matcher m = LINHA_DADO.matcher(linha);
				if (m.matches()) {
					LinhaRelatorioCobranca l = new LinhaRelatorioCobranca();
					l.setNome(m.group("nome").trim());
					l.setSeuNumero(m.group("seu").trim());
					l.setNossoNumero(m.group("nosso").trim());
					try {
						l.setDataVencimento(formatoData.parse(m.group("venc")));
					} catch (ParseException e) {
						resultado.setTotalLinhasIlegiveis(resultado.getTotalLinhasIlegiveis() + 1);
						continue;
					}
					l.setValor(converterValor(m.group("valor")));
					resultado.getLinhas().add(l);
				} else if (PARECE_LINHA_DE_DADO.matcher(linha).find() && !ehCabecalhoOuRodape(linha)) {
					resultado.setTotalLinhasIlegiveis(resultado.getTotalLinhasIlegiveis() + 1);
				}
			}
		}

		return resultado;
	}

	private static boolean ehCabecalhoOuRodape(String linha) {
		return linha.startsWith("Sacado") || linha.startsWith("Cedente") || linha.startsWith("Tipo Consulta")
				|| linha.startsWith("Conta Corrente") || linha.startsWith("Ordenado por")
				|| linha.startsWith("Gerado em") || linha.contains("Relatório - Títulos por Período")
				|| linha.contains("Total de Registros") || linha.contains("Total de Valores");
	}

	private static java.util.Date extrairDataGeracao(String linha) {
		Matcher m = DATA_GERACAO.matcher(linha);
		if (!m.find()) {
			return null;
		}
		String data = m.group(1);
		String hora = m.group(2);
		try {
			SimpleDateFormat formato = new SimpleDateFormat(hora != null ? "dd/MM/yyyy HH:mm" : "dd/MM/yyyy");
			formato.setLenient(false);
			return formato.parse(hora != null ? data + " " + hora : data);
		} catch (ParseException e) {
			return null;
		}
	}

	private static Double converterValor(String valorTexto) {
		try {
			String normalizado = valorTexto.replace(".", "").replace(",", ".");
			return Double.parseDouble(normalizado);
		} catch (Exception e) {
			return 0.0;
		}
	}

}
