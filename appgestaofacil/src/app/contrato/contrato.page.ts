import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { AssinaturaComponent } from './assinatura/assinatura.component';

@Component({
  selector: 'app-contrato',
  templateUrl: './contrato.page.html',
  styleUrls: ['./contrato.page.scss'],
})
export class ContratoPage implements OnInit {

  isItemAvailable = true;
  items = [];

  constructor(public modalController: ModalController) {

    this.initializeItems();
  }

  ngOnInit() {}

  fechar(){
      this.modalController.dismiss({
        'dismissed': true,
        'selecionado':'aassda'
      });
  }

  async assinar() {
    const modal = await this.modalController.create({
      component: AssinaturaComponent
    });
    return await modal.present();
  }

  initializeItems(){
    this.items = ["José Antonio","Farmácia Brasil", "Laboratório Bom Jardim"];
}

}
