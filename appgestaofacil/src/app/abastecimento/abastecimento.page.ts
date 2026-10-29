import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ToastController, AlertController } from '@ionic/angular';
import { Camera, CameraOptions } from '@ionic-native/camera/ngx';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

@Component({
  selector: 'app-abastecimento',
  templateUrl: './abastecimento.page.html',
  styleUrls: ['./abastecimento.page.scss'],
})
export class AbastecimentoPage implements OnInit {

  veiculoSelecionado: any = { id: null, descricao: '', placa: '' };
  motoristaSelecionado: any = { id: null, nome: '' };
  postos: any[] = [];

  data: string = new Date().toISOString();
  km: number = null;
  litros: number = null;
  valorLitro: number = null;
  combustivel: string = null;
  fornecedor: any = null;
  observacao: string = '';
  fotoBase64: string = null;

  combustiveis = ['Diesel S10', 'Diesel S500', 'Gasolina', 'Etanol', 'GNV', 'Arla 32', 'Outro'];

  carregandoPostos = false;
  salvando = false;

  constructor(
    private storage: Storage,
    private navCtrl: NavController,
    private toastController: ToastController,
    private alertController: AlertController,
    private camera: Camera,
    private atualizarDadosService: AtualizarDadosService
  ) { }

  async ngOnInit() {
    const veiculo = await this.storage.get('veiculoSelecionado');
    if (veiculo) { this.veiculoSelecionado = veiculo; }

    const motorista = await this.storage.get('motoristaSelecionado');
    if (motorista) { this.motoristaSelecionado = motorista; }

    await this.carregarPostos();
  }

  /** Busca os fornecedores/postos direto do backend — não usa o cache de sincronização em background. */
  private async carregarPostos() {
    this.carregandoPostos = true;
    try {
      const fornecedores = await this.atualizarDadosService.buscarFornecedoresAgora();
      this.postos = fornecedores.filter(f => f.tipo === 'Posto');
    } catch (error) {
      console.warn('[Abastecimento] Falha ao buscar postos:', error);
      await this.avisar(`Não foi possível carregar os postos (${this.atualizarDadosService.descreverErro(error)}).`);
    } finally {
      this.carregandoPostos = false;
    }
  }

  compareFornecedor(a: any, b: any): boolean {
    return a && b ? a.id === b.id : a === b;
  }

  /** Cadastra um posto que não está na lista, direto pelo app, e já seleciona ele. */
  async cadastrarNovoPosto() {
    const alerta = await this.alertController.create({
      header: 'Novo posto',
      inputs: [{ name: 'nome', type: 'text', placeholder: 'Nome do posto' }],
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Cadastrar',
          handler: async (dados) => {
            const nome = (dados.nome || '').trim();
            if (!nome) { return false; }
            try {
              const criado = await this.atualizarDadosService.criarFornecedorAgora(nome, 'Posto');
              this.postos = [...this.postos, criado];
              this.fornecedor = criado;
            } catch (error) {
              console.warn('[Abastecimento] Falha ao cadastrar posto:', error);
              this.avisar(`Não foi possível cadastrar o posto (${this.atualizarDadosService.descreverErro(error)}).`);
            }
            return true;
          },
        },
      ],
    });
    await alerta.present();
  }

  get valorTotal(): number {
    if (this.litros && this.valorLitro) {
      return Math.round(this.litros * this.valorLitro * 100) / 100;
    }
    return null;
  }

  async tirarFoto() {
    const options: CameraOptions = {
      quality: 70,
      destinationType: this.camera.DestinationType.DATA_URL,
      encodingType: this.camera.EncodingType.JPEG,
      mediaType: this.camera.MediaType.PICTURE,
      sourceType: this.camera.PictureSourceType.CAMERA,
      correctOrientation: true,
    };

    try {
      const imageData = await this.camera.getPicture(options);
      this.fotoBase64 = imageData;
    } catch (error) {
      console.warn('[Abastecimento] Captura de foto cancelada/falhou:', error);
    }
  }

  removerFoto() {
    this.fotoBase64 = null;
  }

  async salvar() {
    if (!this.veiculoSelecionado || !this.veiculoSelecionado.id) {
      this.avisar('Selecione um veículo na tela inicial antes de registrar o abastecimento.');
      return;
    }
    if (!this.motoristaSelecionado || !this.motoristaSelecionado.id) {
      this.avisar('Selecione um motorista na tela inicial antes de registrar o abastecimento.');
      return;
    }
    if (!this.km) {
      this.avisar('Informe o km do odômetro.');
      return;
    }
    if (!this.litros) {
      this.avisar('Informe os litros abastecidos.');
      return;
    }
    if (!this.valorLitro) {
      this.avisar('Informe o valor por litro.');
      return;
    }
    if (!this.combustivel) {
      this.avisar('Selecione o combustível.');
      return;
    }

    const lancamento: any = {
      tipo: 'Abastecimento',
      veiculo: { id: this.veiculoSelecionado.id },
      motorista: this.motoristaSelecionado && this.motoristaSelecionado.id ? { id: this.motoristaSelecionado.id } : null,
      // manda o ISO completo (com hora/timezone) — o backend usa JAXB/MOXy pra
      // desserializar o JSON, que espera xsd:dateTime; um "yyyy-MM-dd" sozinho
      // (sem "T"/hora) não bate no formato esperado e a data se perde
      data: this.data,
      km: this.km,
      litros: this.litros,
      valorLitro: this.valorLitro,
      valor: this.valorTotal,
      combustivel: this.combustivel,
      fornecedor: this.fornecedor && this.fornecedor.id ? { id: this.fornecedor.id } : null,
      observacao: this.observacao,
    };

    this.salvando = true;
    try {
      const salvo = await this.atualizarDadosService.salvarLancamentoAgora(lancamento);
      if (!salvo || !salvo.id) {
        throw new Error('resposta inválida do servidor');
      }

      if (this.fotoBase64) {
        try {
          await this.atualizarDadosService.enviarAnexoAgora(salvo.id, this.fotoBase64, `abastecimento-${Date.now()}.jpg`);
        } catch (erroAnexo) {
          console.warn('[Abastecimento] Salvo, mas falha ao enviar anexo:', erroAnexo);
          await this.avisar('Abastecimento salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.');
        }
      }

      await this.avisar('Abastecimento salvo com sucesso!');
      this.navCtrl.pop();
    } catch (error) {
      console.warn('[Abastecimento] Falha ao salvar:', error);
      await this.avisar(`Não foi possível salvar (${this.atualizarDadosService.descreverErro(error)}). Verifique sua conexão e tente novamente.`);
    } finally {
      this.salvando = false;
    }
  }

  private async avisar(mensagem: string) {
    const toast = await this.toastController.create({ message: mensagem, duration: 3000 });
    await toast.present();
  }

  voltar() {
    this.navCtrl.pop();
  }
}
