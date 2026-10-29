import { Component, OnDestroy, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ToastController, ActionSheetController, AlertController } from '@ionic/angular';
import { Camera, CameraOptions } from '@ionic-native/camera/ngx';
import { SpeechRecognition } from '@ionic-native/speech-recognition/ngx';
import { Media, MediaObject } from '@ionic-native/media/ngx';
import { File } from '@ionic-native/file/ngx';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

interface MensagemChat {
  tipo: 'enviada' | 'recebida';
  texto?: string;
  icone?: string;
  pendente?: boolean;
  status?: boolean;
}

/**
 * Captura assistida por IA (RF11 voz / RF12 foto), no mesmo esquema da tela
 * administrativa (`capturaIA.xhtml`): um chat onde o motorista opcionalmente
 * anexa uma foto e/ou fala/digita um complemento, e só quando clica em
 * "Enviar" é que os dois (o que estiver preenchido) vão numa única chamada
 * pro Gemini extrair os campos. Nada é salvo antes de confirmar (RF14).
 */
@Component({
  selector: 'app-captura-ia',
  templateUrl: './captura-ia.page.html',
  styleUrls: ['./captura-ia.page.scss'],
})
export class CapturaIaPage implements OnInit, OnDestroy {

  veiculoSelecionado: any = { id: null, descricao: '', placa: '' };
  motoristaSelecionado: any = { id: null, nome: '' };
  categorias: any[] = [];
  fornecedores: any[] = [];
  veiculos: any[] = [];
  motoristas: any[] = [];

  combustiveis = ['Diesel S10', 'Diesel S500', 'Gasolina', 'Etanol', 'GNV', 'Arla 32', 'Outro'];

  // ─── chat de captura ───
  mensagens: MensagemChat[] = [];
  fotoBase64: string = null;
  fotoNome: string = null;
  textoComplemento = '';
  enviando = false;
  ouvindo = false;
  transcrevendo = false;

  // texto já presente no campo antes de começar a gravar (permite continuar
  // um complemento já digitado, ou uma segunda fala depois de parar)
  private textoAntesDaFala = '';
  private gravacaoAtual: MediaObject = null;
  private caminhoAudioAtual: string = null;

  // foto que efetivamente virou anexo desta extração (some do chat quando
  // uma nova captura começa, mas precisa sobreviver até confirmar() salvar)
  private fotoParaAnexo: string = null;
  private fotoNomeParaAnexo: string = null;

  carregandoCatalogos = false;
  salvando = false;

  // mensagens que vão se revezando enquanto espera a IA responder (áudio/foto
  // podem demorar dezenas de segundos) — só pra deixar claro que ainda está
  // processando, e não travado
  private readonly mensagensProcessandoIA = [
    'Analisando com IA...',
    'Processando...',
    'Interpretando os dados...',
    'Quase lá...',
  ];
  mensagemTranscrevendo = 'Transcrevendo áudio...';
  private intervalosAtivos: any[] = [];

  revisando = false;
  origem: string = null;

  tipo: string = null;
  categoriaDespesa: any = null;
  fornecedor: any = null;
  fornecedorSugerido: string = null;
  valor: number = null;
  litros: number = null;
  valorLitro: number = null;
  km: number = null;
  combustivel: string = null;
  observacao = '';
  numeroDocumento: string = null;
  confianca: number = null;
  textoBruto = '';
  mostrarTextoBruto = false;
  avisoDuplicidade: string = null;

  constructor(
    private storage: Storage,
    private navCtrl: NavController,
    private toastController: ToastController,
    private actionSheetController: ActionSheetController,
    private alertController: AlertController,
    private camera: Camera,
    private speechRecognition: SpeechRecognition,
    private media: Media,
    private file: File,
    private atualizarDadosService: AtualizarDadosService
  ) { }

  async ngOnInit() {
    const veiculo = await this.storage.get('veiculoSelecionado');
    if (veiculo) { this.veiculoSelecionado = veiculo; }

    const motorista = await this.storage.get('motoristaSelecionado');
    if (motorista) { this.motoristaSelecionado = motorista; }

    await this.carregarCatalogos();
  }

  /**
   * Busca categoria/fornecedor/veículo/motorista direto do backend a cada
   * abertura da tela — pra um cadastro feito no admin (ex.: um fornecedor
   * novo) aparecer aqui na mesma hora, sem depender do ciclo de
   * sincronização em background.
   */
  private async carregarCatalogos() {
    this.carregandoCatalogos = true;
    try {
      [this.categorias, this.fornecedores, this.veiculos, this.motoristas] = await Promise.all([
        this.atualizarDadosService.buscarCategoriasDespesaAgora(),
        this.atualizarDadosService.buscarFornecedoresAgora(),
        this.atualizarDadosService.buscarVeiculosAgora(),
        this.atualizarDadosService.buscarMotoristasAgora(),
      ]);
    } catch (error) {
      console.warn('[CapturaIA] Falha ao buscar catálogos:', error);
      await this.avisar(`Não foi possível carregar os catálogos (${this.atualizarDadosService.descreverErro(error)}). Verifique sua conexão.`);
    } finally {
      this.carregandoCatalogos = false;
    }
  }

  /** Casa o texto da IA com uma das opções fixas do select; null se não achar nada parecido. */
  private normalizarCombustivel(valor: string): string {
    if (!valor) { return null; }
    const buscaMinuscula = valor.toLowerCase();
    const encontrado = this.combustiveis.find(
      c => c.toLowerCase() === buscaMinuscula || buscaMinuscula.includes(c.toLowerCase()) || c.toLowerCase().includes(buscaMinuscula)
    );
    if (encontrado) { return encontrado; }
    return buscaMinuscula.includes('diesel') ? 'Diesel S10' : null;
  }

  /**
   * Mantém litros/valorLitro/valor consistentes — com dois dos três valores
   * preenchidos, calcula o terceiro, pra nunca pedir uma conta que já dá
   * pra fazer sozinho.
   */
  recalcularAbastecimento() {
    if (this.litros && this.litros > 0) {
      if (this.valorLitro != null && this.valor == null) {
        this.valor = Math.round(this.litros * this.valorLitro * 100) / 100;
      } else if (this.valor != null && this.valorLitro == null) {
        this.valorLitro = Math.round((this.valor / this.litros) * 1000) / 1000;
      }
    }
  }

  /** Só letras/números em minúsculo — pra "BCW", "bcw-3c49" e "BCW 3C49" casarem igual. */
  private normalizarPlaca(valor: string): string {
    return valor ? valor.toLowerCase().replace(/[^a-z0-9]/g, '') : '';
  }

  compareEntidade(a: any, b: any): boolean {
    return a && b ? a.id === b.id : a === b;
  }

  /** Cadastra um fornecedor que não está na lista, direto pelo app, e já seleciona ele. */
  async cadastrarNovoFornecedor() {
    const alerta = await this.alertController.create({
      header: 'Novo fornecedor',
      inputs: [{ name: 'nome', type: 'text', placeholder: 'Nome do fornecedor' }],
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Cadastrar',
          handler: async (dados) => {
            const nome = (dados.nome || '').trim();
            if (!nome) { return false; }
            try {
              const tipoFornecedor = this.tipo === 'Abastecimento' ? 'Posto' : null;
              const criado = await this.atualizarDadosService.criarFornecedorAgora(nome, tipoFornecedor);
              this.fornecedores = [...this.fornecedores, criado];
              this.fornecedor = criado;
              this.fornecedorSugerido = null;
            } catch (error) {
              console.warn('[CapturaIA] Falha ao cadastrar fornecedor:', error);
              this.avisar(`Não foi possível cadastrar o fornecedor (${this.atualizarDadosService.descreverErro(error)}).`);
            }
            return true;
          },
        },
      ],
    });
    await alerta.present();
  }

  /** Abre um cupom fiscal, nota ou anotação (foto ou galeria) — não chama a IA ainda, só fica pendente no chat. */
  async anexarFoto() {
    const acao = await this.actionSheetController.create({
      header: 'Anexar foto',
      buttons: [
        { text: 'Tirar foto', icon: 'camera-outline', handler: () => this.capturarFoto(this.camera.PictureSourceType.CAMERA) },
        { text: 'Escolher da galeria', icon: 'image-outline', handler: () => this.capturarFoto(this.camera.PictureSourceType.PHOTOLIBRARY) },
        { text: 'Cancelar', icon: 'close-outline', role: 'cancel' },
      ],
    });
    await acao.present();
  }

  private async capturarFoto(sourceType: number) {
    const options: CameraOptions = {
      quality: 70,
      destinationType: this.camera.DestinationType.DATA_URL,
      encodingType: this.camera.EncodingType.JPEG,
      mediaType: this.camera.MediaType.PICTURE,
      sourceType,
      correctOrientation: true,
    };

    try {
      const base64 = await this.camera.getPicture(options);
      this.fotoBase64 = base64;
      this.fotoNome = `comprovante-${Date.now()}.jpg`;
      this.mensagens.push({ tipo: 'enviada', icone: 'image-outline', texto: this.fotoNome, pendente: true });
    } catch (error) {
      console.warn('[CapturaIA] Captura de foto cancelada/falhou:', error);
      // usuário cancelando a câmera/galeria não é erro — só avisa se for
      // algo diferente disso (ex.: plugin nativo ausente no APK instalado)
      if (!this.ehCancelamento(error)) {
        this.avisar('Não foi possível abrir a câmera/galeria. Verifique as permissões do app.');
      }
    }
  }

  /** Detecta se a rejeição da câmera/galeria foi só o usuário cancelando (não é erro de verdade). */
  private ehCancelamento(error: any): boolean {
    const mensagem = typeof error === 'string' ? error : (error && error.message) || '';
    return /cancel|no image selected/i.test(mensagem);
  }

  removerFoto() {
    this.fotoBase64 = null;
    this.fotoNome = null;
    const indice = this.mensagens.map(m => !!m.pendente).lastIndexOf(true);
    if (indice >= 0) { this.mensagens.splice(indice, 1); }
  }

  /**
   * Grava o áudio inteiro sem interrupção e manda pro Gemini transcrever
   * quando parar (RF11) — tentamos reconhecimento de voz ao vivo com o
   * SpeechRecognizer do Android antes, mas ele corta a escuta sozinho a
   * cada poucos segundos de pausa (limitação da plataforma, não é bug do
   * app) e perde pedaços da fala. Gravar o áudio como arquivo não tem esse
   * limite; a transcrição só demora um pouco mais porque só chega depois
   * de mandar o áudio inteiro pra IA, em vez de aparecer palavra por
   * palavra — mas vem completa.
   */
  async alternarGravacao() {
    if (this.ouvindo) {
      await this.pararGravacaoETranscrever();
      return;
    }

    try {
      // o cordova-plugin-media não tem API própria de permissão — reusa a
      // do plugin de reconhecimento de voz, que já está instalado e só
      // verifica a mesma permissão genérica de microfone (RECORD_AUDIO)
      let permitido = await this.speechRecognition.hasPermission();
      if (!permitido) {
        try {
          await this.speechRecognition.requestPermission();
          permitido = true;
        } catch {
          permitido = false;
        }
      }
      if (!permitido) {
        this.avisar('Permita o uso do microfone pra gravar um complemento por voz.');
        return;
      }

      this.textoAntesDaFala = (this.textoComplemento || '').trim();
      const nomeArquivo = `captura_audio_${Date.now()}.3gp`;
      this.caminhoAudioAtual = this.file.dataDirectory.replace(/^file:\/\//, '') + nomeArquivo;
      this.gravacaoAtual = this.media.create(this.caminhoAudioAtual);
      this.gravacaoAtual.startRecord();
      this.ouvindo = true;
    } catch (error) {
      console.warn('[CapturaIA] Falha ao iniciar gravação de áudio:', error);
      this.ouvindo = false;
      this.avisar(`Não foi possível usar o microfone (${this.atualizarDadosService.descreverErro(error)}). Tente novamente ou digite.`);
    }
  }

  private async pararGravacaoETranscrever() {
    this.ouvindo = false;
    this.transcrevendo = true;
    const gravacao = this.gravacaoAtual;
    const caminho = this.caminhoAudioAtual;
    this.gravacaoAtual = null;
    this.caminhoAudioAtual = null;

    this.mensagemTranscrevendo = 'Transcrevendo áudio...';
    const pararRotacaoMensagens = this.iniciarRotacaoMensagens(texto => this.mensagemTranscrevendo = texto);

    try {
      gravacao.stopRecord();
      gravacao.release();
      // dá um instante pro arquivo terminar de ser gravado em disco antes de ler
      await new Promise(resolve => setTimeout(resolve, 300));

      const base64 = await this.lerArquivoComoBase64('file://' + caminho);
      const resultado = await this.atualizarDadosService.interpretarAudio(base64, 'audio/3gpp');
      const transcrito = resultado && resultado.textoBruto ? String(resultado.textoBruto).trim() : '';
      if (transcrito) {
        this.textoComplemento = this.textoAntesDaFala ? `${this.textoAntesDaFala} ${transcrito}` : transcrito;
      } else {
        this.avisar('Não consegui transcrever o áudio. Tente novamente ou digite.');
      }
    } catch (error) {
      console.warn('[CapturaIA] Falha ao transcrever áudio:', error);
      this.avisar(`Não foi possível transcrever o áudio (${this.atualizarDadosService.descreverErro(error)}). Tente novamente ou digite.`);
    } finally {
      pararRotacaoMensagens();
      this.transcrevendo = false;
    }
  }

  /**
   * Alterna `atualizarTexto` pelas mensagens de `mensagensProcessandoIA` a
   * cada 2,5s, pro usuário ver que algo está acontecendo durante uma
   * chamada à IA que pode demorar. Retorna a função pra parar a rotação
   * (sempre chamada no `finally` de quem inicia).
   */
  private iniciarRotacaoMensagens(atualizarTexto: (texto: string) => void): () => void {
    let indice = 0;
    const intervalo = setInterval(() => {
      indice = (indice + 1) % this.mensagensProcessandoIA.length;
      atualizarTexto(this.mensagensProcessandoIA[indice]);
    }, 2500);
    this.intervalosAtivos.push(intervalo);
    return () => clearInterval(intervalo);
  }

  private lerArquivoComoBase64(caminho: string): Promise<string> {
    return new Promise((resolve, reject) => {
      (window as any).resolveLocalFileSystemURL(caminho, (entry: any) => {
        entry.file((arquivo: any) => {
          const leitor = new FileReader();
          leitor.onloadend = () => {
            const resultado = leitor.result as string;
            resolve(resultado.substring(resultado.indexOf(',') + 1));
          };
          leitor.onerror = reject;
          leitor.readAsDataURL(arquivo);
        }, reject);
      }, reject);
    });
  }

  /**
   * Ação única de "Enviar pra IA": junta o que estiver disponível — foto
   * anexada e/ou texto falado/digitado — numa única chamada. Pelo menos um
   * dos dois precisa estar preenchido.
   */
  async enviarParaIA() {
    if (!this.veiculoSelecionado || !this.veiculoSelecionado.id) {
      this.avisar('Selecione um veículo na tela inicial antes de usar a captura por IA.');
      return;
    }

    const texto = (this.textoComplemento || '').trim();
    const temFoto = !!this.fotoBase64;
    if (!temFoto && !texto) {
      this.avisar('Anexe uma foto ou fale/digite algo antes de enviar.');
      return;
    }

    this.mensagens.forEach(m => { if (m.pendente) { m.pendente = false; } });
    if (texto) {
      this.mensagens.push({ tipo: 'enviada', texto });
    }
    const bolhaStatus: MensagemChat = { tipo: 'recebida', texto: this.mensagensProcessandoIA[0], status: true };
    this.mensagens.push(bolhaStatus);
    const pararRotacaoMensagens = this.iniciarRotacaoMensagens(novoTexto => bolhaStatus.texto = novoTexto);

    const fotoBase64Atual = this.fotoBase64;
    const fotoNomeAtual = this.fotoNome;
    this.fotoBase64 = null;
    this.fotoNome = null;
    this.textoComplemento = '';
    this.enviando = true;

    try {
      let resultado: any;
      let origem: string;
      if (temFoto) {
        resultado = await this.atualizarDadosService.interpretarFotoTexto(fotoBase64Atual, 'image/jpeg', texto || null);
        origem = 'IA_Foto';
      } else {
        resultado = await this.atualizarDadosService.interpretarTexto(texto);
        origem = 'IA_Voz';
      }
      await this.aplicarResultado(resultado, origem, temFoto ? fotoBase64Atual : null, temFoto ? fotoNomeAtual : null);
      bolhaStatus.texto = 'Extraí os dados — confira no formulário';
      bolhaStatus.status = false;
    } catch (error) {
      console.warn('[CapturaIA] Falha ao processar captura:', error);
      bolhaStatus.texto = `Não consegui extrair os dados dessa vez (${this.atualizarDadosService.descreverErro(error)}). Tente novamente.`;
      bolhaStatus.status = false;
    } finally {
      pararRotacaoMensagens();
      this.enviando = false;
    }
  }

  private async aplicarResultado(resultado: any, origem: string, fotoBase64ParaAnexo: string, fotoNomeParaAnexo: string) {
    this.origem = origem;
    this.tipo = resultado.tipo || 'Despesa';
    this.valor = resultado.valor;
    this.litros = resultado.litros;
    this.valorLitro = resultado.valorLitro;
    this.km = resultado.km;
    this.combustivel = this.normalizarCombustivel(resultado.combustivel);
    this.observacao = resultado.observacao || '';
    this.numeroDocumento = resultado.numeroDocumento || null;
    this.confianca = resultado.confianca;
    this.textoBruto = resultado.textoBruto || '';
    this.categoriaDespesa = null;
    this.fornecedor = null;
    this.fornecedorSugerido = null;
    this.avisoDuplicidade = null;
    this.fotoParaAnexo = fotoBase64ParaAnexo;
    this.fotoNomeParaAnexo = fotoNomeParaAnexo;

    if (resultado.veiculo) {
      const buscaNormalizada = this.normalizarPlaca(resultado.veiculo);
      const buscaMinuscula = String(resultado.veiculo).toLowerCase();
      const encontrado = this.veiculos.find(v => {
        const placaNormalizada = this.normalizarPlaca(v.placa);
        const descricaoMinuscula = (v.descricao || '').toLowerCase();
        const casouPlaca = !!placaNormalizada && !!buscaNormalizada
          && (placaNormalizada.startsWith(buscaNormalizada) || placaNormalizada.includes(buscaNormalizada));
        const casouDescricao = !!descricaoMinuscula && descricaoMinuscula.includes(buscaMinuscula);
        return casouPlaca || casouDescricao;
      });
      if (encontrado) {
        // só troca o veículo pré-selecionado na Home se a IA achou um diferente
        this.veiculoSelecionado = encontrado;
      } else {
        this.observacao = (this.observacao ? this.observacao + ' — ' : '') + `Veículo sugerido: ${resultado.veiculo}`;
      }
    }

    if (resultado.motorista) {
      const nomeBusca = String(resultado.motorista).toLowerCase();
      const encontrado = this.motoristas.find(
        m => m.nome && (m.nome.toLowerCase().includes(nomeBusca) || nomeBusca.includes(m.nome.toLowerCase()))
      );
      if (encontrado) {
        // só troca o motorista pré-selecionado na Home se a IA achou um diferente
        this.motoristaSelecionado = encontrado;
      } else {
        this.observacao = (this.observacao ? this.observacao + ' — ' : '') + `Motorista sugerido: ${resultado.motorista}`;
      }
    }

    if (resultado.categoria) {
      this.categoriaDespesa = this.categorias.find(
        c => c.nome && c.nome.toLowerCase() === resultado.categoria.toLowerCase()
      ) || null;
    }

    if (resultado.fornecedor) {
      const nomeBusca = String(resultado.fornecedor).toLowerCase();
      this.fornecedor = this.fornecedores.find(
        f => f.nome && (f.nome.toLowerCase().includes(nomeBusca) || nomeBusca.includes(f.nome.toLowerCase()))
      ) || null;
      if (!this.fornecedor) {
        // não achou localmente — manda o nome sugerido junto no lançamento;
        // o backend cadastra automaticamente ao salvar (confirmar())
        this.fornecedorSugerido = resultado.fornecedor;
        this.observacao = (this.observacao ? this.observacao + ' — ' : '')
          + `Fornecedor "${resultado.fornecedor}" será cadastrado automaticamente`;
      }
    }

    this.recalcularAbastecimento();

    // checagem de duplicidade best-effort — só roda se tiver internet;
    // se falhar (offline, timeout etc.) fica em silêncio, não bloqueia nada
    const veiculoId = this.veiculoSelecionado && this.veiculoSelecionado.id;
    if (veiculoId && (this.km || this.numeroDocumento)) {
      this.avisoDuplicidade = await this.atualizarDadosService.verificarDuplicidade(veiculoId, this.km, this.numeroDocumento);
    }

    this.revisando = true;
  }

  async confirmar() {
    if (!this.valor) {
      this.avisar('Informe o valor antes de confirmar.');
      return;
    }
    if (this.tipo === 'Abastecimento') {
      if (!this.motoristaSelecionado || !this.motoristaSelecionado.id) {
        this.avisar('Selecione um motorista antes de confirmar o abastecimento.');
        return;
      }
      if (!this.km) {
        this.avisar('Informe o km do odômetro antes de confirmar o abastecimento.');
        return;
      }
      if (!this.litros) {
        this.avisar('Informe os litros antes de confirmar o abastecimento.');
        return;
      }
      if (!this.valorLitro) {
        this.avisar('Informe o valor por litro antes de confirmar o abastecimento.');
        return;
      }
      if (!this.combustivel) {
        this.avisar('Selecione o combustível antes de confirmar o abastecimento.');
        return;
      }
    }

    const lancamento: any = {
      tipo: this.tipo,
      veiculo: { id: this.veiculoSelecionado.id },
      motorista: this.motoristaSelecionado && this.motoristaSelecionado.id ? { id: this.motoristaSelecionado.id } : null,
      categoriaDespesa: this.categoriaDespesa && this.categoriaDespesa.id ? { id: this.categoriaDespesa.id } : null,
      fornecedor: this.fornecedor && this.fornecedor.id ? { id: this.fornecedor.id } : null,
      fornecedorSugerido: !this.fornecedor && this.fornecedorSugerido ? this.fornecedorSugerido : null,
      // ISO completo (não só "yyyy-MM-dd") — o backend desserializa via
      // JAXB/MOXy, que espera xsd:dateTime; sem hora/timezone a data se perde
      data: new Date().toISOString(),
      valor: this.valor,
      litros: this.litros,
      valorLitro: this.valorLitro,
      km: this.km,
      combustivel: this.combustivel,
      observacao: this.observacao,
      numeroDocumento: this.numeroDocumento,
      origemLancamento: this.origem,
      confiancaIA: this.confianca,
      textoBrutoIA: this.textoBruto,
    };

    this.salvando = true;
    try {
      const salvo = await this.atualizarDadosService.salvarLancamentoAgora(lancamento);
      if (!salvo || !salvo.id) {
        throw new Error('resposta inválida do servidor');
      }

      if (this.fotoParaAnexo) {
        try {
          const nome = this.fotoNomeParaAnexo || `comprovante-${Date.now()}.jpg`;
          await this.atualizarDadosService.enviarAnexoAgora(salvo.id, this.fotoParaAnexo, nome);
        } catch (erroAnexo) {
          console.warn('[CapturaIA] Salvo, mas falha ao enviar anexo:', erroAnexo);
          await this.avisar('Lançamento salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.');
        }
      }

      await this.avisar('Lançamento salvo com sucesso!');
      this.navCtrl.pop();
    } catch (error) {
      console.warn('[CapturaIA] Falha ao salvar:', error);
      await this.avisar(`Não foi possível salvar (${this.atualizarDadosService.descreverErro(error)}). Verifique sua conexão e tente novamente.`);
    } finally {
      this.salvando = false;
    }
  }

  descartar() {
    this.revisando = false;
    this.origem = null;
    this.fotoParaAnexo = null;
    this.fotoNomeParaAnexo = null;
    this.avisoDuplicidade = null;
  }

  private async avisar(mensagem: string) {
    const toast = await this.toastController.create({ message: mensagem, duration: 3500 });
    await toast.present();
  }

  voltar() {
    this.navCtrl.pop();
  }

  ngOnDestroy() {
    if (this.gravacaoAtual) {
      this.gravacaoAtual.stopRecord();
      this.gravacaoAtual.release();
    }
    this.intervalosAtivos.forEach(intervalo => clearInterval(intervalo));
  }
}
