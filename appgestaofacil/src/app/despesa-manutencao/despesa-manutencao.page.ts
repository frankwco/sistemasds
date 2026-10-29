import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ToastController, AlertController } from '@ionic/angular';
import { Camera, CameraOptions } from '@ionic-native/camera/ngx';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

@Component({
  selector: 'app-despesa-manutencao',
  templateUrl: './despesa-manutencao.page.html',
  styleUrls: ['./despesa-manutencao.page.scss'],
})
export class DespesaManutencaoPage implements OnInit {

  veiculoSelecionado: any = { id: null, descricao: '', placa: '' };
  motoristaSelecionado: any = { id: null, nome: '' };
  categorias: any[] = [];
  fornecedores: any[] = [];

  data: string = new Date().toISOString();
  categoriaDespesa: any = null;
  fornecedor: any = null;
  valor: number = null;
  observacao: string = '';
  fotoBase64: string = null;

  manutencao = false;
  tipoManutencao: string = null;
  dataEntrada: string = null;
  dataSaida: string = null;
  kmProximaRevisao: number = null;

  carregandoCatalogos = false;
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

    await this.carregarCatalogos();
  }

  /** Busca categorias de despesa e fornecedores direto do backend a cada abertura da tela. */
  private async carregarCatalogos() {
    this.carregandoCatalogos = true;
    try {
      [this.categorias, this.fornecedores] = await Promise.all([
        this.atualizarDadosService.buscarCategoriasDespesaAgora(),
        this.atualizarDadosService.buscarFornecedoresAgora(),
      ]);
    } catch (error) {
      console.warn('[DespesaManutencao] Falha ao buscar categorias/fornecedores:', error);
      await this.avisar(`Não foi possível carregar categorias/fornecedores (${this.atualizarDadosService.descreverErro(error)}).`);
    } finally {
      this.carregandoCatalogos = false;
    }
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
              const criado = await this.atualizarDadosService.criarFornecedorAgora(nome);
              this.fornecedores = [...this.fornecedores, criado];
              this.fornecedor = criado;
            } catch (error) {
              console.warn('[DespesaManutencao] Falha ao cadastrar fornecedor:', error);
              this.avisar(`Não foi possível cadastrar o fornecedor (${this.atualizarDadosService.descreverErro(error)}).`);
            }
            return true;
          },
        },
      ],
    });
    await alerta.present();
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
      console.warn('[DespesaManutencao] Captura de foto cancelada/falhou:', error);
    }
  }

  removerFoto() {
    this.fotoBase64 = null;
  }

  async salvar() {
    if (!this.veiculoSelecionado || !this.veiculoSelecionado.id) {
      this.avisar('Selecione um veículo na tela inicial antes de registrar.');
      return;
    }
    if (!this.valor) {
      this.avisar('Informe o valor da despesa.');
      return;
    }

    const lancamento: any = {
      tipo: this.manutencao ? 'Serviço' : 'Despesa',
      veiculo: { id: this.veiculoSelecionado.id },
      motorista: this.motoristaSelecionado && this.motoristaSelecionado.id ? { id: this.motoristaSelecionado.id } : null,
      categoriaDespesa: this.categoriaDespesa && this.categoriaDespesa.id ? { id: this.categoriaDespesa.id } : null,
      fornecedor: this.fornecedor && this.fornecedor.id ? { id: this.fornecedor.id } : null,
      // ISO completo (não só "yyyy-MM-dd") — o backend desserializa via
      // JAXB/MOXy, que espera xsd:dateTime; sem hora/timezone a data se perde
      data: this.data,
      valor: this.valor,
      observacao: this.observacao,
    };

    if (this.manutencao) {
      lancamento.tipoManutencao = this.tipoManutencao;
      lancamento.dataEntrada = this.dataEntrada || null;
      lancamento.dataSaida = this.dataSaida || null;
      lancamento.kmProximaRevisao = this.kmProximaRevisao;
    }

    this.salvando = true;
    try {
      const salvo = await this.atualizarDadosService.salvarLancamentoAgora(lancamento);
      if (!salvo || !salvo.id) {
        throw new Error('resposta inválida do servidor');
      }

      if (this.fotoBase64) {
        try {
          await this.atualizarDadosService.enviarAnexoAgora(salvo.id, this.fotoBase64, `despesa-${Date.now()}.jpg`);
        } catch (erroAnexo) {
          console.warn('[DespesaManutencao] Salvo, mas falha ao enviar anexo:', erroAnexo);
          await this.avisar('Registro salvo, mas não foi possível anexar a foto. Anexe depois pela tela administrativa.');
        }
      }

      await this.avisar('Registrado com sucesso!');
      this.navCtrl.pop();
    } catch (error) {
      console.warn('[DespesaManutencao] Falha ao salvar:', error);
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
