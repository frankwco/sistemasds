import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController, ModalController, AlertController } from '@ionic/angular';

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
    private alertController: AlertController) { }

  ngOnInit() {
    this.veiculos = [];
    this.veiculos.push({ "id": null, "descricao": "Todos", "placa": "" });
    this.storage.get('veiculos').then((val) => {
      for (let x = 0; x < val.length; x++) {
        this.veiculos.push(val[x]);
      }
    });
  }

  async fechar(item) {
    //console.log(item);

    this.storage.set("veiculoPesquisaSelecionado", item);
    this.modalCtrl.dismiss({
      'dismissed': true
    });






  }

  voltar() {
    this.modalCtrl.dismiss({
      'dismissed': true
    });
  }

}
