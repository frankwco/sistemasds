package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.CategoriaDespesa;
import base.modelo.Fornecedor;
import base.modelo.LancamentoVeiculo;
import base.modelo.Veiculo;
import base.service.LancamentoVeiculoService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

/**
 * Tela de Despesas e Manutenção — cobre RF06 (despesa genérica: seguro,
 * multa, pedágio, IPVA, lavagem, insumos de biossegurança, outros) e RF08
 * (serviço/manutenção) no mesmo formulário: a seção de manutenção
 * (oficina, preventiva/corretiva, entrada/saída, km da próxima revisão) só
 * aparece quando o campo transiente {@link #manutencao} é marcado, e define
 * se o {@link LancamentoVeiculo} salvo fica com tipo "Despesa" ou "Serviço".
 */
@ViewScoped
@Named("despesaManutencaoMB")
public class DespesaManutencaoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private LancamentoVeiculo lancamento;
	private List<LancamentoVeiculo> listLancamento;

	// não persistido — só controla se a seção de manutenção aparece no formulário
	private boolean manutencao;

	@Inject
	private GenericDAO<LancamentoVeiculo> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private GenericDAO<CategoriaDespesa> daoCategoriaDespesa;

	@Inject
	private GenericDAO<Fornecedor> daoFornecedor;

	@Inject
	private LancamentoVeiculoService lancamentoVeiculoService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		carregarLista();
	}

	public void preencherLista(LancamentoVeiculo t) {
		this.lancamento = t;
		this.manutencao = "Serviço".equals(t.getTipo());
	}

	public void inativar(LancamentoVeiculo t) {
		t.setStatus(false);
		lancamentoVeiculoService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			lancamento.setTipo(manutencao ? "Serviço" : "Despesa");
			if (!manutencao) {
				lancamento.setTipoManutencao(null);
				lancamento.setDataEntrada(null);
				lancamento.setDataSaida(null);
				lancamento.setKmProximaRevisao(null);
			}
			lancamentoVeiculoService.salvar(lancamento, usuarioSessaoMB.getUsuario().getNome());

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogDespesaManutencao();
			carregarLista();

		} catch (IllegalStateException e) {
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public List<Veiculo> completarVeiculo(String str) {
		List<Veiculo> list = daoVeiculo.listaComStatus(Veiculo.class);
		List<Veiculo> selecionados = new ArrayList<>();
		for (Veiculo cur : list) {
			if (cur.getDescricao().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public List<CategoriaDespesa> completarCategoria(String str) {
		List<CategoriaDespesa> list = daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class);
		List<CategoriaDespesa> selecionados = new ArrayList<>();
		for (CategoriaDespesa cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public List<Fornecedor> completarFornecedor(String str) {
		List<Fornecedor> list = daoFornecedor.listaComStatus(Fornecedor.class);
		List<Fornecedor> selecionados = new ArrayList<>();
		for (Fornecedor cur : list) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public void criarNovoObjeto() {
		lancamento = new LancamentoVeiculo();
		lancamento.setTipo("Despesa");
		lancamento.setData(new Date());
		manutencao = false;
	}

	public void carregarLista() {
		listLancamento = dao.listar(LancamentoVeiculo.class, "tipo in ('Despesa','Serviço') order by data desc");
	}

	public LancamentoVeiculo getLancamento() {
		return lancamento;
	}

	public void setLancamento(LancamentoVeiculo lancamento) {
		this.lancamento = lancamento;
	}

	public List<LancamentoVeiculo> getListLancamento() {
		return listLancamento;
	}

	public void setListLancamento(List<LancamentoVeiculo> listLancamento) {
		this.listLancamento = listLancamento;
	}

	public boolean isManutencao() {
		return manutencao;
	}

	public void setManutencao(boolean manutencao) {
		this.manutencao = manutencao;
	}

}
