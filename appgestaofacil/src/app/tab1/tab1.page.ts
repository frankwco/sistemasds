import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AtualizarDadosService } from '../api/atualizar-dados.service';
import { Storage } from '@ionic/storage';
import { VeiculosComponent } from './veiculos/veiculos.component';
import { MotoristasComponent } from './motoristas/motoristas.component';
import { ConstantesApp } from '../util/constanteApp';
import { Platform, NavController, ToastController, AlertController, ModalController } from '@ionic/angular';
import { AngularFirestore } from '@angular/fire/firestore';
import { Observable } from 'rxjs';
//import { auth } from 'firebase/app';
import { AngularFireAuth } from "@angular/fire/auth";

import { DadosPontosPage } from '../dados-pontos/dados-pontos.page';
export interface Item { id: string; name: string; }
@Component({
  selector: 'app-tab1',
  templateUrl: 'tab1.page.html',
  styleUrls: ['tab1.page.scss']
})
export class Tab1Page implements OnInit {
  veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
  motoristaSelecionado = { "id": null, "nome": "", "email": "" };
  log = "";
  private primeiroAcesso = true;
  items = [];
  ii = [];
  itemsFire$: Observable<any[]>;
  usuario = { nome: '' };
  dataAtual = new Date();

  constructor(
    public angularFireAuth: AngularFireAuth,
    private firestore: AngularFirestore,
    public constanteApp: ConstantesApp,
    public router: Router,
    public atualizarDadosService: AtualizarDadosService,
    private storage: Storage,
    private platform: Platform,
    public navCtrl: NavController,
    private alertController: AlertController,
    public modalController: ModalController,

  ) {
    this.storage.get(this.constanteApp.chave).then((val) => {
      if (val == null || val == undefined || val == false) {
        this.navCtrl.navigateRoot('');
      }
    });
  }


  sairFire() {
    this.angularFireAuth.signOut().then(() => {});
  }

  chamar() {
    this.itemsFire$ = this.firestore.collection('users').valueChanges();
  }

  login() {
    this.angularFireAuth.signInWithEmailAndPassword("frankwco@gmail.com", "gwh28!@#").then(a => {});
  }

  ngOnInit() {
    this.storage.get('usuario').then((valUser) => {
      if (valUser != null) {
        this.usuario = valUser;
      }
    });

    this.storage.get('veiculoSelecionado').then((val) => {
      if (val != null) {
        this.veiculoSelecionado = val;
      } else {
        this.veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
      }
    });

    this.storage.get('motoristaSelecionado').then((val) => {
      if (val != null) {
        this.motoristaSelecionado = val;
      } else {
        this.motoristaSelecionado = { "id": null, "nome": "", "email": "" };
      }
    });
  }

  ionViewWillEnter() {
    if (this.primeiroAcesso) {
      // Primeiro acesso: sincroniza tudo (coletas + pontos + dados de referência)
      this.primeiroAcesso = false;
      this.atualizarDadosService.atualizar(true, 'nao');
    } else {
      // Retornou à aba: envia coletas pendentes + atualiza pontos incrementalmente
      this.atualizarDadosService.syncColetasSafe(true, 'sim');
    }

    // O timer de segurança (startAutoSync) e a re-sincronização de "voltou do
    // background" rodam a partir do AppComponent (raiz, vive por toda a
    // sessão do app) — não daqui. coleta/dados-pontos/cliente/etc. são rotas
    // de nível raiz que destroem esta página de verdade ao navegar, então
    // mantê-los aqui os derrubava bem durante a coleta em campo.
  }

  async sair() {

    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Confirmar!',
      message: 'Deseja <strong>SAIR</strong> do APP??',
      buttons: [
        {
          text: 'Não',
          role: 'cancel',
          cssClass: 'secondary',
          handler: (blah) => {
            console.log('Confirm Cancel: blah');
          }
        }, {
          text: 'Sim',
          handler: () => {

            this.storage.set(this.constanteApp.chave, false);
            this.storage.remove("usuario");
            this.navCtrl.navigateRoot('');
          }
        }
      ]
    });

    await alert.present();


  }
  informacoesPonto() {
    this.router.navigate(['dados-pontos']);
  }


  chamarColeta() {
    if (this.veiculoSelecionado.id != null && this.motoristaSelecionado.id != null) {
      this.atualizarDadosService.syncColetasSafe(true, 'sim');
      this.router.navigate(['coleta']);
    } else {
      alert("Selecionar o VEÍCULO e depois o MOTORISTA!");
    }
  }



  async pesquisarVeiculo() {

    const modal = await this.modalController.create({
      component: VeiculosComponent
    });
    modal.onDidDismiss()
      .then(() => {
        this.storage.get('veiculoSelecionado').then((val) => {
          if (val != null) {
            this.veiculoSelecionado = val;
          } else {
            this.veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
          }
        });
      });
    return await modal.present();
  }

  async pesquisarMotorista() {
    const modal = await this.modalController.create({
      component: MotoristasComponent
    });
    modal.onDidDismiss()
      .then(() => {
        this.storage.get('motoristaSelecionado').then((val) => {
          if (val != null) {
            this.motoristaSelecionado = val;
          } else {
            this.motoristaSelecionado = { "id": null, "nome": "", "email": "" };
          }
        });
      });
    return await modal.present();
  }

  async chamarDadosPontos() {
    this.router.navigate(['dados-pontos']);

/*     const modal = await this.modalController.create({
      component: DadosPontosPage
    });
    modal.onDidDismiss()
      .then((data) => {
        console.log("Fechou")
      });
    return await modal.present(); */
  }

  chamarPagina(pagina){
    this.router.navigate([pagina]);
  }


  chamarVeiculo() {
    this.router.navigate(['coleta']);
  }

  pontosSemColetas() {
    this.router.navigate(['pontos-sem-coleta']);
  }

  pontosComAvisos() {
    this.router.navigate(['pontos-com-avisos']);
  }

  chamarCliente() {
    this.router.navigate(['cliente']);
  }

  chamarFechamentoMes() {
    this.router.navigate(['fechamento-mes']);
  }

  chamarContrato() {
    this.router.navigate(['contrato']);
  }
}
