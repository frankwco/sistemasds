import { Component, OnInit } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Storage } from '@ionic/storage';

@Component({
  selector: 'app-pesquisar-cidades',
  templateUrl: './pesquisar-cidades.component.html',
  styleUrls: ['./pesquisar-cidades.component.scss'],
})
export class PesquisarCidadesComponent implements OnInit {
  isItemAvailable = true;
  items = [];

  constructor(private storage:Storage, public modalController: ModalController) {

    this.initializeItems();
  }

  ngOnInit() {}

  fechar(item){
      this.modalController.dismiss({
        'dismissed': true
      });
      this.storage.set("cidadeSelecionada",item);
  }

  voltar(){
    this.modalController.dismiss({
      'dismissed': true
    });
   
}

  initializeItems(){
    //console.log("ini")
    this.storage.get('cidades').then((val) => {
      this.items = val;
      //console.log(" as: "+this.items.length)
 });
}

getItems(ev: any) {

  this.storage.get('cidades').then((valor) => {
    this.items = valor;
    // set val to the value of the searchbar
    //console.log("a: "+ev.target.value);
    const val = ev.target.value;

    // if the value is an empty string don't filter the items
    if (val && val.trim() !== '') {

        this.isItemAvailable = true;
        this.items = this.items.filter((item) => {
            return (item.nome.toLowerCase().indexOf(val.toLowerCase()) > -1);
        })

    } else {
        //console.log("else")
        this.initializeItems();
        //this.isItemAvailable = false;
    }
});


}
}
