import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { PesquisarPontosComponent } from '../coleta/pesquisar-pontos/pesquisar-pontos.component';
import { PesquisarCidadesComponent } from '../coleta/pesquisar-cidades/pesquisar-cidades.component';
import { Storage } from '@ionic/storage';
//import { Validators, FormBuilder, FormGroup } from '@angular/forms';
import { MenuController, NavController, AlertController, Platform } from '@ionic/angular';
import { AtualizarDadosService } from '../api/atualizar-dados.service';
import { DadosPontosService } from '../api/dados-pontos.service';
import { TodasColetasComponent } from '../coleta/todas-coletas/todas-coletas.component';
import { PesquisarTelefonesComponent } from '../coleta/pesquisar-telefones/pesquisar-telefones.component';

@Component({
  selector: 'app-dados-pontos',
  templateUrl: './dados-pontos.page.html',
  styleUrls: ['./dados-pontos.page.scss'],
})
export class DadosPontosPage implements OnInit {
  cidadeSelecionada={"nome":'Selecione'};
  pontoColetaSelecionado = {"id":"", "nomeCidade": "", "nomeRazaoSocial": "Selecione", "gruposColetados": "", "nomeFantasia": "", "apelido": "" };
  pontosColetas = [];
  pontosColetados = [];
  coleta;
  usuario;
  veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
  motoristaSelecionado = { "id": null, "nome": "", "email": "" };
  urlWhats: string = "aaa";
  proximaColeta={"dataProximaColeta":"","mensagem":"", "pontoColeta":""};


  constructor(
    public platform: Platform,
    private navCtrl: NavController,
    private atualizarDadosService: AtualizarDadosService,
    private dadosPontosService: DadosPontosService,
    private menu: MenuController,
    private storage: Storage,
    private alertController: AlertController,
    public modalController: ModalController
  ) {



  

  }

  mostrar() {
    if (this.platform.is("cordova")) {
      window['plugins'].socialsharing.shareViaWhatsAppToPhone('+554499323410', this.pontoColetaSelecionado.nomeRazaoSocial + "/" + this.pontoColetaSelecionado.nomeFantasia + " - " + this.pontoColetaSelecionado.nomeCidade, null /* img */, null /* url */, function () { console.log('share ok') });
    }
  }

 

  ngOnInit() {
    //this.buscarPontos();
 

    this.storage.get('veiculoSelecionado').then((val) => {
      if (val != null) {
        this.veiculoSelecionado = val;
        //console.log(val);
      } else {
        this.veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
      }
    });

    this.storage.get('motoristaSelecionado').then((val) => {
      if (val != null) {
        this.motoristaSelecionado = val;
        //console.log(val);
      } else {
        this.motoristaSelecionado = { "id": null, "nome": "", "email": "" };
      }
    });

    this.storage.get('cidadeSelecionada').then((val) => {
      if (val != null) {
        this.cidadeSelecionada = val;
      } else {
        this.cidadeSelecionada={"nome":'Selecione'};
      }
    });

    this.storage.get('usuario').then((val) => {
      this.usuario = val;
    });


  }

  buscarPontos() {
    this.storage.get('pontosColeta').then((val) => {
      this.pontosColetas = val;
    
    });
  }

  async pesquisarColetasPonto() {
    // passar selecionado para a proxima tela e fazer a consulta no banco   
    const modal = await this.modalController.create({
      component: TodasColetasComponent,
      componentProps: {
        'idPontoColeta': this.pontoColetaSelecionado.id
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("fechou pesquisa coleta")
      });
    return await modal.present();

  }

  async pesquisarTelefones() {
    // passar selecionado para a proxima tela e fazer a consulta no banco   
    const modal = await this.modalController.create({
      component: PesquisarTelefonesComponent ,
      componentProps: {
        'selecionado': this.pontoColetaSelecionado
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("fechou pesquisa coleta")
      });
    return await modal.present();

  }


  async pesquisarPontos() {
    //this.buscarPontosCidade();
    const modal = await this.modalController.create({
      component: PesquisarPontosComponent,
      componentProps: {
        'separarPontos': true
      }
    });
    modal.onDidDismiss()
      .then((data) => {
        this.storage.get('pontoColetaSelecionado').then((val) => {
          // console.log("VALO: "+val)
          if (val) {
            // console.log("AAAAAA")
            this.pontoColetaSelecionado = val;
            this.dadosPontosService.proximaColeta(this.pontoColetaSelecionado.id).subscribe(
              data => {    
               this.proximaColeta = data;
      
              },
              error => {
                alert("Erro")
              });
            //this.coleta.pontoColetaSelecionado = this.pontoColetaSelecionado;
            //console.log(val);
          }
        });

      });
    return await modal.present();
  }

  buscarPontosCidade() {
    this.storage.get('cidadeSelecionada').then((val) => {

      //console.log("cidade: "+this.cidadeSelecionada)
      if (val != null) {
        this.cidadeSelecionada = val;
        let pontosCidadesColetar = [];
        let pontosCidadesColetados = [];
       
      }
    });
  }



  async pesquisarCidades() {

    const modal = await this.modalController.create({
      component: PesquisarCidadesComponent
     
    });

    modal.onDidDismiss()
      .then((data) => {
        this.buscarPontosCidade();
      });
    return await modal.present();
  }



  


}
