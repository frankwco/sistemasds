import { Component, OnInit } from '@angular/core';
import { NavController, ToastController } from '@ionic/angular';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

@Component({
  selector: 'app-alertas',
  templateUrl: './alertas.page.html',
  styleUrls: ['./alertas.page.scss'],
})
export class AlertasPage implements OnInit {

  alertas: any[] = [];
  carregando = false;
  erro: string = null;

  constructor(
    private navCtrl: NavController,
    private toastController: ToastController,
    private atualizarDadosService: AtualizarDadosService
  ) { }

  async ngOnInit() {
    await this.atualizar();
  }

  /** Busca sempre ao vivo — sem cache local nem fallback offline; exige conexão, igual os outros lançamentos de veículo. */
  async atualizar() {
    this.carregando = true;
    this.erro = null;
    try {
      this.alertas = await this.atualizarDadosService.buscarAlertasAgora();
    } catch (error) {
      console.warn('[Alertas] Falha ao buscar:', error);
      this.alertas = [];
      this.erro = `Não foi possível buscar os alertas (${this.atualizarDadosService.descreverErro(error)}).`;
      await this.avisar(this.erro);
    } finally {
      this.carregando = false;
    }
  }

  private async avisar(mensagem: string) {
    const toast = await this.toastController.create({ message: mensagem, duration: 3500 });
    await toast.present();
  }

  voltar() {
    this.navCtrl.pop();
  }
}
