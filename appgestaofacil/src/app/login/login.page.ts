import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Platform, NavController, ToastController, AlertController } from '@ionic/angular';
import { ConstantesService } from '../api/constantes.service';
import 'rxjs/add/operator/map';
import { Http } from '@angular/http';
import { LoginService } from '../api/login.service';
import { Observable } from 'rxjs/Observable';
import { Storage } from '@ionic/storage';
import { LoadingController } from '@ionic/angular';
import {ConstantesApp} from '../util/constanteApp';


@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrls: ['./login.page.scss'],
})
export class LoginPage implements OnInit {

  //email="frankwco@gmail.com";
  //senha="70235";
  email;
  senha;
  manterConectado = false;
  retorno: Observable<any>;
  loading;
  fecharLoading = 0;3
  constructor(
    private constanteApp:ConstantesApp,
    private loadingController: LoadingController,
    private storage: Storage,
    private alertController: AlertController,
    private loginService: LoginService,
    public toastController: ToastController,
    private constantes: ConstantesService,
    private http: Http,
    public navCtrl: NavController,
    private platform: Platform,    
    private router: Router) {
      

  }

  ngOnInit() {
    this.storage.get(this.constanteApp.chave).then((val) => {
      if (val != null && val != undefined && val == true) {
        this.navCtrl.navigateRoot('tabs');
      } else {
        //alert("REMOVEUUUUUUU")
        // Dados de coleta NÃO são removidos aqui: podem existir coletas (ou
        // exclusões) feitas em campo ainda não sincronizadas com o servidor.
        // Apagá-los ao reabrir o login sem "manter conectado" perdia esse
        // trabalho pendente antes de ele conseguir sincronizar.
        this.storage.remove("veiculoSelecionado");
        this.storage.remove("motoristaSelecionado");
        this.storage.remove("cidades");
        this.storage.remove("veiculos");
        this.storage.remove("cidadeSelecionada");
        this.storage.remove("finalizada");
        this.storage.remove("usuario");
      }
    });
  }

  async delay(ms: number) {
   // await new Promise(resolve => setTimeout(() => resolve(), ms)).then(() => {

      console.log("fired")
   // });
  }

  async apresentar() {
    this.loading = await this.loadingController.create({
      cssClass: 'my-custom-class',
      message: 'Aguarde...'
    });

    await this.loading.present();

    //  this.delay(3000);

    const { role, data } = await this.loading.onDidDismiss();
  }

  async parar() {
    if (this.loading != null) {
      this.loading.dismiss();
      this.fecharLoading = 1;
    } else {
      ;
    }
  }



  login() {

/*
    this.http.get(this.constantes.enderecoApi() + "veiculos")
    .map(res => res.json())
    .subscribe(
      data => {
        alert(data);
        //console.log("veiculos atualizados: " + data.length);
      },
      error => {
        //console.log(error);
        this.verificaConexaoInternet(error);
      });
 */
    this.apresentar();
    this.loginService.retornaLogin(this.email, this.senha)
      .subscribe(
        data => {

          this.parar();

          if (data.id) {

            this.navCtrl.navigateRoot('tabs');
            if (this.manterConectado) {
              this.storage.set(this.constanteApp.chave, true);
            }
            //alert(data.acessoTotal);
            this.storage.set('usuario', data);
          } else {
            this.mensagem('Usuário ou senha Inválidos!!');
          }

        },
        error => {
          this.parar();
         // console.log(error);
          this.verificaConexaoInternet(error);
          
          //this.parar();
        });

       
  }

  async mensagem(msg) {
    const toast = await this.toastController.create({
      message: msg,
      duration: 5000
    });
    toast.present();
  }

  async recuperarSenha() {

    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Recuperar a Senha',
      inputs: [
        {
          name: 'email',
          type: 'text',
          placeholder: 'Insira o seu e-mail'
        }
      ],
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel',
          cssClass: 'secondary',
          handler: () => {
            console.log('Confirm Cancel');
          }
        }, {
          text: 'Recuperar',
          handler: (data) => {
            this.apresentar();

            this.loginService.recuperarSenha(data.email)
              .subscribe(
                data => {
                  this.parar();
                  //console.log(data)
                  if (data.sucesso) {
                    this.mensagem("A senha foi encaminhada para o seu e-mail");
                  } else {
                    this.mensagem("Erro ao recuperar a senha. " + data.mensagem);
                  }
                },
                error => {
                  console.log(error);
                  this.verificaConexaoInternet(error);
                  //this.parar();
                });
          }
        }
      ]
    });

    await alert.present();
  }



chamar(msg){
  if(this.platform.is("cordova")){
    window['plugins'].socialsharing.shareViaWhatsAppToPhone('+5544998341617', "teste.png", null /* img */, msg /* url */, function () { console.log('share ok') });
   // window['plugins'].socialsharing.shareViaWhatsAppToPhone('+5544998341617',"null", "https://warsaw.com.br/wp-content/uploads/2020/09/teste.png" /* img */, null /* url */, function() {console.log('share ok')}, function(errormsg){alert(errormsg)})
  }
}

dataURItoBlob(dataURI) {
  const byteString = window.atob(dataURI);
  const arrayBuffer = new ArrayBuffer(byteString.length);
  const int8Array = new Uint8Array(arrayBuffer);
  for (let i = 0; i < byteString.length; i++) {
    int8Array[i] = byteString.charCodeAt(i);
  }
  const blob = new Blob([int8Array], { type: 'image/jpeg' });    
  return blob;
}

  verificaConexaoInternet(error) {
    this.parar();
    if (error.status == 404) {
      //console.log("falha ao achar a página");
      this.mensagem("falha achar página")
    } else if (error.status == 0) {
      //console.log("erro internet"); 
      this.mensagem("erro com a internet ou erro no servidor " + error)
    }
    //this.parar();
  }

}
