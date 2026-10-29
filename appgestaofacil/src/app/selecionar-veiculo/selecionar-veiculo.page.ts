import { Component, OnInit } from '@angular/core';
import { Storage } from '@ionic/storage';
import { NavController } from '@ionic/angular';

@Component({
  selector: 'app-selecionar-veiculo',
  templateUrl: './selecionar-veiculo.page.html',
  styleUrls: ['./selecionar-veiculo.page.scss'],
})
export class SelecionarVeiculoPage implements OnInit {
  veiculos = [];
  constructor(private storage: Storage,
    private navCtrl: NavController) { }

  ngOnInit() {

    this.storage.get('veiculos').then((val) => {
      this.veiculos = val;
    });
  }

  fechar(item) {
    this.navCtrl.pop();
    //this.storage.set("cidadeSelecionada", item);
  }

  voltar() {
    console.log("Voltar")
    this.navCtrl.pop();
  }

}
