package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.LeituraOdometro;
import base.modelo.Veiculo;
import base.service.LeituraOdometroService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

/**
 * Leitura avulsa de odômetro (RF29) — não precisa de despesa/abastecimento
 * junto; atualiza o km atual do veículo direto via
 * {@link LeituraOdometroService#registrar}.
 */
@ViewScoped
@Named("leituraOdometroMB")
public class LeituraOdometroMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private LeituraOdometro leitura;
	private List<LeituraOdometro> listLeitura;

	@Inject
	private GenericDAO<LeituraOdometro> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private LeituraOdometroService leituraOdometroService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		carregarLista();
	}

	public void salvar() {
		try {
			leituraOdometroService.registrar(leitura.getVeiculo(), leitura.getKm(),
					leitura.getData() == null ? new Date() : leitura.getData(), "Manual", null,
					usuarioSessaoMB.getUsuario().getNome());

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogLeituraOdometro();
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

	public void criarNovoObjeto() {
		leitura = new LeituraOdometro();
		leitura.setData(new Date());
	}

	public void carregarLista() {
		listLeitura = dao.listaComStatus(LeituraOdometro.class, "data desc", 50);
	}

	public LeituraOdometro getLeitura() {
		return leitura;
	}

	public void setLeitura(LeituraOdometro leitura) {
		this.leitura = leitura;
	}

	public List<LeituraOdometro> getListLeitura() {
		return listLeitura;
	}

	public void setListLeitura(List<LeituraOdometro> listLeitura) {
		this.listLeitura = listLeitura;
	}

}
