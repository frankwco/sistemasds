import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ToastController, AlertController } from '@ionic/angular';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

/**
 * Lista os lançamentos (Abastecimento/Despesa/Serviço) do usuário
 * autenticado, com opção de excluir um feito errado — filtra pelo id de
 * quem fez login (armazenado em `storage['usuario']` no login.page.ts), NÃO
 * pelo "motorista" selecionável na tela inicial: aquele seletor é só uma
 * escolha local trocável por qualquer um, então usá-lo aqui deixava
 * qualquer pessoa ver (e excluir!) lançamentos de outro motorista só
 * trocando a seleção.
 */
@Component({
  selector: 'app-meus-lancamentos',
  templateUrl: './meus-lancamentos.page.html',
  styleUrls: ['./meus-lancamentos.page.scss'],
})
export class MeusLancamentosPage implements OnInit {

  usuarioLogado: any = { id: null, nome: '' };
  lancamentos: any[] = [];
  carregando = false;
  excluindoId: number = null;
  expandidoId: number = null;

  constructor(
    private storage: Storage,
    private navCtrl: NavController,
    private toastController: ToastController,
    private alertController: AlertController,
    private atualizarDadosService: AtualizarDadosService
  ) { }

  async ngOnInit() {
    const usuario = await this.storage.get('usuario');
    if (usuario) { this.usuarioLogado = usuario; }
    await this.carregar();
  }

  async carregar() {
    if (!this.usuarioLogado || !this.usuarioLogado.id) { return; }
    this.carregando = true;
    try {
      this.lancamentos = await this.atualizarDadosService.buscarMeusLancamentosAgora(this.usuarioLogado.id);
    } catch (error) {
      console.warn('[MeusLancamentos] Falha ao buscar lançamentos:', error);
      this.avisar(`Não foi possível carregar seus lançamentos (${this.atualizarDadosService.descreverErro(error)}).`);
    } finally {
      this.carregando = false;
    }
  }

  iconePara(lancamento: any): string {
    if (lancamento.tipo === 'Abastecimento') { return 'water-outline'; }
    if (lancamento.tipo === 'Serviço') { return 'construct-outline'; }
    return 'pricetag-outline';
  }

  alternarDetalhe(lancamento: any) {
    this.expandidoId = this.expandidoId === lancamento.id ? null : lancamento.id;
  }

  async confirmarExclusao(lancamento: any, evento: Event) {
    evento.stopPropagation();
    const alerta = await this.alertController.create({
      header: 'Excluir lançamento?',
      message: `${lancamento.tipo} de ${this.formatarData(lancamento.data)}${lancamento.valor ? ' — R$ ' + lancamento.valor.toFixed(2) : ''}. Essa ação não pode ser desfeita.`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: 'Excluir', role: 'destructive', handler: () => this.excluir(lancamento) },
      ],
    });
    await alerta.present();
  }

  private async excluir(lancamento: any) {
    this.excluindoId = lancamento.id;
    try {
      await this.atualizarDadosService.excluirLancamentoAgora(lancamento.id, this.usuarioLogado.id);
      this.lancamentos = this.lancamentos.filter(l => l.id !== lancamento.id);
      this.avisar('Lançamento excluído.');
    } catch (error) {
      console.warn('[MeusLancamentos] Falha ao excluir lançamento:', error);
      this.avisar(`Não foi possível excluir (${this.atualizarDadosService.descreverErro(error)}). Tente novamente.`);
    } finally {
      this.excluindoId = null;
    }
  }

  private formatarData(data: string): string {
    if (!data) { return 'data desconhecida'; }
    const d = new Date(data);
    return isNaN(d.getTime()) ? data : d.toLocaleDateString('pt-BR');
  }

  private async avisar(mensagem: string) {
    const toast = await this.toastController.create({ message: mensagem, duration: 3500 });
    await toast.present();
  }

  voltar() {
    this.navCtrl.pop();
  }
}
