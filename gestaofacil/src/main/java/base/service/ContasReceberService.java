package base.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import base.modelo.ContasReceber;
import base.modelo.ContasReceberParcelas;
import dao.GenericDAO;
import util.ExibirMensagem;
import util.FecharDialog;
import util.FormataDataMB;
import util.Mensagem;
import util.NossoNumeroSicoob;
import util.Transacional;

public class ContasReceberService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Inject
	private GenericDAO<ContasReceber> dao;

	@Inject
	private GenericDAO<ContasReceberParcelas> daoContasReceberParcelas;

	@Inject
	private ContasReceberParcelasService contasReceberParcelasService;

	@Transacional
	public void inserirAlterar(ContasReceber tipo) {
		if (tipo.getId() == null) {
			dao.inserir(tipo);
		} else {
			dao.alterar(tipo);
		}
	}

	public void salvar(ContasReceber contasReceber, List<ContasReceberParcelas> listaParcelas) {
		try {
			if (contasReceber != null) {
				// contasReceber.setDataCadastro(new Date());
				// this.inserirAlterar(contasReceber);
				// System.out.println(contasReceber.getId());
				for (ContasReceberParcelas p : listaParcelas) {
					p.setContasReceber(contasReceber);
					// System.out.println(p.getContasReceber().getId());
					//System.out.println("DATA VE: "+p.getDataVencimento());
					contasReceberParcelasService.inserirAlterar(p);
				}
				// ExibirMensagem.exibirMensagem("Contas a Receber inserida com sucesso!");
			} else {
				// ExibirMensagem.exibirMensagem("Contas a Receber já está inserida!");
			}
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void baixarParcela(ContasReceberParcelas contasReceberParcelas) {
		if (contasReceberParcelas != null) {
			if (contasReceberParcelas.getId() != null) {
				contasReceberParcelasService.inserirAlterar(contasReceberParcelas);
				contasReceberParcelas = new ContasReceberParcelas();
				ExibirMensagem.exibirMensagem("Baixa efetuada com sucesso!");
				FecharDialog.fecharDialogContasReceberBaixa();
			} else {
				ExibirMensagem.exibirMensagem("Baixa efetuada com sucesso!");
				FecharDialog.fecharDialogContasReceberBaixa();
			}
		}
	}

	public void cancelarBaixarParcela(ContasReceberParcelas parc) {
		if (parc != null) {
			parc.setValorPago(null);
			parc.setDataPagamento(null);
			if (parc.getId() != null) {
				contasReceberParcelasService.inserirAlterar(parc);
				ExibirMensagem.exibirMensagem("Cancelamento de Baixa efetuada com sucesso!");

			} else {
				ExibirMensagem.exibirMensagem("Cancelamento de Baixa efetuada com sucesso!");
			}
		}
	}

	public List<ContasReceberParcelas> carregarContaReceber(ContasReceber contasReceber) {
		List<ContasReceberParcelas> listaParcelas = daoContasReceberParcelas.listar(ContasReceberParcelas.class,
				"contasReceber.id=" + contasReceber.getId());
		return listaParcelas;
	}

	public List<ContasReceberParcelas> gerarParcelas(ContasReceber contasReceber) {

		Calendar c = Calendar.getInstance();
		c.setTime(contasReceber.getDataPrimeiroVencimento());
		int mes = c.get(Calendar.MONTH);
		// c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH) + 10);
		// c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 1);
		// c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);

		List<ContasReceberParcelas> listaParcelas = new ArrayList<>();
		int mesP = contasReceber.getMesPrimeiraParcela();
		int ano = contasReceber.getAnoPrimeiraParcela();
		// int mesP =Integer.parseInt(new
		// FormataDataMB().formatarData(contasReceber.getDataPrimeiroVencimento(), "MM",
		// "0"));
		// int ano = Integer.parseInt(new
		// FormataDataMB().formatarData(contasReceber.getDataPrimeiroVencimento(),
		// "yyyy", "0"));

		// if(mesP==1) {
		// mesP=12;
		// ano = ano-1;
		// }

		for (int x = 0; x < contasReceber.getQuantidadeParcelas(); x++) {
			ContasReceberParcelas p = new ContasReceberParcelas();
			//quando cai no dia 31 e no próximo não tem 30 ou no caso de fevereiro com 28 dias....
			c.setTime(contasReceber.getDataPrimeiroVencimento());
			
			c.set(Calendar.MONTH, (mes + x));
			p.setReferenteMes(mesP);
			mesP++;
			p.setReferenteAno(ano);
			p.setNossoNumero(String.valueOf(contasReceber.getNossoNumero()));
			p.setDataVencimento(c.getTime());
			p.setParcela((x + 1));
			p.setQuantidadeParcelas(contasReceber.getQuantidadeParcelas());
			p.setValorParcela(contasReceber.getValorParcelas());
			p.setContasReceber(contasReceber);
			if (mesP > 12) {
				mesP = 1;
				ano++;
			}

			listaParcelas.add(p);
		}

		return listaParcelas;
	}

	public void excluirContasReceber(ContasReceber contasReceber) {

		if (contasReceber != null && contasReceber.getId() != null) {

			List<ContasReceberParcelas> listaParcelas = daoContasReceberParcelas.listar(ContasReceberParcelas.class,
					"contasReceber.id" + contasReceber.getId());

			for (ContasReceberParcelas p : listaParcelas) {
				p.setStatus(false);
				contasReceberParcelasService.inserirAlterar(p);
			}
			contasReceber.setStatus(false);
			this.inserirAlterar(contasReceber);
			ExibirMensagem.exibirMensagem("Conta a Receber excluída com sucesso!");
		} else {
			ExibirMensagem.exibirMensagem(
					"Ocorreu um erro ao excluir, tente pesquisar novamente a Conta a Receber que pretende excluir!");
		}
	}

}
