package base.controle;

import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import base.modelo.Anexo;
import base.modelo.CategoriaDespesa;
import base.modelo.Fornecedor;
import base.modelo.InterpretacaoIADTO;
import base.modelo.LancamentoVeiculo;
import base.modelo.Usuario;
import base.modelo.Veiculo;
import base.service.AnexoService;
import base.service.FornecedorService;
import base.service.LancamentoVeiculoService;
import dao.GenericDAO;
import util.ExibirMensagem;
import util.FirebaseStorageUtil;
import util.GeminiClient;
import util.Mensagem;

/**
 * Captura assistida por IA (RF11 voz / RF12 foto) na tela administrativa.
 * Fluxo: (1) o usuário opcionalmente anexa uma foto (cupom fiscal, nota ou
 * anotação) — fica só pendente em {@link #fotoBytes}, sem chamar a IA; (2)
 * opcionalmente fala ou digita um complemento — texto ao vivo via
 * SpeechRecognition do navegador (Chrome/Edge) ou digitado direto, editável
 * antes de enviar; (3) um único clique em "Enviar pra IA"
 * ({@link #processarCaptura()}) manda o que estiver preenchido — foto e/ou
 * texto, pelo menos um dos dois — numa chamada só ao Gemini via
 * {@link GeminiClient} (chamado direto, sem passar pelo REST — já estamos
 * no mesmo servidor). O resultado cai numa área de revisão editável, e só
 * grava o {@link LancamentoVeiculo} quando o usuário confirma (RF14).
 * Confiança e texto bruto (RF15) vão junto, iguais aos que aparecem depois
 * nas telas de Abastecimento/Despesa.
 */
@ViewScoped
@Named("capturaIaMB")
public class CapturaIaMB implements Serializable {

	private static final long serialVersionUID = 1L;

	// mesma lista de opções de combustível da tela de Abastecimento — a IA é
	// instruída a usar exatamente uma delas, mas normalizamos de novo aqui
	// como garantia (RF13 objetivo: nunca deixar um valor fora do select)
	private static final String[] COMBUSTIVEIS_VALIDOS = { "Diesel S10", "Diesel S500", "Gasolina", "Etanol", "GNV",
			"Arla 32", "Outro" };

	private boolean revisando;
	private LancamentoVeiculo lancamento;

	// preenchido via JS (SpeechRecognition do navegador ou digitação direta)
	// por um h:inputHidden, só quando o usuário clica em "Enviar pra IA"
	private String transcricaoTexto;

	// foto anexada (cupom fiscal, nota ou anotação) fica pendente aqui até o
	// usuário clicar em "Enviar pra IA" — só então, junto com o texto (se
	// houver), vira uma chamada única ao Gemini (ver processarCaptura)
	private byte[] fotoBytes;
	private String fotoMimeType;
	private String fotoNomeArquivo;

	// cópia da foto/PDF acima que "sobrevive" ao removerFoto() do fim de
	// processarCaptura() — fica esperando aqui até confirmar() salvar o
	// lançamento (só então dá pra gravar o Anexo, que precisa do id gerado)
	private byte[] anexoPendenteBytes;
	private String anexoPendenteMimeType;
	private String anexoPendenteNome;

	// tipo do último lançamento confirmado com sucesso ("Abastecimento",
	// "Despesa" ou "Serviço") — só existe pra JS montar a bolha de "salvo"
	// com o link certo pra tela de listagem correspondente; null enquanto não
	// há nada recém-salvo, ou depois de uma tentativa de confirmar que falhou
	private String ultimoTipoSalvo;

	// preenchido em aplicarResultado() quando um possível duplicado é
	// encontrado (mesmo número de documento, ou mesmo veículo+odômetro) — só
	// avisa, não bloqueia: a IA pode errar a leitura, e o usuário decide
	private String avisoDuplicidade;

	@Inject
	private GenericDAO<Veiculo> daoVeiculo;

	@Inject
	private GenericDAO<CategoriaDespesa> daoCategoriaDespesa;

	@Inject
	private GenericDAO<Fornecedor> daoFornecedor;

	@Inject
	private GenericDAO<Usuario> daoUsuario;

	@Inject
	private GenericDAO<LancamentoVeiculo> daoLancamento;

	@Inject
	private LancamentoVeiculoService lancamentoVeiculoService;

	@Inject
	private FornecedorService fornecedorService;

	@Inject
	private AnexoService anexoService;

	@Inject
	private UsuarioSessaoMB usuarioSessaoMB;

	/**
	 * O dialog de revisão (RF14) fica sempre no HTML da página, só escondido
	 * via CSS enquanto {@link #revisando} é falso — diferente do antigo card
	 * com {@code rendered="#{capturaIaMB.revisando}"}, que era omitido por
	 * completo do DOM (e por isso precisava do truque do
	 * {@code p:outputPanel} sempre desenhado por fora, pra ajax ter onde
	 * inserir o conteúdo na primeira vez). Como agora o formulário do dialog
	 * é avaliado mesmo com o dialog escondido, {@link #lancamento} nunca pode
	 * ser nulo, senão as expressões EL do formulário (ex.:
	 * {@code capturaIaMB.lancamento.veiculo}) quebram com
	 * NullPointerException já no carregamento da página.
	 */
	@PostConstruct
	public void inicializar() {
		lancamento = new LancamentoVeiculo();
	}

	/**
	 * Só anexa a foto (cupom fiscal, nota ou anotação) — não chama a IA
	 * ainda. O envio pra IA é sempre uma ação explícita do usuário
	 * ({@link #processarCaptura()}), pra dar tempo dele também falar/digitar
	 * um complemento antes de mandar tudo junto numa única chamada.
	 */
	public void anexarFoto(FileUploadEvent evento) {
		UploadedFile arquivo = evento.getFile();
		if (arquivo == null || arquivo.getContents() == null || arquivo.getContents().length == 0) {
			return;
		}
		fotoBytes = arquivo.getContents();
		fotoMimeType = arquivo.getContentType();
		fotoNomeArquivo = arquivo.getFileName();
	}

	public void removerFoto() {
		fotoBytes = null;
		fotoMimeType = null;
		fotoNomeArquivo = null;
	}

	/**
	 * Ação única de "Enviar pra IA": junta o que estiver disponível — foto
	 * anexada ({@link #fotoBytes}) e/ou texto falado/digitado
	 * ({@link #transcricaoTexto}) — numa única chamada ao Gemini. Os dois são
	 * opcionais, mas pelo menos um precisa estar preenchido. Quando os dois
	 * vêm juntos, o texto complementa a foto (ex.: a foto tá borrada num
	 * campo, ou o motorista quer acrescentar algo que não está no papel).
	 */
	public void processarCaptura() {
		try {
			String texto = transcricaoTexto != null ? transcricaoTexto.trim() : "";
			boolean temFoto = fotoBytes != null;
			boolean temTexto = !texto.isEmpty();

			if (!temFoto && !temTexto) {
				ExibirMensagem.exibirMensagem("Anexe uma foto ou fale/digite algo antes de enviar.");
				return;
			}

			InterpretacaoIADTO resultado;
			String origem;
			if (temFoto) {
				resultado = GeminiClient.interpretarFotoComTexto(fotoBytes, fotoMimeType, temTexto ? texto : null);
				origem = "IA_Foto";
			} else {
				resultado = GeminiClient.interpretarTexto(texto);
				resultado.setTextoBruto(texto);
				origem = "IA_Voz";
			}
			aplicarResultado(resultado, origem);
			if (temFoto) {
				// a IA já usou os bytes de fotoBytes acima — guarda uma cópia à
				// parte pra sobreviver ao removerFoto() do finally, até confirmar()
				// gravar o lançamento e poder anexá-la de verdade (RF09)
				anexoPendenteBytes = fotoBytes;
				anexoPendenteMimeType = fotoMimeType;
				anexoPendenteNome = fotoNomeArquivo;
			}
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem(mensagemDeErro(e));
			e.printStackTrace();
		} finally {
			transcricaoTexto = null;
			removerFoto();
		}
	}

	/**
	 * A chamada ao Gemini às vezes estoura o timeout de leitura (visto na
	 * prática, sobretudo com áudio) mesmo com uma margem generosa — nesse
	 * caso a mensagem padrão de erro não ajuda o usuário a saber que basta
	 * tentar de novo.
	 */
	private String mensagemDeErro(Exception e) {
		if (e instanceof SocketTimeoutException
				|| (e.getCause() != null && e.getCause() instanceof SocketTimeoutException)) {
			return "A IA demorou demais para responder. Tente novamente — geralmente funciona na segunda tentativa.";
		}
		return Mensagem.ERRO;
	}

	/** Só letras/números em minúsculo — pra "BCW", "bcw-3c49" e "BCW 3C49" casarem igual. */
	private String normalizarPlaca(String valor) {
		if (valor == null) {
			return "";
		}
		return valor.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	/** Casa o texto da IA com uma das opções fixas do select de combustível; null se não achar nada parecido. */
	private String normalizarCombustivel(String valor) {
		if (valor == null || valor.trim().isEmpty()) {
			return null;
		}
		String buscaMinuscula = valor.toLowerCase();
		for (String opcao : COMBUSTIVEIS_VALIDOS) {
			if (opcao.equalsIgnoreCase(valor) || buscaMinuscula.contains(opcao.toLowerCase())
					|| opcao.toLowerCase().contains(buscaMinuscula)) {
				return opcao;
			}
		}
		if (buscaMinuscula.contains("diesel")) {
			return "Diesel S10";
		}
		return null;
	}

	/**
	 * Mantém litros/valorLitro/valor consistentes entre si — objetivo é nunca
	 * pedir uma conta que já dá pra fazer sozinho: com dois dos três valores
	 * preenchidos, calcula o terceiro. Chamado tanto na extração da IA quanto
	 * via ajax quando o usuário edita um dos três campos na revisão.
	 */
	public void recalcularAbastecimento() {
		if (lancamento == null) {
			return;
		}
		Double litros = lancamento.getLitros();
		Double valorLitro = lancamento.getValorLitro();
		Double valor = lancamento.getValor();

		if (litros != null && litros > 0) {
			if (valorLitro != null && valor == null) {
				lancamento.setValor(arredondar(litros * valorLitro));
			} else if (valor != null && valorLitro == null) {
				lancamento.setValorLitro(arredondar(valor / litros));
			}
		}
	}

	private Double arredondar(double valor) {
		return Math.round(valor * 100d) / 100d;
	}

	private void aplicarResultado(InterpretacaoIADTO resultado, String origem) {
		ultimoTipoSalvo = null;
		anexoPendenteBytes = null;
		anexoPendenteMimeType = null;
		anexoPendenteNome = null;
		avisoDuplicidade = null;
		LancamentoVeiculo novo = new LancamentoVeiculo();
		novo.setTipo(resultado.getTipo() != null ? resultado.getTipo() : "Despesa");
		novo.setData(new Date());
		novo.setValor(resultado.getValor());
		novo.setLitros(resultado.getLitros());
		novo.setValorLitro(resultado.getValorLitro());
		novo.setKm(resultado.getKm());
		novo.setCombustivel(normalizarCombustivel(resultado.getCombustivel()));
		novo.setObservacao(resultado.getObservacao());
		novo.setNumeroDocumento(resultado.getNumeroDocumento());
		novo.setOrigemLancamento(origem);
		novo.setConfiancaIA(resultado.getConfianca());
		novo.setTextoBrutoIA(resultado.getTextoBruto());

		if (resultado.getVeiculo() != null) {
			String buscaNormalizada = normalizarPlaca(resultado.getVeiculo());
			String buscaMinuscula = resultado.getVeiculo().toLowerCase();
			for (Veiculo veiculo : daoVeiculo.listaComStatus(Veiculo.class)) {
				String placaNormalizada = normalizarPlaca(veiculo.getPlaca());
				String descricaoMinuscula = veiculo.getDescricao() != null ? veiculo.getDescricao().toLowerCase() : "";
				boolean casouPlaca = !buscaNormalizada.isEmpty() && !placaNormalizada.isEmpty()
						&& (placaNormalizada.startsWith(buscaNormalizada) || placaNormalizada.contains(buscaNormalizada));
				boolean casouDescricao = !descricaoMinuscula.isEmpty() && descricaoMinuscula.contains(buscaMinuscula);
				if (casouPlaca || casouDescricao) {
					novo.setVeiculo(veiculo);
					break;
				}
			}
			if (novo.getVeiculo() == null) {
				String observacaoAtual = novo.getObservacao();
				novo.setObservacao((observacaoAtual != null && !observacaoAtual.isEmpty() ? observacaoAtual + " — " : "")
						+ "Veículo sugerido pela IA: " + resultado.getVeiculo());
			}
		}

		if (resultado.getMotorista() != null) {
			String nomeBusca = resultado.getMotorista().toLowerCase();
			for (Usuario motorista : daoUsuario.listar(Usuario.class, "motorista is true")) {
				if (motorista.getNome() != null && (motorista.getNome().toLowerCase().contains(nomeBusca)
						|| nomeBusca.contains(motorista.getNome().toLowerCase()))) {
					novo.setMotorista(motorista);
					break;
				}
			}
			if (novo.getMotorista() == null) {
				String observacaoAtual = novo.getObservacao();
				novo.setObservacao((observacaoAtual != null && !observacaoAtual.isEmpty() ? observacaoAtual + " — " : "")
						+ "Motorista sugerido pela IA: " + resultado.getMotorista());
			}
		}

		if (resultado.getCategoria() != null) {
			List<CategoriaDespesa> categorias = daoCategoriaDespesa.listarPorCampo(CategoriaDespesa.class, "nome",
					resultado.getCategoria());
			if (!categorias.isEmpty()) {
				novo.setCategoriaDespesa(categorias.get(0));
			}
		}

		if (resultado.getFornecedor() != null) {
			String nomeBusca = resultado.getFornecedor().toLowerCase();
			for (Fornecedor fornecedor : daoFornecedor.listaComStatus(Fornecedor.class)) {
				if (fornecedor.getNome() != null && (fornecedor.getNome().toLowerCase().contains(nomeBusca)
						|| nomeBusca.contains(fornecedor.getNome().toLowerCase()))) {
					novo.setFornecedor(fornecedor);
					break;
				}
			}
			if (novo.getFornecedor() == null) {
				novo.setFornecedor(fornecedorService.cadastrarAutomatico(resultado.getFornecedor(), novo.getTipo(),
						novo.getCategoriaDespesa()));
				String observacaoAtual = novo.getObservacao();
				novo.setObservacao((observacaoAtual != null && !observacaoAtual.isEmpty() ? observacaoAtual + " — " : "")
						+ "Fornecedor \"" + resultado.getFornecedor() + "\" cadastrado automaticamente pela IA.");
			}
		}

		verificarDuplicidade(novo, resultado.getNumeroDocumento());

		this.lancamento = novo;
		recalcularAbastecimento();
		this.revisando = true;
	}

	/**
	 * Avisa (sem bloquear) quando o lançamento parece já ter sido feito antes
	 * — a IA pode ler o mesmo comprovante duas vezes, por exemplo. Dois
	 * sinais, checados nessa ordem: (1) mesmo número de documento (cupom
	 * fiscal/nota) já usado em outro lançamento; (2) mesmo veículo com o
	 * mesmo km do odômetro já registrado. Qualquer um dos dois é forte
	 * indício de duplicidade — o valor pode até ser preenchido errado pela
	 * IA, mas o número do documento ou o odômetro exato repetido raramente é
	 * coincidência.
	 */
	private void verificarDuplicidade(LancamentoVeiculo novo, String numeroDocumento) {
		if (numeroDocumento != null && !numeroDocumento.trim().isEmpty()) {
			String numeroEscapado = numeroDocumento.trim().replace("'", "''");
			List<LancamentoVeiculo> porDocumento = daoLancamento.listar(LancamentoVeiculo.class,
					"numeroDocumento='" + numeroEscapado + "' and status is true");
			if (!porDocumento.isEmpty()) {
				avisoDuplicidade = "Já existe um lançamento com o número de documento \"" + numeroDocumento.trim()
						+ "\": " + descreverLancamento(porDocumento.get(0)) + ". Pode ser este comprovante lançado de novo.";
				return;
			}
		}

		if (novo.getVeiculo() != null && novo.getKm() != null) {
			List<LancamentoVeiculo> porOdometro = daoLancamento.listar(LancamentoVeiculo.class,
					"veiculo.id=" + novo.getVeiculo().getId() + " and km=" + novo.getKm() + " and status is true");
			if (!porOdometro.isEmpty()) {
				avisoDuplicidade = "Já existe um lançamento deste veículo com o mesmo odômetro (" + novo.getKm()
						+ " km): " + descreverLancamento(porOdometro.get(0)) + ". Pode ser um lançamento duplicado.";
			}
		}
	}

	private String descreverLancamento(LancamentoVeiculo existente) {
		String data = existente.getData() != null ? new SimpleDateFormat("dd/MM/yyyy").format(existente.getData())
				: "data desconhecida";
		String valor = existente.getValor() != null ? String.format(" (R$ %.2f)", existente.getValor()) : "";
		return existente.getTipo() + " de " + data + valor;
	}

	public void confirmar() {
		try {
			if (!"Abastecimento".equals(lancamento.getTipo()) && !"Serviço".equals(lancamento.getTipo())) {
				lancamento.setTipo("Despesa");
			}
			if (!"Serviço".equals(lancamento.getTipo())) {
				lancamento.setTipoManutencao(null);
				lancamento.setDataEntrada(null);
				lancamento.setDataSaida(null);
				lancamento.setKmProximaRevisao(null);
			}
			if (!"Abastecimento".equals(lancamento.getTipo())) {
				lancamento.setLitros(null);
				lancamento.setValorLitro(null);
				lancamento.setCombustivel(null);
			}

			lancamentoVeiculoService.salvar(lancamento, usuarioSessaoMB.getUsuario().getNome());
			salvarAnexoPendente();
			ultimoTipoSalvo = lancamento.getTipo();
			ExibirMensagem.exibirMensagem(Mensagem.SUCESSO);
			descartar();
		} catch (IllegalStateException e) {
			ultimoTipoSalvo = null;
			ExibirMensagem.exibirMensagemErro(e.getMessage());
		} catch (Exception e) {
			ultimoTipoSalvo = null;
			ExibirMensagem.exibirMensagem(Mensagem.ERRO);
			e.printStackTrace();
		}
	}

	/**
	 * Grava a foto/PDF anexada durante a captura como um {@link Anexo} do
	 * lançamento recém-salvo (RF09) — mesmo mecanismo (Firebase Storage +
	 * metadados no MySQL) usado pelo dialog de Anexos de Abastecimentos e
	 * Despesas/Manutenção. Só roda depois do {@code salvar()} porque precisa
	 * do id gerado do lançamento pra montar o caminho no Storage. Uma falha
	 * aqui não desfaz o lançamento (já foi salvo com sucesso) — só avisa, pra
	 * não fazer o usuário achar que nada foi salvo e tentar de novo,
	 * duplicando o lançamento.
	 */
	private void salvarAnexoPendente() {
		if (anexoPendenteBytes == null) {
			return;
		}
		try {
			String caminho = FirebaseStorageUtil.upload(anexoPendenteBytes, anexoPendenteNome, anexoPendenteMimeType,
					lancamento.getId());

			Anexo anexo = new Anexo();
			anexo.setLancamento(lancamento);
			anexo.setNomeArquivo(anexoPendenteNome);
			anexo.setCaminho(caminho);
			anexo.setContentType(anexoPendenteMimeType);
			anexo.setTamanho((long) anexoPendenteBytes.length);
			anexo.setCriadoPor(usuarioSessaoMB.getUsuario().getNome());
			anexo.setCriadoEm(new Date());
			anexo.setStatus(true);
			anexoService.inserirAlterar(anexo);
		} catch (Exception e) {
			ExibirMensagem.exibirMensagem("Lançamento salvo, mas não foi possível anexar o arquivo. "
					+ "Anexe manualmente depois pela tela de Abastecimentos/Despesas.");
			e.printStackTrace();
		}
	}

	public void descartar() {
		revisando = false;
		lancamento = new LancamentoVeiculo();
		anexoPendenteBytes = null;
		anexoPendenteMimeType = null;
		anexoPendenteNome = null;
		avisoDuplicidade = null;
	}

	public List<Veiculo> completarVeiculo(String str) {
		List<Veiculo> lista = daoVeiculo.listaComStatus(Veiculo.class);
		List<Veiculo> selecionados = new ArrayList<>();
		for (Veiculo cur : lista) {
			if (cur.getDescricao().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public List<CategoriaDespesa> completarCategoria(String str) {
		List<CategoriaDespesa> lista = daoCategoriaDespesa.listaComStatus(CategoriaDespesa.class);
		List<CategoriaDespesa> selecionados = new ArrayList<>();
		for (CategoriaDespesa cur : lista) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public List<Fornecedor> completarFornecedor(String str) {
		List<Fornecedor> lista = daoFornecedor.listaComStatus(Fornecedor.class);
		List<Fornecedor> selecionados = new ArrayList<>();
		for (Fornecedor cur : lista) {
			if (cur.getNome().toLowerCase().contains(str.toLowerCase())) {
				selecionados.add(cur);
			}
		}
		return selecionados;
	}

	public String getTranscricaoTexto() {
		return transcricaoTexto;
	}

	public void setTranscricaoTexto(String transcricaoTexto) {
		this.transcricaoTexto = transcricaoTexto;
	}

	public String getFotoNomeArquivo() {
		return fotoNomeArquivo;
	}

	public String getUltimoTipoSalvo() {
		return ultimoTipoSalvo;
	}

	public String getAvisoDuplicidade() {
		return avisoDuplicidade;
	}

	public boolean isRevisando() {
		return revisando;
	}

	public LancamentoVeiculo getLancamento() {
		return lancamento;
	}

	public void setLancamento(LancamentoVeiculo lancamento) {
		this.lancamento = lancamento;
	}

}
