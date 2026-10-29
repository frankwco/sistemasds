import { Component } from '@angular/core';
import { Storage } from '@ionic/storage';
import { VeiculosComponent } from './veiculos/veiculos.component';
import { ModalController } from '@ionic/angular';
import { GrupoColeta, gruposDaColeta, formatarPeso, instanteDaColeta } from '../util/coleta-lista';

/** Linha da lista já calculada (grupos, textos, instante), para o template não refazer isso a cada ciclo. */
interface ItemColeta {
  /** Chave do trackBy: identificadorApp, ou a posição quando a coleta não tem. */
  chave: string;
  titulo: string;
  fantasia: string;
  apelido: string;
  cidade: string;
  sincronizado: boolean;
  grupos: GrupoColeta[];
  total: string;
  data: string;
  email: string;
  veiculo: string;
  /** Instante da coleta (ms), para ordenar da mais recente para a mais antiga. */
  instante: number;
}

@Component({
  selector: 'app-tab2',
  templateUrl: 'tab2.page.html',
  styleUrls: ['tab2.page.scss']
})
export class Tab2Page {
  /** Quantas coletas entram na tela por vez (o resto vem ao rolar até o fim). */
  private readonly TAMANHO_PAGINA = 30;

  /** Todas as coletas ativas (do veículo filtrado), da mais recente para a mais antiga. */
  private todos: ItemColeta[] = [];
  /** Fatia de `todos` que está desenhada na tela. */
  visiveis: ItemColeta[] = [];
  private limite = this.TAMANHO_PAGINA;
  /** Spinner só na primeira carga; nas seguintes a lista antiga fica até a nova estar pronta. */
  carregando = true;
  currentSelected;
  veiculoSelecionado;
  usuario;
  constructor(private storage: Storage,
    public modalController: ModalController) {
    this.veiculoSelecionado = { "id": null, "descricao": "Todos", "placa": "" };
    this.usuario = { 'acessoTotal': false };
  

  }

  ionViewWillEnter() {
    this.storage.get('usuario').then((valUser) => {
      this.usuario = valUser || { 'acessoTotal': false };
      this.initializeItems();
    });

  }

  async initializeItems() {
    if (this.todos.length === 0) {
      this.carregando = true;
      // Cede a thread por um instante para o spinner ser pintado antes da leitura pesada.
      await new Promise(resolve => setTimeout(resolve, 50));
    }
    try {
      const val: any[] = (await this.storage.get('coletas')) || [];
      const todosVeiculos = this.veiculoSelecionado.descricao == 'Todos';
      this.todos = val
        .filter(c => c.status && (todosVeiculos || (c.veiculo && c.veiculo.placa == this.veiculoSelecionado.placa)))
        .map((c, i) => this.montarItem(c, i))
        .sort((x, y) => y.instante - x.instante);
      this.limite = this.TAMANHO_PAGINA;
      this.visiveis = this.todos.slice(0, this.limite);
    } finally {
      this.carregando = false;
    }
  }

  private montarItem(coleta: any, posicao: number): ItemColeta {
    const ponto = coleta.pontoColetaSelecionado || {};
    return {
      chave: coleta.identificadorApp ? String(coleta.identificadorApp) : 'sem-id-' + posicao,
      titulo: ponto.nomeRazaoSocial,
      fantasia: ponto.nomeFantasia,
      apelido: ponto.apelido,
      cidade: ponto.cidade ? ponto.cidade.nome : '',
      sincronizado: !!coleta.sincronizado,
      grupos: gruposDaColeta(coleta),
      total: formatarPeso(coleta.totalColetado),
      data: this.formatarDataHora(coleta.dataUltimaColeta),
      email: coleta.emailUsuario,
      veiculo: coleta.veiculo ? coleta.veiculo.descricao : '',
      instante: instanteDaColeta(coleta),
    };
  }

  trackByColeta(_idx: number, item: ItemColeta) {
    return item.chave;
  }

  /** Rolou até o fim: desenha a próxima página. */
  carregarMais(evento: any) {
    this.limite += this.TAMANHO_PAGINA;
    this.visiveis = this.todos.slice(0, this.limite);
    evento.target.complete();
    evento.target.disabled = this.visiveis.length >= this.todos.length;
  }

  get temMais(): boolean {
    return this.visiveis.length < this.todos.length;
  }

  get total(): number {
    return this.todos.length;
  }

    formatarDataHora(data) {
      //const data = '2023-05-24T11:34:54+03:00';
      const dataObj = new Date(data);
      const fusoHorario = dataObj.getTimezoneOffset() / 30; // Calcula o deslocamento de fuso horário em horas
      dataObj.setHours(dataObj.getHours() + fusoHorario); // Ajusta a hora de acordo com o deslocamento de fuso horário
      const dataFormatada = `${('0' + dataObj.getDate()).slice(-2)}/${('0' + (dataObj.getMonth() + 1)).slice(-2)}/${dataObj.getFullYear()} ${('0' + dataObj.getHours()).slice(-2)}:${('0' + dataObj.getMinutes()).slice(-2)}`;
      //console.log(data+" está ficando "+dataFormatada);
      

return dataFormatada;
    }
    

  async pesquisarVeiculo() {

    const modal = await this.modalController.create({
      component: VeiculosComponent
    });
    modal.onDidDismiss()
      .then((data) => {
        this.storage.get('veiculoPesquisaSelecionado').then((val) => {
          //this.atualizarDadosService.enviarColetas(false,'sim');
          if (val != null) {
            this.veiculoSelecionado = val;
            //console.log(val);
          } else {
            this.veiculoSelecionado = { "id": null, "descricao": "Todos", "placa": "" };
          }
          this.initializeItems();
        });
      });
    return await modal.present();
  }

}
