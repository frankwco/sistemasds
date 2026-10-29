package base.controle;

import java.io.Serializable;
import java.util.List;

import base.modelo.InformacaoFinanceira;
import base.modelo.ItensInformacaoFinanceiraGrupo;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;

public class RenovacaoLoteItem implements Serializable {

	private static final long serialVersionUID = 1L;

	private InformacaoFinanceira contratoOriginal;
	private InformacaoFinanceira contratoRenovado;
	private List<ItensInformacaoFinanceiraGrupo> itensGrupo;
	private List<ItensInformacaoFinanceiraPontoColeta> itensPontoColeta;
	private Boolean salvo;
	private String mensagemErro;

	public InformacaoFinanceira getContratoOriginal() {
		return contratoOriginal;
	}

	public void setContratoOriginal(InformacaoFinanceira contratoOriginal) {
		this.contratoOriginal = contratoOriginal;
	}

	public InformacaoFinanceira getContratoRenovado() {
		return contratoRenovado;
	}

	public void setContratoRenovado(InformacaoFinanceira contratoRenovado) {
		this.contratoRenovado = contratoRenovado;
	}

	public List<ItensInformacaoFinanceiraGrupo> getItensGrupo() {
		return itensGrupo;
	}

	public void setItensGrupo(List<ItensInformacaoFinanceiraGrupo> itensGrupo) {
		this.itensGrupo = itensGrupo;
	}

	public List<ItensInformacaoFinanceiraPontoColeta> getItensPontoColeta() {
		return itensPontoColeta;
	}

	public void setItensPontoColeta(List<ItensInformacaoFinanceiraPontoColeta> itensPontoColeta) {
		this.itensPontoColeta = itensPontoColeta;
	}

	public Boolean getSalvo() {
		return salvo;
	}

	public void setSalvo(Boolean salvo) {
		this.salvo = salvo;
	}

	public String getMensagemErro() {
		return mensagemErro;
	}

	public void setMensagemErro(String mensagemErro) {
		this.mensagemErro = mensagemErro;
	}

	/**
	 * Expostos como boolean primitivo (em vez de comparar o Boolean salvo com
	 * null/true/false via EL) para evitar ambiguidade de coerção de nulos em
	 * implementações antigas de EL.
	 */
	public boolean isAguardando() {
		return salvo == null;
	}

	public boolean isSucesso() {
		return Boolean.TRUE.equals(salvo);
	}

	public boolean isFalha() {
		return Boolean.FALSE.equals(salvo);
	}

}
