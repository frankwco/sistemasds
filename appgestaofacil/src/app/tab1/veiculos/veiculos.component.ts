import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ModalController, AlertController  } from '@ionic/angular';

@Component({
  selector: 'app-veiculos',
  templateUrl: './veiculos.component.html',
  styleUrls: ['./veiculos.component.scss'],
})
export class VeiculosComponent implements OnInit {
  veiculos = [];
  constructor(private storage: Storage,
    public navCtrl: NavController,
    public modalCtrl: ModalController,
    private alertController: AlertController ) { }

  ngOnInit() {

    this.storage.get('veiculos').then((val) => {
      this.veiculos = val;
    });
  }

 async fechar(item) {
    //console.log(item);

    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Confirmação!',
      message: 'SELECIONAR o veículo <strong>'+item.descricao+"</strong>?? Seleção errada pode ocasionar erros nos lançamentos!!",
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
           
            this.storage.set("veiculoSelecionado", item);
            this.modalCtrl.dismiss({
              'dismissed': true
            });

          }
        }
      ]
    });

    await alert.present();




 
    
  }

  voltar() {
    this.modalCtrl.dismiss({
      'dismissed': true
    });
  }

}
