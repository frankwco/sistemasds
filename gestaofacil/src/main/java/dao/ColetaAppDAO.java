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
import base.modelo.ColetaAppPontosSemColeta;
import base.modelo.Estado;
import base.modelo.InformacaoFinanceira;
import base.modelo.ItensInformacaoFinanceiraPontoColeta;
import base.modelo.PontoColeta;
import util.FormataDataMB;
import util.Formatacoes;

public class ColetaAppDAO implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;
	
	//TEM QUE PEGAR A VIGENCIA DO CONTRATO, SÓ TRAZER DOS CONTRATOS VIGENTES....
	public List<ColetaAppPontosSemColeta> diasSemColetaPonto() {
		String dataFormatada = Formatacoes.formatarInvertido(new Date());
		String queryString = "SELECT "
				   + "gestaofacil.pessoa.`nomeRazaoSocial`, "
					+"gestaofacil.pessoa.`nomeFantasia`, "
				   + "gestaofacil.pessoa.`endereco`, "
				   + "gestaofacil.pessoa.`numero`, "
				    +"gestaofacil.pessoa.`bairro`, "
				    + "gestaofacil.cidade.id AS idCidade, "
				   + "gestaofacil.cidade.nome AS nomeCidade, "
				   + "gestaofacil.estado.sigla, "
				   + "gestaofacil.ponto_coleta.dataUltimaColeta, "
				    +"pessoa.id, "
				   +"DATEDIFF('"+dataFormatada+"', gestaofacil.ponto_coleta.dataUltimaColeta) AS diasSemColeta "
				   
				+"FROM gestaofacil.ponto_coleta INNER JOIN gestaofacil.pessoa  ON  gestaofacil.ponto_coleta.id = gestaofacil.pessoa.id "
				+"INNER JOIN gestaofacil.cidade ON gestaofacil.pessoa.`cidade_id` = gestaofacil.cidade.id "
				+"INNER JOIN gestaofacil.estado ON gestaofacil.cidade.estado_id=gestaofacil.estado.id "
				                       +"WHERE DATEDIFF('"+dataFormatada+"', gestaofacil.ponto_coleta.dataUltimaColeta)>10 "
				+"and ponto_coleta.status is true "
				+ "and (SELECT count(informacoes_financeiras.id) FROM informacoes_financeiras inner JOIN itens_informacoes_financeiras_ponto_coleta on informacoes_financeiras.id=itens_informacoes_financeiras_ponto_coleta.informacoesFinanceiras_id where itens_informacoes_financeiras_ponto_coleta.pontoColeta_id=ponto_coleta.id and situacaoContrato = 'Ativo' and informacoes_financeiras.status is true)>0 "

			+"ORDER BY diasSemColeta desc";
		
		List<ColetaAppPontosSemColeta> retorno = new ArrayList<>();
		Query query = null;
		try {
			query = manager.createNativeQuery(queryString, ColetaAppPontosSemColeta.class);
			retorno = (List<ColetaAppPontosSemColeta>) query.getResultList();			
			//query = manager.createNativeQuery(queryString);
			//retorno = (List<ColetaAppPontosSemColeta>)  query.getResultList();	
//			System.out.println("RE");
//			System.out.println(retorno.get(0).getNomeCidade());

		} catch (Exception e) {
			System.out.println("ERRO");
			e.printStackTrace();
		}
		//System.out.println(retorno.get(0));
		
		return retorno;
	}

	public List<ColetaAppPontosSemColeta> buscarInformacaoFinanceiraNomeData(String dataInicial, String dataFinal) {
		System.out.println(dataInicial +" - "+dataFinal);
		String queryString = "SELECT "
				   + "gestaofacil.pessoa.`nomeRazaoSocial`, "
					+"gestaofacil.pessoa.`nomeFantasia`, "
				   + "gestaofacil.pessoa.`endereco`, "
				   + "gestaofacil.pessoa.`numero`, "
				    +"gestaofacil.pessoa.`bairro`, "
				   + "gestaofacil.cidade.nome, "
				   + "gestaofacil.estado.sigla, "
				    +"pessoa.id, "
				    +"informacoes_financeiras.vigente, "
				    +"coleta_app.dataUltimaColeta "
				+"FROM gestaofacil.ponto_coleta INNER JOIN gestaofacil.pessoa  ON  gestaofacil.ponto_coleta.id = "
				+"gestaofacil.pessoa.id INNER JOIN gestaofacil.cidade ON gestaofacil.pessoa.`cidade_id` = gestaofacil.cidade.id "
				+"INNER JOIN gestaofacil.estado ON gestaofacil.cidade.estado_id=gestaofacil.estado.id "
				+"INNER JOIN itens_informacoes_financeiras_ponto_coleta on "
				+"itens_informacoes_financeiras_ponto_coleta.pontoColeta_id = ponto_coleta.id "
				+"INNER JOIN informacoes_financeiras on informacoes_financeiras.id = "
				+"itens_informacoes_financeiras_ponto_coleta.informacoesFinanceiras_id WHERE not exists (SELECT *FROM coleta_app "
				                       +"WHERE coleta_app.dataUltimaColeta BETWEEN  '"+dataInicial+"'  AND  '"+dataFinal+"'  "
				                       +"and coleta_app.pontoColetaSelecionado_id = ponto_coleta.id and coleta_app.status is true) "
				+"and ponto_coleta.status is true and informacoes_financeiras.vigente is true GROUP BY ponto_coleta.id  "
			+"ORDER BY `informacoes_financeiras`.`vigente`  DESC";
		
		List<ColetaAppPontosSemColeta> retorno = new ArrayList<>();
		Query query = null;
		try {
			query = manager.createNativeQuery(queryString, ColetaAppPontosSemColeta.class);
			retorno = (List<ColetaAppPontosSemColeta>) query.getResultList();			

		} catch (Exception e) {
			System.out.println("ERRO");
			e.printStackTrace();
		}
		//System.out.println(retorno.get(0));
		
		return retorno;
	}

	
}
