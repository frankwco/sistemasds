package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import base.modelo.Cliente;
import base.modelo.RelatorioCobranca;
import base.modelo.RelatorioCobrancaItem;
import util.Transacional;
import util.cobranca.GrupoConfirmadoCobranca;
import util.cobranca.LinhaRelatorioCobranca;
import util.cobranca.NomeSimilaridade;

/**
 * Processa o relatório de vencidos do banco: cruza cada título com o cliente dono do contrato
 * (via código de contrato, extraído do "Seu Número") e mantém emDebito/historicoCobranca em dia.
 *
 * Regra de cruzamento (validada manualmente contra o banco de produção antes de implementar):
 *  1) Se o código de contrato só apareceu para 1 cliente na história do sistema -> confirma.
 *  2) Se apareceu pra mais de um (contrato antigo cancelado e código reaproveitado depois),
 *     mas só 1 desses clientes está com contrato Ativo e dentro da vigência hoje -> confirma esse.
 *  3) Caso contrário fica PENDENTE_ESCOLHA, com os candidatos guardados pra tela de revisão decidir.
 */
public class RelatorioCobrancaService implements Serializable {

	private static final long serialVersionUID = 1L;

	// Aceita tanto "código-mês" (191158-1) quanto "código/qualquercoisa" (3816/acerto-01,
	// 3799/3800-02) — os dois formatos existem no relatório do banco, mas o código de contrato
	// é sempre o número antes do primeiro "-" ou "/".
	private static final Pattern CODIGO_CONTRATO_NO_SEU_NUMERO = Pattern.compile("^(\\d+)[-/].+$");

	private static final List<String> STATUS_CONFIRMADOS = Arrays.asList("CONFIRMADO_AUTOMATICO", "CONFIRMADO_MANUAL");

	@Inject
	private EntityManager manager;

	@Transacional
	public RelatorioCobranca processarLote(RelatorioCobranca lote, List<LinhaRelatorioCobranca> linhas) {
		manager.persist(lote);

		Set<String> seuNumerosDoLote = new HashSet<>();
		Set<Long> clientesAfetados = new HashSet<>();
		int confirmados = 0;
		int pendentes = 0;
		double valorTotal = 0.;

		for (LinhaRelatorioCobranca linha : linhas) {
			seuNumerosDoLote.add(linha.getSeuNumero());

			// mesmo título já rastreado (em aberto) por uma importação anterior — não duplica,
			// só conta pro resumo deste lote (é normal um título vencido aparecer de novo semana após semana)
			RelatorioCobrancaItem existente = buscarItemAbertoPorSeuNumero(linha.getSeuNumero());
			if (existente != null) {
				if (existente.getCliente() != null) {
					clientesAfetados.add(existente.getCliente().getId());
				}
				if (STATUS_CONFIRMADOS.contains(existente.getStatusResolucao())) {
					confirmados++;
				} else {
					pendentes++;
				}
				valorTotal += linha.getValor() != null ? linha.getValor() : 0.;
				continue;
			}

			RelatorioCobrancaItem item = new RelatorioCobrancaItem();
			item.setRelatorioCobranca(lote);
			item.setNomeSacadoRelatorio(linha.getNome());
			item.setSeuNumero(linha.getSeuNumero());
			item.setNossoNumero(linha.getNossoNumero());
			item.setDataVencimento(linha.getDataVencimento());
			item.setValor(linha.getValor());

			resolverItem(item);
			manager.persist(item);

			if (item.getCliente() != null) {
				clientesAfetados.add(item.getCliente().getId());
			}
			if (STATUS_CONFIRMADOS.contains(item.getStatusResolucao())) {
				confirmados++;
			} else {
				pendentes++;
			}
			valorTotal += linha.getValor() != null ? linha.getValor() : 0.;
		}

		clientesAfetados.addAll(aplicarBaixaAutomatica(lote, seuNumerosDoLote));

		for (Long clienteId : clientesAfetados) {
			recomputarEmDebito(clienteId);
		}

		lote.setTotalLinhas(linhas.size());
		lote.setTotalConfirmados(confirmados);
		lote.setTotalPendentes(pendentes);
		lote.setValorTotal(valorTotal);
		manager.merge(lote);

		return lote;
	}

	private void resolverItem(RelatorioCobrancaItem item) {
		Integer codigo = extrairCodigoContrato(item.getSeuNumero());
		item.setCodigoContratoExtraido(codigo);

		if (codigo == null) {
			item.setStatusResolucao("PENDENTE_ESCOLHA");
			item.setCandidatosClienteIds("");
			return;
		}

		List<Long> todosOsClientes = buscarClientesPorCodigoContrato(codigo);

		if (todosOsClientes.size() == 1) {
			confirmarAutomatico(item, todosOsClientes.get(0));
			return;
		}
		if (todosOsClientes.isEmpty()) {
			item.setStatusResolucao("PENDENTE_ESCOLHA");
			item.setCandidatosClienteIds("");
			return;
		}

		List<Long> clientesAtivos = buscarClientesAtivosPorCodigoContrato(codigo);
		if (clientesAtivos.size() == 1) {
			confirmarAutomatico(item, clientesAtivos.get(0));
			return;
		}

		item.setStatusResolucao("PENDENTE_ESCOLHA");
		item.setCandidatosClienteIds(join(todosOsClientes));
	}

	private void confirmarAutomatico(RelatorioCobrancaItem item, Long clienteId) {
		Cliente cliente = manager.find(Cliente.class, clienteId);
		item.setCliente(cliente);
		item.setStatusResolucao("CONFIRMADO_AUTOMATICO");
		item.setNomeDivergente(!NomeSimilaridade.pareceMesmoNome(item.getNomeSacadoRelatorio(),
				cliente.getNomeRazaoSocial(), cliente.getNomeFantasia()));
	}

	private Integer extrairCodigoContrato(String seuNumero) {
		if (seuNumero == null) {
			return null;
		}
		Matcher m = CODIGO_CONTRATO_NO_SEU_NUMERO.matcher(seuNumero.trim());
		if (!m.matches()) {
			return null;
		}
		try {
			return Integer.valueOf(m.group(1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private RelatorioCobrancaItem buscarItemAbertoPorSeuNumero(String seuNumero) {
		List<RelatorioCobrancaItem> resultado = manager
				.createQuery("select i from RelatorioCobrancaItem i where i.seuNumero = :seu and i.quitado = false "
						+ "order by i.id desc", RelatorioCobrancaItem.class)
				.setParameter("seu", seuNumero).setMaxResults(1).getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	private List<Long> buscarClientesPorCodigoContrato(Integer codigo) {
		return manager
				.createQuery("select distinct f.cliente.id from InformacaoFinanceira f "
						+ "where f.codigoContratoAtual = :codigo and f.codigoContratoAtual > 0", Long.class)
				.setParameter("codigo", codigo).getResultList();
	}

	private List<Long> buscarClientesAtivosPorCodigoContrato(Integer codigo) {
		return manager.createQuery("select distinct f.cliente.id from InformacaoFinanceira f "
				+ "where f.codigoContratoAtual = :codigo and f.situacaoContrato = 'Ativo' and f.dataFinal >= :hoje",
				Long.class).setParameter("codigo", codigo).setParameter("hoje", new Date()).getResultList();
	}

	/** Fecha automaticamente os títulos que estavam em aberto e não aparecem mais no relatório novo. */
	private Set<Long> aplicarBaixaAutomatica(RelatorioCobranca loteAtual, Set<String> seuNumerosDoLoteAtual) {
		Set<Long> clientesAfetados = new HashSet<>();

		List<RelatorioCobrancaItem> abertosAnteriores = manager
				.createQuery("select i from RelatorioCobrancaItem i where i.relatorioCobranca <> :loteAtual "
						+ "and i.quitado = false", RelatorioCobrancaItem.class)
				.setParameter("loteAtual", loteAtual).getResultList();

		for (RelatorioCobrancaItem antigo : abertosAnteriores) {
			if (!seuNumerosDoLoteAtual.contains(antigo.getSeuNumero())) {
				antigo.setQuitado(true);
				antigo.setDataQuitacao(new Date());
				antigo.setFormaQuitacao("AUTOMATICA_SUMIU_RELATORIO");
				manager.merge(antigo);
				if (antigo.getCliente() != null) {
					clientesAfetados.add(antigo.getCliente().getId());
				}
			}
		}
		return clientesAfetados;
	}

	private void recomputarEmDebito(Long clienteId) {
		Long count = manager
				.createQuery("select count(i) from RelatorioCobrancaItem i where i.cliente.id = :cid "
						+ "and i.quitado = false and i.statusResolucao in ('CONFIRMADO_AUTOMATICO','CONFIRMADO_MANUAL')",
						Long.class)
				.setParameter("cid", clienteId).getSingleResult();

		Cliente c = manager.find(Cliente.class, clienteId);
		if (c != null) {
			c.setEmDebito(count > 0);
			manager.merge(c);
		}
	}

	@Transacional
	public void resolverManualmente(Long itemId, Long clienteId) {
		RelatorioCobrancaItem item = manager.find(RelatorioCobrancaItem.class, itemId);
		Cliente cliente = manager.find(Cliente.class, clienteId);
		item.setCliente(cliente);
		item.setStatusResolucao("CONFIRMADO_MANUAL");
		manager.merge(item);
		recomputarEmDebito(clienteId);
	}

	@Transacional
	public void ignorarItem(Long itemId) {
		RelatorioCobrancaItem item = manager.find(RelatorioCobrancaItem.class, itemId);
		item.setStatusResolucao("IGNORADO");
		manager.merge(item);
	}

	@Transacional
	public void marcarCobrancaEnviada(List<Long> itemIds, String canal) {
		for (Long itemId : itemIds) {
			RelatorioCobrancaItem item = manager.find(RelatorioCobrancaItem.class, itemId);
			item.setDataCobrancaEnviada(new Date());
			item.setCanalCobranca(canal);
			manager.merge(item);
		}
	}

	/** Último lote importado (para a tela continuar mostrando os dados ao ser reaberta). */
	public RelatorioCobranca buscarUltimoLote() {
		List<RelatorioCobranca> resultado = manager
				.createQuery("select l from RelatorioCobranca l order by l.dataImportacao desc", RelatorioCobranca.class)
				.setMaxResults(1).getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	/**
	 * Títulos em aberto por status, de qualquer importação — não só do lote mais recente.
	 * Um título deduplicado continua ligado ao lote que o criou primeiro, então filtrar só
	 * pelo lote atual deixaria a tela vazia sempre que uma reimportação não trouxer nada novo.
	 */
	public List<RelatorioCobrancaItem> buscarItensAbertosPorStatus(List<String> status) {
		return manager
				.createQuery("select i from RelatorioCobrancaItem i where i.quitado = false "
						+ "and i.statusResolucao in :status order by i.nomeSacadoRelatorio, i.dataVencimento",
						RelatorioCobrancaItem.class)
				.setParameter("status", status).getResultList();
	}

	/** Todos os títulos ainda em aberto de um cliente, de qualquer importação (para o badge/overlay). */
	public List<RelatorioCobrancaItem> buscarItensEmAberto(Long clienteId) {
		return manager
				.createQuery("select i from RelatorioCobrancaItem i where i.cliente.id = :cid "
						+ "and i.quitado = false and i.statusResolucao in ('CONFIRMADO_AUTOMATICO','CONFIRMADO_MANUAL') "
						+ "order by i.dataVencimento", RelatorioCobrancaItem.class)
				.setParameter("cid", clienteId).getResultList();
	}

	/** Um cliente pode ter mais de uma parcela vencida no mesmo lote — agrupa pra listar/cobrar uma vez só. */
	public List<GrupoConfirmadoCobranca> agruparPorCliente(List<RelatorioCobrancaItem> itens) {
		Map<Long, GrupoConfirmadoCobranca> porClienteId = new LinkedHashMap<>();
		for (RelatorioCobrancaItem item : itens) {
			if (item.getCliente() == null) {
				continue;
			}
			Long clienteId = item.getCliente().getId();
			GrupoConfirmadoCobranca grupo = porClienteId.get(clienteId);
			if (grupo == null) {
				grupo = new GrupoConfirmadoCobranca();
				grupo.setCliente(item.getCliente());
				porClienteId.put(clienteId, grupo);
			}
			grupo.adicionar(item);
		}
		List<GrupoConfirmadoCobranca> grupos = new ArrayList<>(porClienteId.values());
		grupos.sort(Comparator.comparing(g -> g.getCliente().getNomeRazaoSocial()));

		// título vencido que continua aparecendo semana após semana não é duplicado, então fica
		// preso ao lote que o criou primeiro — sem isso, a mensagem de cobrança citaria a data do
		// relatório antigo em vez da última importação (mesma data do "Resumo da importação").
		RelatorioCobranca loteAtual = buscarUltimoLote();
		for (GrupoConfirmadoCobranca grupo : grupos) {
			grupo.setLoteReferencia(loteAtual);
		}

		return grupos;
	}

	/** Detalhe dos clientes candidatos de um item ambíguo, pra montar o combo na tela de revisão. */
	public List<Cliente> buscarClientesPorIds(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return new ArrayList<>();
		}
		return manager.createQuery("select c from Cliente c where c.id in :ids", Cliente.class)
				.setParameter("ids", ids).getResultList();
	}

	/** Busca por nome/CNPJ para os casos sem candidato nenhum (formato de Seu Número não reconhecido). */
	public List<Cliente> buscarClientesPorNome(String texto) {
		if (texto == null || texto.trim().length() < 3) {
			return new ArrayList<>();
		}
		TypedQuery<Cliente> query = manager.createQuery(
				"select c from Cliente c where upper(c.nomeRazaoSocial) like :texto "
						+ "or upper(c.nomeFantasia) like :texto or c.cpfCnpj like :textoOriginal order by c.nomeRazaoSocial",
				Cliente.class);
		query.setParameter("texto", "%" + texto.trim().toUpperCase() + "%");
		query.setParameter("textoOriginal", "%" + texto.trim() + "%");
		query.setMaxResults(20);
		return query.getResultList();
	}

	public List<Long> parseIds(String candidatosClienteIds) {
		List<Long> ids = new ArrayList<>();
		if (candidatosClienteIds == null || candidatosClienteIds.trim().isEmpty()) {
			return ids;
		}
		for (String parte : candidatosClienteIds.split(",")) {
			try {
				ids.add(Long.valueOf(parte.trim()));
			} catch (NumberFormatException e) {
				// ignora token inválido
			}
		}
		return ids;
	}

	private String join(List<Long> ids) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(ids.get(i));
		}
		return sb.toString();
	}

}
