package dao;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import base.modelo.Cidade;
import base.modelo.Cliente;
import base.modelo.Estado;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.PontoColeta;
import util.Formatacoes;

public class InformacaoFinaceiraDAO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	public List<InformacaoFinanceira> buscarInformacaoFinanceiraNomeData(String condicao) {
		List<InformacaoFinanceira> retorno = new ArrayList<>();
		Query query = null;
		try {
			query = manager.createNativeQuery(
					"SELECT pessoa.nomeRazaoSocial, informacoes_financeiras.dataFinal FROM `informacoes_financeiras` INNER JOIN cliente on cliente.id=informacoes_financeiras.cliente_id INNER JOIN pessoa on pessoa.id = cliente.id WHERE informacoes_financeiras.status is true and "
							+ condicao);
			List<Object[]> authors = query.getResultList();

			for (Object[] a : authors) {
				InformacaoFinanceira f = new InformacaoFinanceira();
				Cliente c = new Cliente();
				c.setNomeRazaoSocial(String.valueOf(a[0].toString()));
				f.setCliente(c);

				f.setDataFinal((Date) a[1]);
				retorno.add(f);

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return retorno;
	}

	public List<InformacaoFinanceira> buscarInformacaoFinanceiraClientePontos(String tex) {
		List<InformacaoFinanceira> retorno = new ArrayList<>();
		Query query = null;
		try {
			query = manager.createNativeQuery("SELECT " + "informacoes_financeiras.id, " + "pessoa.nomeRazaoSocial, "
					+ "informacoes_financeiras.situacaoContrato, "
					+ "informacoes_financeiras.dataFinal FROM `informacoes_financeiras` INNER JOIN cliente on cliente.id=informacoes_financeiras.cliente_id INNER JOIN pessoa on pessoa.id = cliente.id WHERE informacoes_financeiras.status is true and vigente is true order by dataFinal asc");
			List<Object[]> authors = query.getResultList();

			for (Object[] a : authors) {

				InformacaoFinanceira f = new InformacaoFinanceira();
				f.setId(Long.parseLong(a[0].toString()) );
				f.setSituacaoContrato((String) a[2]);
				f.setDataFinal((Date) a[3]);
				Cliente c = new Cliente();
				c.setNomeRazaoSocial(String.valueOf(a[1].toString()));
				f.setCliente(c);

				query = manager.createNativeQuery("SELECT pessoa.nomeRazaoSocial, pessoa.nomeFantasia, pessoa.apelido, pessoa.endereco, pessoa.numero, cidade.nome, estado.sigla FROM `itens_informacoes_financeiras_ponto_coleta` INNER JOIN ponto_coleta on ponto_coleta.id=itens_informacoes_financeiras_ponto_coleta.pontoColeta_id INNER JOIN pessoa on pessoa.id=ponto_coleta.id INNER JOIN cidade on cidade.id=pessoa.cidade_id INNER JOIN estado ON estado.id=cidade.estado_id WHERE itens_informacoes_financeiras_ponto_coleta.status is true and itens_informacoes_financeiras_ponto_coleta.informacoesFinanceiras_id="+ f.getId());

				List<Object[]> pontos = query.getResultList();
				List<ItensInformacaoFinanceiraPontoColeta> listaItensInformacoesFinanceirasPontoColeta = new ArrayList<>();
				for (Object[] ponto : pontos) {
					PontoColeta p = new PontoColeta();
					p.setNomeRazaoSocial((String) ponto[0]);
					p.setNomeFantasia((String) ponto[1]);
					p.setApelido((String) ponto[2]);
					p.setEndereco((String) ponto[3]);
					p.setNumero((String) ponto[4]);
					Cidade cid = new Cidade();
					cid.setNome((String) ponto[5]);
					Estado est = new Estado();
					est.setSigla((String) ponto[6]);
					cid.setEstado(est);
					p.setCidade(cid);
					ItensInformacaoFinanceiraPontoColeta itensI = new ItensInformacaoFinanceiraPontoColeta();
					itensI.setPontoColeta(p);
					itensI.setInformacoesFinanceiras(f);
					listaItensInformacoesFinanceirasPontoColeta.add(itensI);
				}
				//System.out.println(listaItensInformacoesFinanceirasPontoColeta.size());
				f.setListaItensInformacoesFinanceirasPontoColeta(listaItensInformacoesFinanceirasPontoColeta);
				retorno.add(f);

			}

		} catch (

		Exception e) {
			e.printStackTrace();
		}
		return retorno;
	}
}
