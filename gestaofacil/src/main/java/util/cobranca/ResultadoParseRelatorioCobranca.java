package util.cobranca;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResultadoParseRelatorioCobranca {

	private List<LinhaRelatorioCobranca> linhas = new ArrayList<>();
	private int totalLinhasArquivo = 0;
	private int totalLinhasIlegiveis = 0;
	private Date dataGeracaoArquivo;

	public List<LinhaRelatorioCobranca> getLinhas() {
		return linhas;
	}

	public int getTotalLinhasArquivo() {
		return totalLinhasArquivo;
	}

	public void setTotalLinhasArquivo(int totalLinhasArquivo) {
		this.totalLinhasArquivo = totalLinhasArquivo;
	}

	public int getTotalLinhasIlegiveis() {
		return totalLinhasIlegiveis;
	}

	public void setTotalLinhasIlegiveis(int totalLinhasIlegiveis) {
		this.totalLinhasIlegiveis = totalLinhasIlegiveis;
	}

	/** Data de geração do relatório, lida da linha "Gerado em ..." do próprio arquivo — null se não foi encontrada. */
	public Date getDataGeracaoArquivo() {
		return dataGeracaoArquivo;
	}

	public void setDataGeracaoArquivo(Date dataGeracaoArquivo) {
		this.dataGeracaoArquivo = dataGeracaoArquivo;
	}

}
