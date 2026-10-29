import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ToastController, AlertController, Platform } from '@ionic/angular';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

/**
 * Cobrança por WhatsApp em campo — mesma lógica de base/cobranca.xhtml (web), sem a parte de
 * importação de CSV/disparo de e-mail em massa, que continuam exclusivas do backoffice web.
 * Só aparece pra quem tem usuario.acessoSistema=true (ver tab1.page.html) — o backend também
 * confere isso a cada chamada.
 */
@Component({
  selector: 'app-cobranca',
  templateUrl: './cobranca.page.html',
  styleUrls: ['./cobranca.page.scss'],
})
export class CobrancaPage implements OnInit {

  usuarioLogado: any = { id: null, nome: '' };
  resumo: any = null;
  cobrancas: any[] = [];
  carregando = false;
  expandidoId: number = null;
  anterioresVisiveis: { [clienteId: number]: boolean } = {};

  // chave "clienteId-numeroTelefone" da ação em andamento, pra travar só o botão certo
  confirmandoTelefone: string = null;
  enviandoTelefone: string = null;

  constructor(
    private storage: Storage,
    private navCtrl: NavController,
    private toastController: ToastController,
    private alertController: AlertController,
    private platform: Platform,
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
      const dados = await this.atualizarDadosService.buscarCobrancasAgora(this.usuarioLogado.id);
      this.resumo = dados.resumo;
      this.cobrancas = dados.clientes || [];
    } catch (error) {
      console.warn('[Cobranca] Falha ao buscar cobranças:', error);
      this.avisar(`Não foi possível carregar as cobranças (${this.atualizarDadosService.descreverErro(error)}).`);
    } finally {
      this.carregando = false;
    }
  }

  async atualizar(evento?: any) {
    await this.carregar();
    if (evento) { evento.target.complete(); }
  }

  alternarDetalhe(cobranca: any) {
    this.expandidoId = this.expandidoId === cobranca.clienteId ? null : cobranca.clienteId;
  }

  toggleAnteriores(clienteId: number) {
    this.anterioresVisiveis[clienteId] = !this.anterioresVisiveis[clienteId];
  }

  chaveTelefone(clienteId: number, numeroTelefone: number): string {
    return `${clienteId}-${numeroTelefone}`;
  }

  /** Notificação mais recente (rodada atual, ou a última de rodadas anteriores se ainda não
   * teve nenhuma nesta), pra mostrar direto no card sem precisar abrir "ver mais". */
  ultimoHistorico(cobranca: any): any {
    if (cobranca.historicoAtual && cobranca.historicoAtual.length > 0) {
      return cobranca.historicoAtual[cobranca.historicoAtual.length - 1];
    }
    if (cobranca.historicoAnterior && cobranca.historicoAnterior.length > 0) {
      return cobranca.historicoAnterior[cobranca.historicoAnterior.length - 1];
    }
    return null;
  }

  async confirmarOuDesmarcarWhatsapp(cobranca: any, numeroTelefone: number) {
    const confirmado = numeroTelefone === 1 ? cobranca.telefone1Whatsapp : cobranca.telefone2Whatsapp;
    const alerta = await this.alertController.create({
      header: confirmado ? 'Desfazer confirmação?' : 'Confirmar WhatsApp?',
      message: confirmado
        ? 'Desfazer a confirmação deste número como WhatsApp?'
        : 'Confirmar que este número é WhatsApp?',
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        { text: confirmado ? 'Desfazer' : 'Confirmar', handler: () => this.alterarWhatsapp(cobranca, numeroTelefone, !confirmado) },
      ],
    });
    await alerta.present();
  }

  private async alterarWhatsapp(cobranca: any, numeroTelefone: number, confirmar: boolean) {
    const chave = this.chaveTelefone(cobranca.clienteId, numeroTelefone);
    this.confirmandoTelefone = chave;
    try {
      const atualizado = confirmar
        ? await this.atualizarDadosService.confirmarWhatsappAgora(this.usuarioLogado.id, cobranca.clienteId, numeroTelefone)
        : await this.atualizarDadosService.desmarcarWhatsappAgora(this.usuarioLogado.id, cobranca.clienteId, numeroTelefone);
      this.substituir(atualizado);
    } catch (error) {
      console.warn('[Cobranca] Falha ao confirmar/desmarcar WhatsApp:', error);
      this.avisar(`Não foi possível salvar (${this.atualizarDadosService.descreverErro(error)}). Tente novamente.`);
    } finally {
      this.confirmandoTelefone = null;
    }
  }

  async enviarWhatsapp(cobranca: any, numeroTelefone: number) {
    const telefone = numeroTelefone === 1 ? cobranca.telefone1 : cobranca.telefone2;
    if (!telefone) { return; }

    const confirmado = numeroTelefone === 1 ? cobranca.telefone1Whatsapp : cobranca.telefone2Whatsapp;
    if (!confirmado) {
      const alerta = await this.alertController.create({
        header: 'Número ainda não confirmado',
        message: 'Esse número ainda não foi confirmado como WhatsApp. Confirme antes de enviar, caso seja WhatsApp mesmo.',
        buttons: [
          { text: 'Cancelar', role: 'cancel' },
          { text: 'Confirmar e enviar', handler: () => this.confirmarEEnviar(cobranca, numeroTelefone) },
        ],
      });
      await alerta.present();
      return;
    }

    this.abrirWhatsapp(cobranca, numeroTelefone);
  }

  private async confirmarEEnviar(cobranca: any, numeroTelefone: number) {
    await this.alterarWhatsapp(cobranca, numeroTelefone, true);
    this.abrirWhatsapp(cobranca, numeroTelefone);
  }

  private abrirWhatsapp(cobranca: any, numeroTelefone: number) {
    const telefone = numeroTelefone === 1 ? cobranca.telefone1 : cobranca.telefone2;

    if (!this.platform.is('cordova')) {
      this.avisar('Enviar WhatsApp só funciona no app instalado no celular.');
      return;
    }

    const chave = this.chaveTelefone(cobranca.clienteId, numeroTelefone);
    this.enviandoTelefone = chave;
    const numeroNormalizado = this.normalizarTelefoneWhatsapp(telefone);

    window['plugins'].socialsharing.shareViaWhatsAppToPhone(
      numeroNormalizado,
      cobranca.mensagemWhatsapp,
      null, null,
      () => {
        this.enviandoTelefone = null;
        // só registra no histórico se de fato abriu o WhatsApp (mesma regra do web:
        // CobrancaMB.registrarEnvioWhatsapp só grava se o telefone já estava confirmado)
        this.atualizarDadosService.registrarEnvioWhatsappAgora(this.usuarioLogado.id, cobranca.clienteId, numeroTelefone)
          .then(atualizado => this.substituir(atualizado))
          .catch(error => console.warn('[Cobranca] Falha ao registrar envio de WhatsApp:', error));
      },
      () => { this.enviandoTelefone = null; }
    );
  }

  /** Mesma regra de util.cobranca.WhatsappUtil.normalizarNumero, do backend: só dígitos, com o
   * 55 do Brasil na frente quando o telefone só tem DDD+número (10 ou 11 dígitos). */
  private normalizarTelefoneWhatsapp(telefone: string): string {
    let digitos = (telefone || '').replace(/\D/g, '');
    if (digitos.length === 10 || digitos.length === 11) {
      digitos = '55' + digitos;
    }
    return '+' + digitos;
  }

  private substituir(atualizado: any) {
    if (!atualizado) { return; }
    const indice = this.cobrancas.findIndex(c => c.clienteId === atualizado.clienteId);
    if (indice >= 0) { this.cobrancas[indice] = atualizado; }
  }

  private async avisar(mensagem: string) {
    const toast = await this.toastController.create({ message: mensagem, duration: 3500 });
    await toast.present();
  }

  voltar() {
    this.navCtrl.pop();
  }
}
