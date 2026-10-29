import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { fromEventPattern } from 'rxjs';
import { PesquisarPontosComponent } from './pesquisar-pontos/pesquisar-pontos.component';
import { PesquisarCidadesComponent } from './pesquisar-cidades/pesquisar-cidades.component';
import { PesquisarColetasComponent } from './pesquisar-coletas/pesquisar-coletas.component';
import { AssinaturaComponent } from './assinatura/assinatura.component';
import { Storage } from '@ionic/storage';
import { Validators, FormBuilder, FormGroup } from '@angular/forms';
import { MenuController, NavController, AlertController, Platform } from '@ionic/angular';
import { AtualizarDadosService } from '../api/atualizar-dados.service';


@Component({
  selector: 'app-coleta',
  templateUrl: './coleta.page.html',
  styleUrls: ['./coleta.page.scss'],
})
export class ColetaPage implements OnInit {
  cidadeSelecionada;
  pontoColetaSelecionado = { "nomeCidade": "", "nomeRazaoSocial": "Selecione", "gruposColetados": "", "nomeFantasia": "", "apelido": "" };
  pontosColetas = [];
  pontosColetados = [];
  coleta;
  usuario;
  veiculoSelecionado = { "id": null, "descricao": "", "placa": "" };
  motoristaSelecionado = { "id": null, "nome": "", "email": "" };
  urlWhats: string = "aaa"

  private todo: FormGroup;
  constructor(
    public platform: Platform,
    private navCtrl: NavController,
    private atualizarDadosService: AtualizarDadosService,
    private menu: MenuController,
    private formBuilder: FormBuilder,
    private storage: Storage,
    private alertController: AlertController,
    public modalController: ModalController
  ) {



    this.storage.get('usuario').then((val) => {
      this.usuario = val;
    });

  }

  mostrar() {
    if (this.platform.is("cordova")) {
      window['plugins'].socialsharing.shareViaWhatsAppToPhone('+554499323410', this.pontoColetaSelecionado.nomeRazaoSocial + "/" + this.pontoColetaSelecionado.nomeFantasia + " - " + this.pontoColetaSelecionado.nomeCidade, null /* img */, null /* url */, function () { console.log('share ok') });
    }
  }

  novaColeta() {
    this.cidadeSelecionada = { "nome": "Selecione", "id": "" };
    this.pontoColetaSelecionado = { "nomeCidade": "", "nomeRazaoSocial": "Selecione", "gruposColetados": "", "nomeFantasia": "", "apelido": "" };
    this.coleta = { "situacao": "Coletado", "a": "0", "e": "0", "b": "0", "a2": "0", "a3": "0", "a4": "0", dataUltimaColeta: new Date() };
    this.storage.set('pontoColetaSelecionado', "");
    this.todo = this.formBuilder.group({
      a: [0],
      e: [0],
      b: [0],
      a2: [0],
      a3: [0],
      a4: [0],
      situacao: "Coletado"
    });



    this.storage.get('cidadeSelecionada').then((val) => {
      if (val != null) {
        this.cidadeSelecionada = val;
        //console.log(val);
      } else {
        this.cidadeSelecionada = { "nome": "Selecione", "id": "" };
      }
    });
  }

  ngOnInit() {
    //this.buscarPontos();
    this.novaColeta();

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


  }



  async pesquisarColetas() {
    // this.atualizarDadosService.buscarColetas();

    const modal = await this.modalController.create({
      component: PesquisarColetasComponent
    });
    modal.onDidDismiss()
      .then((data) => {
        //this.atualizarDadosService.atualizar();
      });
    return await modal.present();
  }

  buscarPontos() {
    this.storage.get('pontosColeta').then((val) => {
      this.pontosColetas = val;
      //console.log("coletas "+ val[1]);
      //this.buscarPontosCidade();
    });
  }

  convertString(value) {
    value = value.replace(",", ".");
    //return parseFloat(parseFloat(value).toFixed(3));
    return parseFloat(value);
  }

  async pesquisarPontos() {
    //this.buscarPontosCidade();
    const modal = await this.modalController.create({
      component: PesquisarPontosComponent
    });
    modal.onDidDismiss()
      .then((data) => {
        this.storage.get('pontoColetaSelecionado').then((val) => {
          // console.log("VALO: "+val)
          if (val) {
            // console.log("AAAAAA")
            this.pontoColetaSelecionado = val;
            this.coleta.pontoColetaSelecionado = this.pontoColetaSelecionado;
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
        //NESSE MÉTODO MARCAR OS PONTOS QUE FORAM COLETADOS, DE ACORDO COM OS PONTOS DA LISTA DE COLETAS
        /*
                let dt = new Date();
        
                let dia = dt.getDate(), mes = dt.getMonth() + 1, ano = dt.getFullYear();
        
                for (let x = 0; x < this.pontosColetas.length; x++) {
                  if (this.pontosColetas[x].cidade.id == this.cidadeSelecionada.id) {
                    //console.log("Dia ultima: "+this.pontosColetas[x].id+" "+this.pontosColetas[x].diaUltimaColeta)
                    if (dia == this.pontosColetas[x].diaUltimaColeta && mes == this.pontosColetas[x].mesUltimaColeta && ano == this.pontosColetas[x].anoUltimaColeta) {
        
                      pontosCidadesColetados.push(this.pontosColetas[x]);
                    } else {
                      pontosCidadesColetar.push(this.pontosColetas[x]);
                    }
                  }
                }
                //console.log("TESTE: "+pontosCidadesColetar.length)
        
                this.storage.set('pontosCidadesColetar', pontosCidadesColetar);
                this.storage.set('pontosCidadesColetados', pontosCidadesColetados);
        
                console.log(" Coletados c: " + pontosCidadesColetados.length)
                **/
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

  async finalizarVerificar() {

    if (this.convertString(this.coleta.a) > 20 || this.convertString(this.coleta.e) > 20 || this.convertString(this.coleta.b) > 20 || this.convertString(this.coleta.a2) > 20 || this.convertString(this.coleta.a3) > 20 || this.convertString(this.coleta.a4) > 20) {


      const alert = await this.alertController.create({
        cssClass: 'my-custom-class',
        header: 'Confirmar!',
        message: 'Verifique se os valores estão corretos, percebi um peso maior que 20KG. Deseja continuar??',
        buttons: [
          {
            text: 'Não/Cancelar',
            role: 'cancel',
            cssClass: 'secondary',
            handler: (blah) => {
              console.log('Confirm Cancel: blah');
            }
          }, {
            text: 'Sim',
            handler: () => {
              this.finalizar();
            }
          }
        ]
      });

      await alert.present();

    } else {

  
       this.coleta.a = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.a).toFixed(2)));
       this.coleta.e = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.e).toFixed(2)));
       this.coleta.b = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.b).toFixed(2)));
       this.coleta.a2 = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.a2).toFixed(2)));
       this.coleta.a3 = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.a3).toFixed(2)));
       this.coleta.a4 = this.formatarTresCasas(parseFloat(this.convertString(this.coleta.a4).toFixed(2)));
         
      this.finalizar();

    }
  }

  formatarTresCasas(numero) {
    return numero.toLocaleString('pt-BR', {
      minimumFractionDigits: 3,
      maximumFractionDigits: 3
    })
  }

  async finalizar() {
    //alert("FIMM")

    //console.log()


    /**this.geolocation.getCurrentPosition().then((resp) => {
      // resp.coords.latitude
      // resp.coords.longitude
      alert(resp.coords.latitude+" "+ resp.coords.longitude);
     }).catch((error) => {
       console.log('Error getting location', error);
     });*/

    const modal = await this.modalController.create({
      component: AssinaturaComponent
    });
    this.coleta.veiculo = this.veiculoSelecionado;
    this.coleta.nomeMotorista = this.motoristaSelecionado.nome;
    this.coleta.emailMotorista = this.motoristaSelecionado.email;
    this.coleta.sincronizado = false;
    this.coleta.emailUsuario = this.usuario.email;
    this.coleta.totalColetado = this.convertString(this.coleta.a) + this.convertString(this.coleta.e) + this.convertString(this.coleta.b) + this.convertString(this.coleta.a2) + this.convertString(this.coleta.a3)+ this.convertString(this.coleta.a4);
    this.storage.set('coleta', this.coleta);

    modal.onDidDismiss()
      .then((data) => {
        //this.buscarPontos();
        this.storage.get("finalizada").then((data) => {
          if (data == 'sim') {
            this.novaColeta();
          }
        });

      });

    return await modal.present();
  }

}
