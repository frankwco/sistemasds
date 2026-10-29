import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { DadosPontosService } from '../api/dados-pontos.service';
import { PesquisarTelefonesComponent } from '../coleta/pesquisar-telefones/pesquisar-telefones.component';
import { TodasColetasComponent } from '../coleta/todas-coletas/todas-coletas.component';


@Component({
  selector: 'app-pontos-sem-coleta',
  templateUrl: './pontos-sem-coleta.page.html',
  styleUrls: ['./pontos-sem-coleta.page.scss'],
})
export class PontosSemColetaPage implements OnInit {

  mes = "" + (new Date().getMonth() + 1);
  ano = "" + (new Date().getFullYear());
  itens = [];
  buscando = false;

  constructor(private dadosPontosService: DadosPontosService, public modalController: ModalController) { }

  ngOnInit() {
    this.buscando = true;
    this.dadosPontosService.diasSemColetaPontos().subscribe(
      data => {
        this.itens = data;
        this.buscando = false;
      },
      error => {
        this.buscando = false;
        alert("Erro")
      });

  }

  consultar() {
    alert("em construção")

    /* this.dadosPontosService.semColetasMesAno(this.mes, this.ano).subscribe(
      data => {
        this.itens = data;
      },
      error => {
        alert("Erro ao realizar consulta")
      }); */


  }

  async pesquisarColetasPonto(id) {
    // passar selecionado para a proxima tela e fazer a consulta no banco   
    const modal = await this.modalController.create({
      component: TodasColetasComponent,
      componentProps: {
        'idPontoColeta': id
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("fechou pesquisa coleta");
      });
    return await modal.present();

  }

  async pesquisarTelefones(id) {
    // passar selecionado para a proxima tela e fazer a consulta no banco   
    const modal = await this.modalController.create({
      component: PesquisarTelefonesComponent,
      componentProps: {
        'selecionado': id
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("fechou pesquisa coleta")
      });
    return await modal.present();

  }

}
