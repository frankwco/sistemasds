package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.Fornecedor;
import base.modelo.LancamentoVeiculo;
import base.modelo.Veiculo;
import base.service.LancamentoVeiculoService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

/**
 * Tela enxuta de Abastecimento (RF07) — veículo, km, litros, valor/litro,
 * combustível e posto. Sempre grava um {@link LancamentoVeiculo} com
 * tipo="Abastecimento"; a categoria "Combustível" e a leitura de odômetro
 * (RF29) são resolvidas em {@link LancamentoVeiculoService#salvar}.
 */
@ViewScoped
@Named("abastecimentoMB")
public class AbastecimentoMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String TIPO = "Abastecimento";

	private LancamentoVeiculo lancamento;
	private List<LancamentoVeiculo> listLancamento;

	@Inject
	private GenericDAO<LancamentoVeiculo> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

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
			lancamento.setTipo(TIPO);
			if (lancamento.getLitros() != null && lancamento.getValorLitro() != null) {
				lancamento.setValor(lancamento.getLitros() * lancamento.getValorLitro());
			}
			lancamentoVeiculoService.salvar(lancamento, usuarioSessaoMB.getUsuario().getNome());

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogAbastecimento();
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

	public List<Fornecedor> completarPosto(String str) {
		List<Fornecedor> list = daoFornecedor.listar(Fornecedor.class, "tipo='Posto'");
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
		lancamento.setTipo(TIPO);
		lancamento.setData(new Date());
	}

	/**
	 * Ao selecionar o veículo no autoComplete, repete motorista, combustível,
	 * posto e valor/litro do último abastecimento dele — na prática o
	 * motorista e o posto raramente mudam de um abastecimento para o outro, e
	 * o valor/litro serve de referência para conferir o preço atual.
	 */
	public void preencherUltimoAbastecimento() {
		if (lancamento.getVeiculo() == null || lancamento.getVeiculo().getId() == null) {
			return;
		}
		List<LancamentoVeiculo> ultimos = dao.listar(LancamentoVeiculo.class, "tipo='" + TIPO + "' and veiculo.id="
				+ lancamento.getVeiculo().getId() + " order by data desc, id desc");
		if (!ultimos.isEmpty()) {
			LancamentoVeiculo ultimo = ultimos.get(0);
			lancamento.setMotorista(ultimo.getMotorista());
			lancamento.setCombustivel(ultimo.getCombustivel());
			lancamento.setFornecedor(ultimo.getFornecedor());
			lancamento.setValorLitro(ultimo.getValorLitro());
		}
	}

	public void carregarLista() {
		listLancamento = dao.listar(LancamentoVeiculo.class, "tipo='" + TIPO + "' order by data desc");
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

}
