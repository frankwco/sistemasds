package base.service;

import java.io.Serializable;

import javax.inject.Inject;

import base.modelo.CategoriaDespesa;
import base.modelo.Fornecedor;
import base.modelo.LancamentoVeiculo;
import dao.GenericDAO;
import util.Transacional;

public class FornecedorService implements Serializable {

	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<Fornecedor> dao;

	@Transacional
	public void inserirAlterar(Fornecedor fornecedor) {
		if (fornecedor.getId() == null) {
			dao.inserir(fornecedor);
		} else {
			dao.alterar(fornecedor);
		}
	}

	/**
	 * Cadastra um fornecedor a partir só de um nome (sem CNPJ/telefone/etc.) —
	 * usado quando a Captura por IA (admin ou app) lê um fornecedor num
	 * comprovante que não bate com nenhum já cadastrado. Compartilhado pelos
	 * dois lados (antes só existia em {@code CapturaIaMB}) pra não duplicar a
	 * lógica de inferir o tipo.
	 *
	 * Precisa do próprio @Transacional aqui: como o método chama
	 * {@link #inserirAlterar(Fornecedor)} internamente (via "this"), essa
	 * chamada não passa pelo proxy do CDI e o interceptor de
	 * {@link Transacional} não entraria em ação — sem isso, o insert nunca é
	 * commitado, e o {@link LancamentoVeiculo} que referencia esse fornecedor
	 * "fantasma" (transiente, sem id) quebra ao salvar de verdade.
	 */
	@Transacional
	public Fornecedor cadastrarAutomatico(String nome, String tipoLancamento, CategoriaDespesa categoriaDespesa) {
		Fornecedor fornecedor = new Fornecedor();
		fornecedor.setNome(nome);
		fornecedor.setTipo(inferirTipo(tipoLancamento, categoriaDespesa));
		fornecedor.setStatus(true);
		inserirAlterar(fornecedor);
		return fornecedor;
	}

	/**
	 * Cadastro explícito, com o tipo escolhido por quem está cadastrando —
	 * diferente de {@link #cadastrarAutomatico}, que só tem um nome pra
	 * trabalhar e precisa chutar o tipo. Usado quando o motorista, pelo app,
	 * digita um fornecedor/posto que ainda não existe.
	 */
	@Transacional
	public Fornecedor cadastrar(String nome, String tipo) {
		Fornecedor fornecedor = new Fornecedor();
		fornecedor.setNome(nome);
		fornecedor.setTipo(tipo != null && !tipo.trim().isEmpty() ? tipo : "Oficina");
		fornecedor.setStatus(true);
		inserirAlterar(fornecedor);
		return fornecedor;
	}

	/**
	 * "Posto" pra abastecimento, "Seguradora" se a categoria de despesa tiver
	 * "seguro" no nome, e "Oficina" como padrão pros demais casos — não tem
	 * como perguntar pro usuário o tipo nesse ponto do fluxo (a IA só
	 * extraiu um nome), então é um chute razoável, corrigível depois na tela
	 * de Fornecedores.
	 */
	private String inferirTipo(String tipoLancamento, CategoriaDespesa categoriaDespesa) {
		if ("Abastecimento".equals(tipoLancamento)) {
			return "Posto";
		}
		if (categoriaDespesa != null && categoriaDespesa.getNome() != null
				&& categoriaDespesa.getNome().toLowerCase().contains("segur")) {
			return "Seguradora";
		}
		return "Oficina";
	}

}
