package util.cobranca;

import java.util.List;

import base.modelo.Cliente;
import base.modelo.RelatorioCobrancaItem;

/**
 * Um canal de disparo de cobrança (email, e futuramente WhatsApp).
 * A tela de disparo não precisa mudar quando um novo canal for adicionado.
 *
 * O envio é por cliente (um aviso só, listando todos os títulos em aberto dele),
 * não um disparo por título.
 */
public interface CanalCobranca {

	/** Identificador salvo em RelatorioCobrancaItem.canalCobranca, ex: "EMAIL". */
	String getNome();

	/** @return true se o disparo foi enviado (ou enfileirado) com sucesso. */
	boolean enviar(Cliente cliente, List<RelatorioCobrancaItem> itensEmAberto);

}
