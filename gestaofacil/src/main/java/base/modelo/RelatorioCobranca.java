package base.modelo;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Um lote de importação do relatório de vencidos do banco (upload semanal do módulo de cobrança).
 */
@Entity
@Table(name = "relatorio_cobranca")
public class RelatorioCobranca implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataImportacao = new Date();

	/** Data de geração do relatório lida da linha "Gerado em ..." do próprio CSV do banco — não
	 * confundir com dataImportacao, que é quando alguém subiu o arquivo no sistema (pode ser dias depois). */
	@Temporal(TemporalType.TIMESTAMP)
	private Date dataCriacaoArquivo;

	private String nomeArquivoOriginal;
	private String usuario;

	private Integer totalLinhas = 0;
	private Integer totalConfirmados = 0;
	private Integer totalPendentes = 0;
	private Integer totalLinhasIlegiveis = 0;
	private Double valorTotal = 0.;

	private Boolean status = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDataImportacao() {
		return dataImportacao;
	}

	public void setDataImportacao(Date dataImportacao) {
		this.dataImportacao = dataImportacao;
	}

	public String getDataImportacaoFormatada() {
		if (dataImportacao == null) {
			return "";
		}
		return new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(dataImportacao);
	}

	public Date getDataCriacaoArquivo() {
		return dataCriacaoArquivo;
	}

	public void setDataCriacaoArquivo(Date dataCriacaoArquivo) {
		this.dataCriacaoArquivo = dataCriacaoArquivo;
	}

	public String getDataCriacaoArquivoFormatada() {
		if (dataCriacaoArquivo == null) {
			return "";
		}
		return new SimpleDateFormat("dd/MM/yyyy").format(dataCriacaoArquivo);
	}

	public String getNomeArquivoOriginal() {
		return nomeArquivoOriginal;
	}

	public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
		this.nomeArquivoOriginal = nomeArquivoOriginal;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public Integer getTotalLinhas() {
		return totalLinhas;
	}

	public void setTotalLinhas(Integer totalLinhas) {
		this.totalLinhas = totalLinhas;
	}

	public Integer getTotalConfirmados() {
		return totalConfirmados;
	}

	public void setTotalConfirmados(Integer totalConfirmados) {
		this.totalConfirmados = totalConfirmados;
	}

	public Integer getTotalPendentes() {
		return totalPendentes;
	}

	public void setTotalPendentes(Integer totalPendentes) {
		this.totalPendentes = totalPendentes;
	}

	public Integer getTotalLinhasIlegiveis() {
		return totalLinhasIlegiveis;
	}

	public void setTotalLinhasIlegiveis(Integer totalLinhasIlegiveis) {
		this.totalLinhasIlegiveis = totalLinhasIlegiveis;
	}

	public Double getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(Double valorTotal) {
		this.valorTotal = valorTotal;
	}

	public String getValorTotalFormatado() {
		NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
		return formato.format(valorTotal != null ? valorTotal : 0.0);
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

}
