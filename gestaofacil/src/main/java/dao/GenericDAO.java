package dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;

public class GenericDAO<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	public void alterar(T objeto) {
		manager.merge(objeto);
	}

	public void remover(Class<T> classe, Long i) {
		Object o =  manager.find(classe, i);		
		manager.remove(o);		
	}

	public void inserir(T objeto) {
		manager.persist(objeto);
	}

	public T buscarPorId(Class<T> classe, Long i) {
		return manager.find(classe, i);
	}

//	public T buscarCondicao(Class classeEntidade, String condicao) {
//		return (T) manager.createQuery("from "+classeEntidade.getSimpleName()+" where status is true and "+condicao).getSingleResult();
//	}
	
	public List<T> listaComStatus(Class classeEntidade, String orderBy, Integer limit) {
		return manager
				.createQuery("from " + classeEntidade.getSimpleName() + " where status is true order by " + orderBy).setMaxResults(limit)
				.getResultList();
	}

	public List<T> listaComStatus(Class classeEntidade) {
		return manager.createQuery("from " + classeEntidade.getSimpleName() + " where status is true order by id")
				.getResultList();
	}

	public List<T> listaComStatus(Class classeEntidade, String orderBy) {
		return manager
				.createQuery("from " + classeEntidade.getSimpleName() + " where status is true order by " + orderBy)
				.getResultList();
	}

	public List<T> listar(Class classeEntidade, String condicao) {
		Query query = null;
		try {
			query = manager
					.createQuery("from " + classeEntidade.getSimpleName() + " where status is true and " + condicao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query.getResultList();
	}

	public List<T> listarPorCampo(Class classeEntidade, String campo, Object valor) {
		Query query = manager.createQuery(
				"from " + classeEntidade.getSimpleName() + " where status is true and " + campo + " = :valor");
		query.setParameter("valor", valor);
		return query.getResultList();
	}
	
	
	public List backup(String tabela, String condicao) {		
		return manager.createNativeQuery("SELECT * FROM " + tabela+" "+condicao).getResultList();
	}

//	public List<T> listarCodicaoLivre(Class classeEntidade, String condicao) {
//		Query query = null;
//		try {
//			query = manager.createQuery("from " + classeEntidade.getSimpleName() + " where " + condicao);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return query.getResultList();
//	}
	
	

	public List<T> listaPontosColetaAtivos(Class classeEntidade, String parametro) {
		// return manager.createQuery("from PontoColeta as pc inner join
		// ItensInformacaoFinanceiraPontoColeta as iif on pc.id=iif.pontoColeta.id inner
		// join InformacaoFinanceira as if on if.id=iif.informacoesFinanceiras.id where
		// status is true and if.situacaoContrato<>'Inativo'").getResultList();
		/*if (parametro.length() > 3) {
			return manager.createQuery(
					"select pc from ItensInformacaoFinanceiraPontoColeta as iif inner join iif.pontoColeta as pc inner join iif.informacoesFinanceiras as if where  if.status is true and pc.status is true and if.situacaoContrato='Ativo' and "+parametro+" group by iif.pontoColeta order by iif.pontoColeta.dataUltimaColeta asc")
					.getResultList();
		} else {
			return manager.createQuery(
					"select pc from ItensInformacaoFinanceiraPontoColeta as iif inner join iif.pontoColeta as pc inner join iif.informacoesFinanceiras as if where if.status is true and pc.status is true and if.situacaoContrato='Ativo' group by iif.pontoColeta order by iif.pontoColeta.dataUltimaColeta asc")
					.getResultList();

		}*/
		
		if (parametro.length() > 3) {
		    return manager.createQuery(
		        "select distinct pc " +
		        "from ItensInformacaoFinanceiraPontoColeta iif " +
		        "  join iif.pontoColeta pc " +
		        "  join iif.informacoesFinanceiras iff " +
		        "where iff.status = true " +
		        "  and pc.status = true " +
		        "  and iff.situacaoContrato = 'Ativo' " +
		        "  and " + parametro + " " +
		        "order by " +
		        "  case when pc.periodicidadeColeta = 'Coleta Única' then 0 else 1 end, " +
		        "  case when pc.primeiraColeta = true then 0 else 1 end, " +
		        "  case when length(coalesce(pc.aviso, '')) > 2 then 0 else 1 end, " +		        
		        "  pc.dataUltimaColeta desc"
		    ).getResultList();
		} else {
		    return manager.createQuery(
		        "select distinct pc " +
		        "from ItensInformacaoFinanceiraPontoColeta iif " +
		        "  join iif.pontoColeta pc " +
		        "  join iif.informacoesFinanceiras iff " +
		        "where iff.status = true " +
		        "  and pc.status = true " +
		        "  and iff.situacaoContrato = 'Ativo' " +
		        "order by " +
		        "  case when pc.periodicidadeColeta = 'Coleta Única' then 0 else 1 end, " +
		        "  case when pc.primeiraColeta = true then 0 else 1 end, " +
		        "  case when length(coalesce(pc.aviso, '')) > 2 then 0 else 1 end, " +		        
		        "  pc.dataUltimaColeta desc"
		    ).getResultList();
		}

		
	}
	
	public List<T> listaPontosColetaAtivosReduzidos(Class classeEntidade, String parametro) {
	
		if (parametro.length() > 3) {
			return manager.createQuery(
					"select pc.id, pc.nomeRazaoSocial, pc.nomeFantasia, pc.apelido, pc.cidade.id, pc.cidade.nome from ItensInformacaoFinanceiraPontoColeta as iif inner join iif.pontoColeta as pc inner join iif.informacoesFinanceiras as if where pc.status is true and if.situacaoContrato='Ativo' and "+parametro+" group by iif.pontoColeta")
					.getResultList();
		} else {
			return manager.createQuery(
					"select pc.id, pc.nomeRazaoSocial, pc.nomeFantasia, pc.apelido, pc.cidade.id, pc.cidade.nome from ItensInformacaoFinanceiraPontoColeta as iif inner join iif.pontoColeta as pc inner join iif.informacoesFinanceiras as if where pc.status is true and if.situacaoContrato='Ativo' group by iif.pontoColeta")
					.getResultList();

		}
	}


	public List listarCadastros(Class classe, String condicao) {
		Query query = null;
		try {
			query = manager
					.createQuery("from " + classe.getSimpleName() + " where " + condicao + " and status is true");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query.getResultList();
	}

	public List listarCadastrosSemStatus(Class classe, String condicao) {
		Query query = null;
		try {
			query = manager.createQuery("from " + classe.getSimpleName() + " where " + condicao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return query.getResultList();
	}

//	
//	
//	
//	public void update(String alteracao) {
//		String sql = "";
//		
//			sql = ("update " + alteracao);
//			int update = manager.createQuery(sql).executeUpdate();
//			
//		
//	}

	public void updateSenha(String senha, String email) {
		String sql = "";

		sql = ("update Usuario set senha = '" + senha + "' where email like '" + email + "'");
		int update = manager.createQuery(sql).executeUpdate();

	}
	
	public void updateSenhaEmail2(String senha, String email) {
		String sql = "";

		sql = ("update Usuario set senha2 = '" + senha + "' where email2 like '" + email + "'");
		int update = manager.createQuery(sql).executeUpdate();

	}

	public void updateSenhaCliente(String senha, String email) {
		String sql = "";

		sql = ("update Cliente set senha = '" + senha + "' where email like '" + email + "'");
		int update = manager.createQuery(sql).executeUpdate();

	}
	
	public void updateSenhaClienteEmail2(String senha, String email) {
		String sql = "";

		sql = ("update Cliente set senha2 = '" + senha + "' where email2 like '" + email + "'");
		int update = manager.createQuery(sql).executeUpdate();

	}

	public void updateSenhaAdminCliente(String senhaHash, Date validade, Long id) {
		manager.createQuery("update Cliente set senhaAdmin = :senha, senhaAdminValidade = :validade where id = :id")
				.setParameter("senha", senhaHash).setParameter("validade", validade, TemporalType.TIMESTAMP)
				.setParameter("id", id).executeUpdate();
	}

	public void revogarSenhaAdminCliente(Long id) {
		manager.createQuery("update Cliente set senhaAdmin = null, senhaAdminValidade = null where id = :id")
				.setParameter("id", id).executeUpdate();
	}

}
