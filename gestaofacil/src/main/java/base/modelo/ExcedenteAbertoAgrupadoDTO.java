package base.modelo;

import java.io.Serializable;
import java.util.Date;

import util.Formatacoes;

/**
 * Um contrato (idInformacaoFinanceiraUnico) com todos os meses de excedente em aberto
 * agrupados numa única linha — o detalhamento mês/ano:valor fica empilhado em uma célula,
 * no mesmo formato usado na coluna "Excedente/Fat." de fechamentoMes.xhtml.
 */
public class ExcedenteAbertoAgrupadoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long idInformacaoFinanceiraUnico;
	private String clienteNome;
	private String clienteTelefone;
	private String situacaoContrato;
	private Date dataInicial;
	private Date dataFinal;
	private Double valorTotal = 0.;
	private Integer quantidadeMeses = 0;
	private StringBuilder linhasDetalhe = new StringBuilder();

	public void adicionarItem(Integer mes, Integer ano, Double valor) {
		if (valor == null) {
			valor = 0.;
		}
		linhasDetalhe.append("<tr><td>").append(mes).append("/").append(ano)
				.append("</td><td style=\"text-align:right\">")
				.append(Formatacoes.formatarNumeroDuasCasasDecimais(valor)).append("</td></tr>");
		valorTotal += valor;
		quantidadeMeses++;
	}

	public String getDetalheHtml() {
		StringBuilder html = new StringBuilder("<table style=\"width:100%; border: 1px solid gray;\">");
		html.append(linhasDetalhe);
		html.append("<tr><td><b>Total:</b></td><td style=\"text-align:right\">")
				.append(Formatacoes.formatarNumeroDuasCasasDecimais(valorTotal)).append("</td></tr>");
		html.append("</table>");
		return html.toString();
	}

	public Long getIdInformacaoFinanceiraUnico() {
		return idInformacaoFinanceiraUnico;
	}

	public void setIdInformacaoFinanceiraUnico(Long idInformacaoFinanceiraUnico) {
		this.idInformacaoFinanceiraUnico = idInformacaoFinanceiraUnico;
	}

	public String getClienteNome() {
		return clienteNome;
	}

	public void setClienteNome(String clienteNome) {
		this.clienteNome = clienteNome;
	}

	public String getClienteTelefone() {
		return clienteTelefone;
	}

	public void setClienteTelefone(String clienteTelefone) {
		this.clienteTelefone = clienteTelefone;
	}

	public String getSituacaoContrato() {
		return situacaoContrato;
	}

	public void setSituacaoContrato(String situacaoContrato) {
		this.situacaoContrato = situacaoContrato;
	}

	public Date getDataInicial() {
		return dataInicial;
	}

	public void setDataInicial(Date dataInicial) {
		this.dataInicial = dataInicial;
	}

	public Date getDataFinal() {
		return dataFinal;
	}

	public void setDataFinal(Date dataFinal) {
		this.dataFinal = dataFinal;
	}

	public Double getValorTotal() {
		return valorTotal;
	}

	public Integer getQuantidadeMeses() {
		return quantidadeMeses;
	}

}
