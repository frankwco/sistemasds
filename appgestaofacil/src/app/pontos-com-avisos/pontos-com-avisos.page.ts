import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { ModalController } from '@ionic/angular';
import { TodasColetasComponent } from '../coleta/todas-coletas/todas-coletas.component';

@Component({
  selector: 'app-pontos-com-avisos',
  templateUrl: './pontos-com-avisos.page.html',
  styleUrls: ['./pontos-com-avisos.page.scss'],
})
export class PontosComAvisosPage implements OnInit {

  pontosColetas = [];

  constructor(private storage: Storage, public modalController: ModalController) { }

  ngOnInit() {
    
    this.storage.get('pontosColeta').then((vals) => {
      //console.log("Quantidade Pontos:: "+vals.length);
      vals.forEach(element => {
        //console.log(element.situacaoAviso)
       /*  if(element.nomeRazaoSocial=='DENISE DE CARVALHO KIMURA'){
          //console.log(element);
        } */
        if (element.situacaoAviso) {
          this.pontosColetas.push(element);
        }
      });
    });
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



}
