import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ModalController, AlertController  } from '@ionic/angular';

@Component({
  selector: 'app-motoristas',
  templateUrl: './motoristas.component.html',
  styleUrls: ['./motoristas.component.scss'],
})
export class MotoristasComponent implements OnInit {

  motoristas = [];
  constructor(private storage: Storage,
    public navCtrl: NavController,
    public modalCtrl: ModalController,
    private alertController: AlertController ) { }

  ngOnInit() {

    this.storage.get('motoristas').then((val) => {
      console.log(val);
      this.motoristas = val;
    });
  }

 async fechar(item) {
    //console.log(item);

    const alert = await this.alertController.create({
      cssClass: 'my-custom-class',
      header: 'Confirmação!',
      message: 'SELECIONAR o motorista <strong>'+item.nome+"</strong>?? Seleção errada pode ocasionar erros nos lançamentos!!",
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
           
            this.storage.set("motoristaSelecionado", item);
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
