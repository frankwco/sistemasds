package base.modelo;

import java.io.Serializable;
import java.util.Date;

/**
 * Projeção plana (sem hidratar entidades JPA) usada na tela de Excedentes em Aberto,
 * pra evitar o N+1 causado pelas associações @ManyToOne/@OneToMany EAGER de
 * InformacaoFinanceira e Pessoa (pontoColetaGerarContrato, contrato,
 * listaItensInformacoesFinanceirasPontoColeta, cidade, cidadeResponsavel).
 */
public class ExcedenteAbertoDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long idInformacaoFinanceiraUnico;
	private String clienteNome;
	private String clienteTelefone;
	private Integer mes;
	private Integer ano;
	private Double valorFaturar;
	private String situacaoContrato;
	private Date dataInicial;
	private Date dataFinal;

	public ExcedenteAbertoDTO(Long idInformacaoFinanceiraUnico, String clienteNome, String clienteTelefone,
			Integer mes, Integer ano, Double valorFaturar, String situacaoContrato, Date dataInicial,
			Date dataFinal) {
		this.idInformacaoFinanceiraUnico = idInformacaoFinanceiraUnico;
		this.clienteNome = clienteNome;
		this.clienteTelefone = clienteTelefone;
		this.mes = mes;
		this.ano = ano;
		this.valorFaturar = valorFaturar;
		this.situacaoContrato = situacaoContrato;
		this.dataInicial = dataInicial;
		this.dataFinal = dataFinal;
	}

	public Long getIdInformacaoFinanceiraUnico() {
		return idInformacaoFinanceiraUnico;
	}

	public String getClienteNome() {
		return clienteNome;
	}

	public String getClienteTelefone() {
		return clienteTelefone;
	}

	public Integer getMes() {
		return mes;
	}

	public Integer getAno() {
		return ano;
	}

	public Double getValorFaturar() {
		return valorFaturar;
	}

	public String getSituacaoContrato() {
		return situacaoContrato;
	}

	public Date getDataInicial() {
		return dataInicial;
	}

	public Date getDataFinal() {
		return dataFinal;
	}

}
