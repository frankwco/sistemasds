import { Component, Input, OnInit } from '@angular/core';
import { ModalController, AlertController } from '@ionic/angular';
import { Storage } from '@ionic/storage';
import { PontoColetaService } from '../../service/ponto-coleta.service';
import { PesquisarTelefonesComponent } from '../pesquisar-telefones/pesquisar-telefones.component';
import { TodasColetasComponent } from '../todas-coletas/todas-coletas.component';

/** Ponto com os textos de busca já normalizados (minúsculos, sem acento), calculados uma vez só. */
interface EntradaBusca {
  ponto: any;
  nome: string;
  endereco: string;
}

@Component({
  selector: 'app-pesquisar-pontos',
  templateUrl: './pesquisar-pontos.component.html',
  styleUrls: ['./pesquisar-pontos.component.scss'],
})
export class PesquisarPontosComponent implements OnInit {
  /** Quantos pontos entram na tela por vez (o resto vem ao rolar até o fim). */
  private readonly TAMANHO_PAGINA = 30;

  isItemAvailable = true;
  /** Spinner enquanto a lista de pontos é lida do Storage. */
  carregando = true;
  /** Pontos da cidade a coletar (ou todos, com separarPontos) que passam pela busca. */
  items = [];
  /** Pontos da cidade já coletados. */
  pontosColetados = [];
  /** Fatias de `items` e `pontosColetados` que estão desenhadas na tela. */
  visiveis = [];
  coletadosVisiveis = [];
  pontosColetas;
  pontosCidadesColetar = [];
  todosPontos = [];
  @Input() separarPontos: any;

  private baseBusca: EntradaBusca[] = [];
  private filtroNome = '';
  private filtroEndereco = '';
  private limiteItens = this.TAMANHO_PAGINA;
  private limiteColetados = this.TAMANHO_PAGINA;

  constructor(private alertController: AlertController, private pontoColetaService: PontoColetaService,
    private storage: Storage, public modalController: ModalController) {
  }

  ngOnInit() {
    // No ngOnInit (e não no construtor) para o separarPontos já estar preenchido.
    this.initializeItems();
  }

  voltar() {
    this.modalController.dismiss({
      'dismissed': true
    });
  }

  async mostrarMensagemTodosAvisos() {

    let textoAviso = "";
    this.todosPontos.forEach(element => {
      if (element.situacaoAviso) {
        textoAviso += element.nomeRazaoSocial + "<br/><hr/>";
      }
    });

    if (textoAviso.length > 2) {

      const alert = await this.alertController.create({
        cssClass: 'my-custom-class',
        header: "ATENÇÃO: Pontos com aviso, verifique antes de coletar",
        message: textoAviso,
        buttons: [
            {
            text: 'Ciente!!',
            handler: () => {


            }
          }
        ]
      });

      await alert.present();
    }
  }

  async mostrarMensagemAviso(selecionado) {
    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: selecionado.nomeRazaoSocial + " " + selecionado.apelido,
      message: "AVISO: " + selecionado.aviso,
      buttons: [
        {
          text: 'NÃO COLETAR',
          role: 'cancel',
          cssClass: 'secondary',
          handler: (blah) => {
            console.log('Confirm Cancel');
          }
        }, {
          text: 'Coletar!!',
          handler: () => {
            this.modalController.dismiss({
              'dismissed': true
            });
            this.storage.set("pontoColetaSelecionado", selecionado);
          }
        }
      ]
    });

    await alert.present();
  }

  async fechar(selecionado) {
    if (selecionado.situacaoAviso) {
      this.mostrarMensagemAviso(selecionado);

    } else {
      this.modalController.dismiss({
        'dismissed': true
      });
      this.storage.set("pontoColetaSelecionado", selecionado);
    }


  }

  async pesquisarColetasPonto(selecionado) {
    // passar selecionado para a proxima tela e fazer a consulta no banco
    const modal = await this.modalController.create({
      component: TodasColetasComponent,
      componentProps: {
        'idPontoColeta': selecionado.id
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        //console.log("fechou pesquisa coleta")
      });
    return await modal.present();

  }

  async pesquisarTelefones(selecionado) {
    // passar selecionado para a proxima tela e fazer a consulta no banco
    const modal = await this.modalController.create({
      component: PesquisarTelefonesComponent,
      componentProps: {
        'selecionado': selecionado
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("fechou pesquisa coleta")
      });
    return await modal.present();

  }

  async buscarPontosCidades() {
    this.carregando = true;
    // Cede a thread por um instante para o spinner ser pintado antes da leitura pesada.
    await new Promise(resolve => setTimeout(resolve, 50));

    try {
      this.pontosColetas = (await this.storage.get('pontosColeta')) || [];
      const cidadeSelecionada = await this.storage.get('cidadeSelecionada');

      if (cidadeSelecionada != null) {
        this.pontosCidadesColetar = [];
        this.todosPontos = [];
        const pontosCidadesColetados = [];
        //NESSE MÉTODO MARCAR OS PONTOS QUE FORAM COLETADOS, DE ACORDO COM OS PONTOS DA LISTA DE COLETAS

        let dt = new Date();

        let dia = dt.getDate(), mes = dt.getMonth() + 1, ano = dt.getFullYear();

        for (let x = 0; x < this.pontosColetas.length; x++) {
          const ponto = this.pontosColetas[x];

          // Ignora pontos inativos ou sem contrato ativo
          if (ponto.status == 0 || ponto.contratoAtivo == 0) {
            continue;
          }

          if (ponto.idCidade == cidadeSelecionada.id) {
            // Coleta Única: uma vez coletada, fica marcada até o contrato ser renovado
            // (que zera diaUltimaColeta/mesUltimaColeta/anoUltimaColeta no backend),
            // não apenas no dia em que a coleta ocorreu.
            const jaColetado = ponto.periodicidadeColeta === 'Coleta Única'
              ? !!(ponto.diaUltimaColeta && ponto.mesUltimaColeta && ponto.anoUltimaColeta)
              : (dia == ponto.diaUltimaColeta && mes == ponto.mesUltimaColeta && ano == ponto.anoUltimaColeta);

            if (jaColetado) {
              pontosCidadesColetados.push(ponto);
            } else {
              this.pontosCidadesColetar.push(ponto);
            }
            this.todosPontos.push(ponto);
          }
        }

        // Verificar o histórico de exibições (sem await: não segura a lista)
        this.storage.get('avisosExibidos').then((avisos) => {
          let avisosHoje = avisos ? avisos[cidadeSelecionada.id] || [] : [];

          avisosHoje = avisosHoje.filter(aviso => {
              let data = new Date(aviso);
              return data.getDate() === dia && data.getMonth() + 1 === mes && data.getFullYear() === ano;
          });

          if (avisosHoje.length < 2) {
              this.mostrarMensagemTodosAvisos();

              // Atualizar o histórico de exibições
              if (!avisos) {
                  avisos = {};
              }
              if (!avisos[cidadeSelecionada.id]) {
                  avisos[cidadeSelecionada.id] = [];
              }
              avisos[cidadeSelecionada.id].push(new Date());
              this.storage.set('avisosExibidos', avisos);
          }
        });

        this.pontosColetados = pontosCidadesColetados;
        this.montarBusca();
        this.aplicarFiltros();
      }
    } finally {
      this.carregando = false;
    }
  }

  initializeItems() {
    this.buscarPontosCidades();
  }

  /** Texto minúsculo e sem acento, para a busca ignorar maiúsculas e acentuação. */
  private normalizar(texto: any): string {
    return String(texto || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }

  /** Lista base da busca (todos os pontos ou só os a coletar) com os textos já normalizados. */
  private montarBusca() {
    const base = this.separarPontos ? this.todosPontos : this.pontosCidadesColetar;
    this.baseBusca = base.map(ponto => ({
      ponto,
      // O separador impede que o fim de um campo e o começo do próximo formem um falso acerto.
      nome: this.normalizar([ponto.nomeRazaoSocial, ponto.nomeFantasia, ponto.apelido].join('\n')),
      endereco: this.normalizar(ponto.enderecoCompleto),
    }));
  }

  /** Aplica as duas buscas juntas e volta para a primeira página. */
  private aplicarFiltros() {
    const nome = this.normalizar(this.filtroNome).trim();
    const endereco = this.normalizar(this.filtroEndereco).trim();

    this.items = this.baseBusca
      .filter(e => (!nome || e.nome.indexOf(nome) > -1) && (!endereco || e.endereco.indexOf(endereco) > -1))
      .map(e => e.ponto);

    this.limiteItens = this.TAMANHO_PAGINA;
    this.limiteColetados = this.TAMANHO_PAGINA;
    this.visiveis = this.items.slice(0, this.limiteItens);
    this.coletadosVisiveis = this.pontosColetados.slice(0, this.limiteColetados);
  }

  trackByPonto(_idx: number, ponto: any) {
    return ponto.id;
  }

  getItemsEndereco(ev: any) {
    this.filtroEndereco = ev.target.value || '';
    this.aplicarFiltros();
  }

  getItems(ev: any) {
    this.filtroNome = ev.target.value || '';
    this.aplicarFiltros();
  }

  /** Rolou até o fim: desenha mais pontos a coletar e, esgotados, mais pontos coletados. */
  carregarMais(evento: any) {
    if (this.visiveis.length < this.items.length) {
      this.limiteItens += this.TAMANHO_PAGINA;
      this.visiveis = this.items.slice(0, this.limiteItens);
    } else {
      this.limiteColetados += this.TAMANHO_PAGINA;
      this.coletadosVisiveis = this.pontosColetados.slice(0, this.limiteColetados);
    }
    evento.target.complete();
    evento.target.disabled = !this.temMais;
  }

  get temMais(): boolean {
    return this.visiveis.length < this.items.length || this.coletadosVisiveis.length < this.pontosColetados.length;
  }
}
