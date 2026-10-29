import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ModalController, AlertController, ToastController } from '@ionic/angular';
import { Storage } from '@ionic/storage';
import { PontoColetaService } from '../../service/ponto-coleta.service';
import { AtualizarDadosService } from '../../api/atualizar-dados.service';
import { AngularFirestore, AngularFirestoreCollection } from '@angular/fire/firestore';
import { GrupoColeta, gruposDaColeta, formatarPeso, instanteDaColeta } from '../../util/coleta-lista';

/**
 * Linha da lista já com tudo calculado (grupos, textos, instante da coleta), para o
 * template não refazer isso a cada ciclo de detecção de mudanças em cada coleta.
 */
interface ItemColeta {
  /** Chave do trackBy: identificadorApp, ou a posição quando a coleta não tem. */
  chave: string;
  coleta: any;
  titulo: string;
  sincronizado: boolean;
  grupos: GrupoColeta[];
  total: string;
  data: string;
  /** Nome do ponto em minúsculas, para a busca. */
  busca: string;
  /** Instante da coleta (ms), para ordenar da mais recente para a mais antiga. */
  instante: number;
}

@Component({
  selector: 'app-pesquisar-coletas',
  templateUrl: './pesquisar-coletas.component.html',
  styleUrls: ['./pesquisar-coletas.component.scss'],
})
export class PesquisarColetasComponent implements OnInit {

  /** Quantas coletas entram na tela por vez (o resto vem ao rolar até o fim). */
  private readonly TAMANHO_PAGINA = 30;

  isItemAvailable = true;
  /** Todas as coletas ativas, da mais recente para a mais antiga. */
  private allItems: ItemColeta[] = [];
  /** allItems depois do filtro da busca. */
  private filtrados: ItemColeta[] = [];
  /** Fatia de `filtrados` que está desenhada na tela. */
  visiveis: ItemColeta[] = [];
  private limite = this.TAMANHO_PAGINA;
  /** Mostra o spinner enquanto a lista é lida do Storage. */
  carregando = true;
  /** Overlay "Excluindo..." enquanto a exclusão é gravada. */
  excluindo = false;
  currentSelected;
  private colectionFirestore: AngularFirestoreCollection<any>;


  constructor(
    private firestore: AngularFirestore,
    private pontoColetaService: PontoColetaService,
    private alertController: AlertController,
    private toastController: ToastController,
    private storage: Storage,
    private atualizarDadosService: AtualizarDadosService,
    public modalController: ModalController) {

    this.initializeItems();
  }



  ngOnInit() { }

  fechar() {
    this.modalController.dismiss({
      'dismissed': true
    });
  }

  async initializeItems() {
    this.carregando = true;
    this.allItems = [];
    this.filtrados = [];
    this.visiveis = [];
    await this.deixarTelaPintar();
    try {
      const val: any[] = (await this.storage.get('coletas')) || [];
      this.allItems = val
        .filter(c => c.status)
        .map((c, i) => this.montarItem(c, i))
        .sort((x, y) => y.instante - x.instante);
      this.aplicarFiltro('');
    } finally {
      this.carregando = false;
    }
  }

  /** Cede a thread principal por um instante para o spinner/overlay ser pintado antes do trabalho pesado. */
  private deixarTelaPintar(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, 50));
  }

  trackByColeta(_idx: number, item: ItemColeta) {
    return item.chave;
  }

  private montarItem(coleta: any, posicao: number): ItemColeta {
    const ponto = coleta.pontoColetaSelecionado || {};
    const cidade = ponto.cidade ? ponto.cidade.nome : '';

    return {
      chave: coleta.identificadorApp ? String(coleta.identificadorApp) : 'sem-id-' + posicao,
      coleta,
      titulo: `${ponto.nomeRazaoSocial}-${ponto.nomeFantasia}-${ponto.apelido}-${cidade}`,
      sincronizado: !!coleta.sincronizado,
      grupos: gruposDaColeta(coleta),
      total: formatarPeso(coleta.totalColetado),
      data: this.formatarData(coleta),
      busca: (ponto.nomeRazaoSocial || '').toLowerCase(),
      instante: instanteDaColeta(coleta),
    };
  }

  private formatarData(coleta: any): string {
    if (coleta.anoUltimaColeta) {
      const dois = (n: number) => String(n || 0).padStart(2, '0');
      return `${dois(coleta.diaUltimaColeta)}/${dois(coleta.mesUltimaColeta)}/${coleta.anoUltimaColeta} ` +
        `${dois(coleta.horaUltimaColeta)}:${dois(coleta.minutoUltimaColeta)}`;
    }
    try {
      return new DatePipe('pt-BR').transform(coleta.dataUltimaColeta, 'dd/MM/yyyy HH:mm') || '';
    } catch {
      return '';
    }
  }

  /** Recalcula a lista filtrada e volta para a primeira página. */
  private aplicarFiltro(texto: string) {
    const busca = (texto || '').trim().toLowerCase();
    this.filtrados = busca ? this.allItems.filter(i => i.busca.indexOf(busca) > -1) : this.allItems;
    this.limite = this.TAMANHO_PAGINA;
    this.visiveis = this.filtrados.slice(0, this.limite);
  }

  /** Rolou até o fim: desenha a próxima página. */
  carregarMais(evento: any) {
    this.limite += this.TAMANHO_PAGINA;
    this.visiveis = this.filtrados.slice(0, this.limite);
    evento.target.complete();
    evento.target.disabled = this.visiveis.length >= this.filtrados.length;
  }

  get temMais(): boolean {
    return this.visiveis.length < this.filtrados.length;
  }

  get total(): number {
    return this.filtrados.length;
  }

  formatarMensagemConfirmar(coleta) {
    let mensagem = coleta.pontoColetaSelecionado.nomeRazaoSocial + '??<br/><br/>';
    mensagem += "<strong>";
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('A;')) {
      mensagem += "A: " + coleta.a + "<br/><br/>";
    }
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('E;')) {
      mensagem += "E: " + coleta.e + "<br/><br/>";
    }
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('B;')) {
      mensagem += "B: " + coleta.b + "<br/><br/>";
    }
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('A2;')) {
      mensagem += "A2: " + coleta.a2 + "<br/><br/>";
    }
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('A3;')) {
      mensagem += "A3: " + coleta.a3+ "<br/><br/>";
    }
    if (coleta.pontoColetaSelecionado.gruposColetados.includes('A4;')) {
      mensagem += "A4: " + coleta.a4;
    }
    mensagem += "</strong>"
    return mensagem;
  }

  async confirmarExcluir(item: ItemColeta, idx) {
    this.currentSelected = idx;
    let valor = Math.floor(Math.random() * 10 + 1);
    let valor2 = Math.floor(Math.random() * 10 + 1);
    let resultadoSoma = valor + valor2;

    const alert = await this.alertController.create({
      cssClass: ['my-custom-class', 'confirmar-exclusao-alert'],
      header: 'Confirmação!',
      message: 'Deseja <strong>EXCLUIR</strong> a coleta de ' + this.formatarMensagemConfirmar(item.coleta) +
        '<br/><br/><strong style="color:#eb445a">Qual a soma de ' + valor + ' + ' + valor2 + '?</strong>',
      inputs: [
        {
          name: 'total',
          type: 'number',
          placeholder: 'Insira o resultado'
        }
      ],
      buttons: [
        {
          text: 'CANCELAR',
          role: 'cancel',
          cssClass: 'secondary',
          handler: (blah) => {
            this.currentSelected = -1
          }
        }, {
          text: 'Confirmar',
          handler: (value) => {
            this.currentSelected = -1;
            if (value.total == resultadoSoma) {
              this.excluir(item);
            } else {
              this.presentAlert("O resultado não corresponde. Tente novamente!!");
            }
          }
        }
      ]
    });

    await alert.present();

  }

  presentAlert(msg) {
    alert(msg)
  }

  async excluir(item: ItemColeta) {
    this.excluindo = true;
    await this.deixarTelaPintar();

    // Some da lista na hora; a gravação no Storage acontece por baixo, com o
    // loading na frente (também evita um segundo toque durante a exclusão).
    const coletaExcluida = item.coleta;
    const mesmaColeta = (c: any) => c.identificadorApp == coletaExcluida.identificadorApp;
    this.allItems = this.allItems.filter(i => i !== item);
    this.filtrados = this.filtrados.filter(i => i !== item);
    this.visiveis = this.visiveis.filter(i => i !== item);

    try {
      const pontosParaZerar: any[] = [];

      // Pela fila de gravação de 'coletas': uma sincronização em andamento não
      // pode regravar a lista velha por cima da exclusão (nem o contrário).
      await this.atualizarDadosService.modificarColetas(val => {
        for (const coleta of val) {
          if (mesmaColeta(coleta)) {
            coleta.sincronizado = false;
            coleta.status = false;
            pontosParaZerar.push(coleta.pontoColetaSelecionado.id);
          }
        }
      });

      // Soft-delete: marca status=false em vez de apagar o documento.
      // Isso preserva o rastro no Firestore para que o serviço de
      // reconciliação do backend (consumirFirebase) consiga detectar
      // a remoção e aplicá-la no banco relacional também.
      this.colectionFirestore = this.firestore.collection<any>('coleta').doc('veiculo').collection<any>('' + coletaExcluida.diaUltimaColeta + '-' + coletaExcluida.mesUltimaColeta + '-' + coletaExcluida.anoUltimaColeta);
      this.colectionFirestore.doc("" + coletaExcluida.pontoColetaSelecionado.id).set(
        { status: false, dataExclusaoApp: new Date().toISOString() },
        { merge: true }
      ).catch((error) => {
          console.error('[Sync] Falha ao excluir coleta do Firebase:', error);
        });

      await this.excluirColetaZerarPonto(pontosParaZerar);

      // Só envia a exclusão pendente + pontos incrementais, sem rebaixar os
      // catálogos. Sem await: não segura a tela.
      this.atualizarDadosService.syncColetasSafe(true, 'sim');
      this.excluindo = false;
      this.mostrarToast("Excluído!!");
    } catch (error) {
      console.error('[Coleta] Falha ao excluir coleta localmente:', error);
      this.excluindo = false;
      this.presentAlert("Erro ao excluir a coleta, tente novamente.");
      // Recarrega do Storage para a lista voltar a refletir o que está salvo
      this.initializeItems();
    }
  }

  /** Zera a "última coleta" dos pontos afetados, regravando a lista só se algo mudou. */
  async excluirColetaZerarPonto(idsPontoColeta: any[]) {
    if (idsPontoColeta.length === 0) {
      return;
    }
    const pontos: any[] = (await this.storage.get('pontosColeta')) || [];
    let alterou = false;
    for (const ponto of pontos) {
      if (idsPontoColeta.includes(ponto.id)) {
        ponto.diaUltimaColeta = 0;
        ponto.mesUltimaColeta = 0;
        ponto.anoUltimaColeta = 0;
        alterou = true;
      }
    }
    if (alterou) {
      await this.storage.set('pontosColeta', pontos);
    }
  }

  private async mostrarToast(mensagem: string) {
    const toast = await this.toastController.create({
      message: mensagem,
      duration: 2500,
      position: 'bottom'
    });
    await toast.present();
  }

  sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
  }

  getItems(ev: any) {
    this.aplicarFiltro(ev.target.value);
  }
}
