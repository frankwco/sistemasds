import { Component, ViewChildren, QueryList, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Platform, IonRouterOutlet, ToastController } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { AngularFireAuth } from "@angular/fire/auth";
import { AtualizarDadosService } from './api/atualizar-dados.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss']
})
export class AppComponent {

  lastTimeBackPress = 0;
  timePeriodToExit = 2000;

  @ViewChildren(IonRouterOutlet) routerOutlets: QueryList<IonRouterOutlet>;
  constructor(
    public angularFireAuth: AngularFireAuth,
    private platform: Platform,
    private splashScreen: SplashScreen,
    private statusBar: StatusBar,
    public router: Router,
    public toastCtrl: ToastController,
    private atualizarDadosService: AtualizarDadosService,
  ) {
    this.statusBar.backgroundColorByHexString('#2dd36f');
    this.initializeApp();

    this.backButtonEvent();
    this.angularFireAuth.signInWithEmailAndPassword("frankwco@gmail.com", "gwh28!@#").then(a => {
      //console.log("logou no fb");
    });

    // Ficam aqui (componente raiz, vive durante toda a sessão do app) e não
    // na Tab1Page: coleta/dados-pontos/cliente/etc. são rotas de nível raiz,
    // fora da estrutura de tabs — navegar até elas destrói a Tab1Page de
    // verdade (ngOnDestroy), o que derrubava o timer de segurança e a
    // re-sincronização de "voltou do background" bem durante a coleta em
    // campo, que é quando mais precisava deles.
    this.atualizarDadosService.startAutoSync();

    // Adiado para não competir com a abertura do app (apaga chaves grandes do Storage).
    this.platform.ready().then(() => {
      setTimeout(() => this.atualizarDadosService.limparHistoricoColetasAntigo(), 10000);
    });
    this.platform.resume.subscribe(() => {
      console.log('[Sync] App voltou do background — sincronizando tudo...');
      this.atualizarDadosService.atualizar(true, 'sim');
    });

    // Sincroniza assim que a internet voltar, em vez de esperar o próximo
    // ciclo do timer (até 2min) — importante para exclusões/coletas feitas
    // offline serem confirmadas no backend o quanto antes.
    window.addEventListener('online', () => {
      console.log('[Sync] Conexão restabelecida — sincronizando tudo...');
      this.atualizarDadosService.atualizar(true, 'sim');
    });
  }

  backButtonEvent() {
    this.platform.backButton.subscribeWithPriority(0, () => {
      this.routerOutlets.forEach(async (outlet: IonRouterOutlet) => {
        if (this.router.url != '/tabs/tab1') {
          await this.router.navigate(['/tabs/tab1']);
        } else if (this.router.url === '/tabs/tab1') {
          //  alert(new Date().getTime() - this.lastTimeBackPress + " - " + this.timePeriodToExit);
          if (new Date().getTime() - this.lastTimeBackPress < this.timePeriodToExit) {
            navigator['app'].exitApp(); //Exit from app
          } else {
            //navigator['app'].exitApp();
            //ESTE CÓDIGO ABAIXO EXIGE QUE O USUÁRIO APERTE DUAS VEZES NO BACKBUTTON PARA SAIR DO APP.
            //É NECESSÁRIO RETIRAR A LINHA ACIMA TAMBÉM
            let toast = await this.toastCtrl.create({
              message: 'Pressione novamente para sair do APP',
              duration: 3000,
              position: 'bottom'
            });
            toast.present();
            this.lastTimeBackPress = new Date().getTime();
          }
        }
      });
    });
  }



  initializeApp() {
    this.platform.ready().then(() => {
      this.statusBar.styleDefault();
      this.splashScreen.hide();
    });
  }
}
