package base.controle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import base.modelo.Lembrete;
import base.modelo.Veiculo;
import base.service.LembreteService;
import util.ExibirMensagem;
import util.FecharDialog;
import util.Mensagem;
import dao.GenericDAO;

/**
 * Lembrete avulso (RF31) — alvo por data ou por km. Concluir (RF33) tem duas
 * ações: só marcar como feito, ou marcar e já gerar um Serviço em aberto no
 * veículo (quando o lembrete tem veículo vinculado).
 */
@ViewScoped
@Named("lembreteMB")
public class LembreteMB implements Serializable {

	private static final long serialVersionUID = 1L;

	private Lembrete lembrete;
	private List<Lembrete> listLembrete;

	@Inject
	private GenericDAO<Lembrete> dao;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private LembreteService lembreteService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	@PostConstruct
	public void inicializar() {
		criarNovoObjeto();
		carregarLista();
	}

	public void preencherLista(Lembrete t) {
		this.lembrete = t;
	}

	public void inativar(Lembrete t) {
		t.setStatus(false);
		lembreteService.inserirAlterar(t);
		criarNovoObjeto();
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void salvar() {
		try {
			if (lembrete.getId() == null) {
				lembrete.setCriadoPor(usuarioSessaoMB.getUsuario().getNome());
				lembrete.setCriadoEm(new Date());
			}
			lembrete.setStatus(true);
			lembreteService.inserirAlterar(lembrete);

			criarNovoObjeto();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			FecharDialog.fecharDialogLembrete();
			carregarLista();

		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	public void concluir(Lembrete t) {
		lembreteService.concluir(t, false, usuarioSessaoMB.getUsuario().getNome());
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
	}

	public void concluirEGerarServico(Lembrete t) {
		lembreteService.concluir(t, true, usuarioSessaoMB.getUsuario().getNome());
		ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		carregarLista();
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
		lembrete = new Lembrete();
		lembrete.setAlvoTipo("Data");
		lembrete.setConcluido(false);
	}

	public void carregarLista() {
		listLembrete = dao.listar(Lembrete.class, "concluido is false order by id desc");
	}

	public Lembrete getLembrete() {
		return lembrete;
	}

	public void setLembrete(Lembrete lembrete) {
		this.lembrete = lembrete;
	}

	public List<Lembrete> getListLembrete() {
		return listLembrete;
	}

	public void setListLembrete(List<Lembrete> listLembrete) {
		this.listLembrete = listLembrete;
	}

}
