package base.modelo;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name = "fechamento_mes")
public class FechamentoMes implements Serializable {

	public FechamentoMes() {
		super();
	}

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Integer mes;
	private Integer ano;
	private Date dataFechamento;
	private Double valorTotalPorQuilo = 0.;
	private Double valorFinal = 0.;
	private Boolean emitidoContasReceber = false;
	private Integer quantidadePontos;
	private Double diferencaGerarContasPagar = 0.;
	private Boolean status = true;
	@ManyToOne
	private InformacaoFinanceira informacaoFinanceira;

	private Double valorFixoTodosPontos = 0.;
	private Double valorExcedenteTodosPontos = 0.;
	private Double pesoExcedenteTodosPontos = 0.;
	private Long idInformacaoFinanceiraUnico;

	private Double valorFixoMensalContratoPonto = 0.;//
	private Double valorFixoMensalCobradoParte = 0.;//
	private Double valorExcedenteGruposContrato = 0.;//
	private Double valorExcedenteGruposCobradosParte = 0.;//
	private Double valorFaturar = 0.;
	private Double pesoExcedenteGruposContrato = 0.;//
	private Double pesoExcedenteCobradoParte = 0.;//
	private Double pesoColetadoGruposContrato = 0.;//
	private Double pesoColetadoGrupoParte = 0.;//
	private Double valorPagoFaturamento = 0.;

	private Boolean faturado = false;

	private String observacao = "";
	private Boolean fechamentoAnteriorAberto = false;
	private Double valorDesconto = 0.;

	@Transient
	private String fechamentosAbertos;

	@Temporal(TemporalType.DATE)
	private Date dataInicialFechamento;

	@Temporal(TemporalType.DATE)
	private Date dataFinalFechamento;

	private Boolean certificadoAvulso;

	public Boolean getCertificadoAvulso() {
		return certificadoAvulso;
	}

	public void setCertificadoAvulso(Boolean certificadoAvulso) {
		this.certificadoAvulso = certificadoAvulso;
	}

	public Date getDataInicialFechamento() {
		return dataInicialFechamento;
	}

	public void setDataInicialFechamento(Date dataInicialFechamento) {
		this.dataInicialFechamento = dataInicialFechamento;
	}

	public Date getDataFinalFechamento() {
		return dataFinalFechamento;
	}

	public void setDataFinalFechamento(Date dataFinalFechamento) {
		this.dataFinalFechamento = dataFinalFechamento;
	}

	public String getFechamentosAbertos() {
		return fechamentosAbertos;
	}

	public void setFechamentosAbertos(String fechamentosAbertos) {
		this.fechamentosAbertos = fechamentosAbertos;
	}

	public Double getValorDesconto() {
		if (valorDesconto == null) {
			valorDesconto = 0.;
		}
		return valorDesconto;
	}

	public void setValorDesconto(Double valorDesconto) {
		this.valorDesconto = valorDesconto;
	}

	public Boolean getFechamentoAnteriorAberto() {
		return fechamentoAnteriorAberto;
	}

	public void setFechamentoAnteriorAberto(Boolean fechamentoAnteriorAberto) {
		this.fechamentoAnteriorAberto = fechamentoAnteriorAberto;
	}

	public String getObservacao() {
		return observacao;
	}

	public void setObservacao(String observacao) {
		this.observacao = observacao;
	}

	public Double getValorPagoFaturamento() {
		return valorPagoFaturamento;
	}

	public void setValorPagoFaturamento(Double valorPagoFaturamento) {
		this.valorPagoFaturamento = valorPagoFaturamento;
	}

	public Double getValorFaturar() {
		return valorFaturar;
	}

	public void setValorFaturar(Double valorFaturar) {
		this.valorFaturar = valorFaturar;
	}

	public Long getIdInformacaoFinanceiraUnico() {
		return idInformacaoFinanceiraUnico;
	}

	public void setIdInformacaoFinanceiraUnico(Long idInformacaoFinanceiraUnico) {
		this.idInformacaoFinanceiraUnico = idInformacaoFinanceiraUnico;
	}

	public Boolean getFaturado() {
		if (faturado == null) {
			faturado = false;
		}
		return faturado;
	}

	public void setFaturado(Boolean faturado) {
		this.faturado = faturado;
	}

	public Double getValorExcedenteTodosPontos() {
		return valorExcedenteTodosPontos;
	}

	public void setValorExcedenteTodosPontos(Double valorExcedenteTodosPontos) {
		this.valorExcedenteTodosPontos = valorExcedenteTodosPontos;
	}

	public Double getPesoExcedenteTodosPontos() {
		return pesoExcedenteTodosPontos;
	}

	public void setPesoExcedenteTodosPontos(Double pesoExcedenteTodosPontos) {
		this.pesoExcedenteTodosPontos = pesoExcedenteTodosPontos;
	}

	public Double getValorFixoTodosPontos() {
		return valorFixoTodosPontos;
	}

	public void setValorFixoTodosPontos(Double valorFixoTodosPontos) {
		this.valorFixoTodosPontos = valorFixoTodosPontos;
	}

	public InformacaoFinanceira getInformacaoFinanceira() {
		return informacaoFinanceira;
	}

	public void setInformacaoFinanceira(InformacaoFinanceira informacaoFinanceira) {
		this.informacaoFinanceira = informacaoFinanceira;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getMes() {
	return mes;
	}
	
	public String getMesComZero() {
		if(String.valueOf(mes).length()==1) {
			return "0"+String.valueOf(mes);
		}
		return String.valueOf(mes);
	}
	
	public String getMesComZeroAno() {
		if(String.valueOf(mes).length()==1) {
			return "0"+String.valueOf(mes+"/"+ano);
		}
		return String.valueOf(mes+"/"+ano);
	}

	public void setMes(Integer mes) {
		this.mes = mes;
	}

	public Integer getAno() {
		return ano;
	}

	public void setAno(Integer ano) {
		this.ano = ano;
	}

	public Date getDataFechamento() {
		return dataFechamento;
	}

	public void setDataFechamento(Date dataFechamento) {
		this.dataFechamento = dataFechamento;
	}

	public Double getValorTotalPorQuilo() {
		return valorTotalPorQuilo;
	}

	public void setValorTotalPorQuilo(Double valorTotalPorQuilo) {
		this.valorTotalPorQuilo = valorTotalPorQuilo;
	}

	public Double getValorFinal() {
		return valorFinal;
	}

	public void setValorFinal(Double valorFinal) {
		this.valorFinal = valorFinal;
	}

	public Boolean getEmitidoContasReceber() {
		return emitidoContasReceber;
	}

	public void setEmitidoContasReceber(Boolean emitidoContasReceber) {
		this.emitidoContasReceber = emitidoContasReceber;
	}

	public Integer getQuantidadePontos() {
		return quantidadePontos;
	}

	public void setQuantidadePontos(Integer quantidadePontos) {
		this.quantidadePontos = quantidadePontos;
	}

	public Double getDiferencaGerarContasPagar() {
		return diferencaGerarContasPagar;
	}

	public void setDiferencaGerarContasPagar(Double diferencaGerarContasPagar) {
		this.diferencaGerarContasPagar = diferencaGerarContasPagar;
	}

	public Double getValorFixoMensalContratoPonto() {
		return valorFixoMensalContratoPonto;
	}

	public void setValorFixoMensalContratoPonto(Double valorFixoMensalContratoPonto) {
		this.valorFixoMensalContratoPonto = valorFixoMensalContratoPonto;
	}

	public Double getValorFixoMensalCobradoParte() {
		return valorFixoMensalCobradoParte;
	}

	public void setValorFixoMensalCobradoParte(Double valorFixoMensalCobradoParte) {
		this.valorFixoMensalCobradoParte = valorFixoMensalCobradoParte;
	}

	public Double getValorExcedenteGruposContrato() {
		return valorExcedenteGruposContrato;
	}

	public void setValorExcedenteGruposContrato(Double valorExcedenteGruposContrato) {
		this.valorExcedenteGruposContrato = valorExcedenteGruposContrato;
	}

	public Double getValorExcedenteGruposCobradosParte() {
		return valorExcedenteGruposCobradosParte;
	}

	public void setValorExcedenteGruposCobradosParte(Double valorExcedenteGruposCobradosParte) {
		this.valorExcedenteGruposCobradosParte = valorExcedenteGruposCobradosParte;
	}

	public Double getPesoExcedenteGruposContrato() {
		return pesoExcedenteGruposContrato;
	}

	public void setPesoExcedenteGruposContrato(Double pesoExcedenteGruposContrato) {
		this.pesoExcedenteGruposContrato = pesoExcedenteGruposContrato;
	}

	public Double getPesoExcedenteCobradoParte() {
		return pesoExcedenteCobradoParte;
	}

	public void setPesoExcedenteCobradoParte(Double pesoExcedenteCobradoParte) {
		this.pesoExcedenteCobradoParte = pesoExcedenteCobradoParte;
	}

	public Double getPesoColetadoGruposContrato() {
		return pesoColetadoGruposContrato;
	}

	public void setPesoColetadoGruposContrato(Double pesoColetadoGruposContrato) {
		this.pesoColetadoGruposContrato = pesoColetadoGruposContrato;
	}

	public Double getPesoColetadoGrupoParte() {
		return pesoColetadoGrupoParte;
	}

	public void setPesoColetadoGrupoParte(Double pesoColetadoGrupoParte) {
		this.pesoColetadoGrupoParte = pesoColetadoGrupoParte;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ano == null) ? 0 : ano.hashCode());
		result = prime * result + ((dataFechamento == null) ? 0 : dataFechamento.hashCode());
		result = prime * result + ((dataFinalFechamento == null) ? 0 : dataFinalFechamento.hashCode());
		result = prime * result + ((dataInicialFechamento == null) ? 0 : dataInicialFechamento.hashCode());
		result = prime * result + ((diferencaGerarContasPagar == null) ? 0 : diferencaGerarContasPagar.hashCode());
		result = prime * result + ((emitidoContasReceber == null) ? 0 : emitidoContasReceber.hashCode());
		result = prime * result + ((faturado == null) ? 0 : faturado.hashCode());
		result = prime * result + ((fechamentoAnteriorAberto == null) ? 0 : fechamentoAnteriorAberto.hashCode());
		result = prime * result + ((fechamentosAbertos == null) ? 0 : fechamentosAbertos.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((idInformacaoFinanceiraUnico == null) ? 0 : idInformacaoFinanceiraUnico.hashCode());
		result = prime * result + ((informacaoFinanceira == null) ? 0 : informacaoFinanceira.hashCode());
		result = prime * result + ((mes == null) ? 0 : mes.hashCode());
		result = prime * result + ((observacao == null) ? 0 : observacao.hashCode());
		result = prime * result + ((pesoColetadoGrupoParte == null) ? 0 : pesoColetadoGrupoParte.hashCode());
		result = prime * result + ((pesoColetadoGruposContrato == null) ? 0 : pesoColetadoGruposContrato.hashCode());
		result = prime * result + ((pesoExcedenteCobradoParte == null) ? 0 : pesoExcedenteCobradoParte.hashCode());
		result = prime * result + ((pesoExcedenteGruposContrato == null) ? 0 : pesoExcedenteGruposContrato.hashCode());
		result = prime * result + ((pesoExcedenteTodosPontos == null) ? 0 : pesoExcedenteTodosPontos.hashCode());
		result = prime * result + ((quantidadePontos == null) ? 0 : quantidadePontos.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((valorDesconto == null) ? 0 : valorDesconto.hashCode());
		result = prime * result
				+ ((valorExcedenteGruposCobradosParte == null) ? 0 : valorExcedenteGruposCobradosParte.hashCode());
		result = prime * result
				+ ((valorExcedenteGruposContrato == null) ? 0 : valorExcedenteGruposContrato.hashCode());
		result = prime * result + ((valorExcedenteTodosPontos == null) ? 0 : valorExcedenteTodosPontos.hashCode());
		result = prime * result + ((valorFaturar == null) ? 0 : valorFaturar.hashCode());
		result = prime * result + ((valorFinal == null) ? 0 : valorFinal.hashCode());
		result = prime * result + ((valorFixoMensalCobradoParte == null) ? 0 : valorFixoMensalCobradoParte.hashCode());
		result = prime * result
				+ ((valorFixoMensalContratoPonto == null) ? 0 : valorFixoMensalContratoPonto.hashCode());
		result = prime * result + ((valorFixoTodosPontos == null) ? 0 : valorFixoTodosPontos.hashCode());
		result = prime * result + ((valorPagoFaturamento == null) ? 0 : valorPagoFaturamento.hashCode());
		result = prime * result + ((valorTotalPorQuilo == null) ? 0 : valorTotalPorQuilo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FechamentoMes other = (FechamentoMes) obj;
		if (ano == null) {
			if (other.ano != null)
				return false;
		} else if (!ano.equals(other.ano))
			return false;
		if (dataFechamento == null) {
			if (other.dataFechamento != null)
				return false;
		} else if (!dataFechamento.equals(other.dataFechamento))
			return false;
		if (dataFinalFechamento == null) {
			if (other.dataFinalFechamento != null)
				return false;
		} else if (!dataFinalFechamento.equals(other.dataFinalFechamento))
			return false;
		if (dataInicialFechamento == null) {
			if (other.dataInicialFechamento != null)
				return false;
		} else if (!dataInicialFechamento.equals(other.dataInicialFechamento))
			return false;
		if (diferencaGerarContasPagar == null) {
			if (other.diferencaGerarContasPagar != null)
				return false;
		} else if (!diferencaGerarContasPagar.equals(other.diferencaGerarContasPagar))
			return false;
		if (emitidoContasReceber == null) {
			if (other.emitidoContasReceber != null)
				return false;
		} else if (!emitidoContasReceber.equals(other.emitidoContasReceber))
			return false;
		if (faturado == null) {
			if (other.faturado != null)
				return false;
		} else if (!faturado.equals(other.faturado))
			return false;
		if (fechamentoAnteriorAberto == null) {
			if (other.fechamentoAnteriorAberto != null)
				return false;
		} else if (!fechamentoAnteriorAberto.equals(other.fechamentoAnteriorAberto))
			return false;
		if (fechamentosAbertos == null) {
			if (other.fechamentosAbertos != null)
				return false;
		} else if (!fechamentosAbertos.equals(other.fechamentosAbertos))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (idInformacaoFinanceiraUnico == null) {
			if (other.idInformacaoFinanceiraUnico != null)
				return false;
		} else if (!idInformacaoFinanceiraUnico.equals(other.idInformacaoFinanceiraUnico))
			return false;
		if (informacaoFinanceira == null) {
			if (other.informacaoFinanceira != null)
				return false;
		} else if (!informacaoFinanceira.equals(other.informacaoFinanceira))
			return false;
		if (mes == null) {
			if (other.mes != null)
				return false;
		} else if (!mes.equals(other.mes))
			return false;
		if (observacao == null) {
			if (other.observacao != null)
				return false;
		} else if (!observacao.equals(other.observacao))
			return false;
		if (pesoColetadoGrupoParte == null) {
			if (other.pesoColetadoGrupoParte != null)
				return false;
		} else if (!pesoColetadoGrupoParte.equals(other.pesoColetadoGrupoParte))
			return false;
		if (pesoColetadoGruposContrato == null) {
			if (other.pesoColetadoGruposContrato != null)
				return false;
		} else if (!pesoColetadoGruposContrato.equals(other.pesoColetadoGruposContrato))
			return false;
		if (pesoExcedenteCobradoParte == null) {
			if (other.pesoExcedenteCobradoParte != null)
				return false;
		} else if (!pesoExcedenteCobradoParte.equals(other.pesoExcedenteCobradoParte))
			return false;
		if (pesoExcedenteGruposContrato == null) {
			if (other.pesoExcedenteGruposContrato != null)
				return false;
		} else if (!pesoExcedenteGruposContrato.equals(other.pesoExcedenteGruposContrato))
			return false;
		if (pesoExcedenteTodosPontos == null) {
			if (other.pesoExcedenteTodosPontos != null)
				return false;
		} else if (!pesoExcedenteTodosPontos.equals(other.pesoExcedenteTodosPontos))
			return false;
		if (quantidadePontos == null) {
			if (other.quantidadePontos != null)
				return false;
		} else if (!quantidadePontos.equals(other.quantidadePontos))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (valorDesconto == null) {
			if (other.valorDesconto != null)
				return false;
		} else if (!valorDesconto.equals(other.valorDesconto))
			return false;
		if (valorExcedenteGruposCobradosParte == null) {
			if (other.valorExcedenteGruposCobradosParte != null)
				return false;
		} else if (!valorExcedenteGruposCobradosParte.equals(other.valorExcedenteGruposCobradosParte))
			return false;
		if (valorExcedenteGruposContrato == null) {
			if (other.valorExcedenteGruposContrato != null)
				return false;
		} else if (!valorExcedenteGruposContrato.equals(other.valorExcedenteGruposContrato))
			return false;
		if (valorExcedenteTodosPontos == null) {
			if (other.valorExcedenteTodosPontos != null)
				return false;
		} else if (!valorExcedenteTodosPontos.equals(other.valorExcedenteTodosPontos))
			return false;
		if (valorFaturar == null) {
			if (other.valorFaturar != null)
				return false;
		} else if (!valorFaturar.equals(other.valorFaturar))
			return false;
		if (valorFinal == null) {
			if (other.valorFinal != null)
				return false;
		} else if (!valorFinal.equals(other.valorFinal))
			return false;
		if (valorFixoMensalCobradoParte == null) {
			if (other.valorFixoMensalCobradoParte != null)
				return false;
		} else if (!valorFixoMensalCobradoParte.equals(other.valorFixoMensalCobradoParte))
			return false;
		if (valorFixoMensalContratoPonto == null) {
			if (other.valorFixoMensalContratoPonto != null)
				return false;
		} else if (!valorFixoMensalContratoPonto.equals(other.valorFixoMensalContratoPonto))
			return false;
		if (valorFixoTodosPontos == null) {
			if (other.valorFixoTodosPontos != null)
				return false;
		} else if (!valorFixoTodosPontos.equals(other.valorFixoTodosPontos))
			return false;
		if (valorPagoFaturamento == null) {
			if (other.valorPagoFaturamento != null)
				return false;
		} else if (!valorPagoFaturamento.equals(other.valorPagoFaturamento))
			return false;
		if (valorTotalPorQuilo == null) {
			if (other.valorTotalPorQuilo != null)
				return false;
		} else if (!valorTotalPorQuilo.equals(other.valorTotalPorQuilo))
			return false;
		return true;
	}

}
