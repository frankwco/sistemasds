import { Component, Input, OnInit } from '@angular/core';
import { ModalController, Platform } from '@ionic/angular';
import { Storage } from '@ionic/storage';


@Component({
  selector: 'app-pesquisar-telefones',
  templateUrl: './pesquisar-telefones.component.html',
  styleUrls: ['./pesquisar-telefones.component.scss'],
})
export class PesquisarTelefonesComponent implements OnInit {
  @Input() selecionado: any;
  itens=[];

  constructor(public modalController: ModalController,public platform: Platform,private storage: Storage) { }

  ngOnInit() { 
    if(this.selecionado.telefone1){
      this.itens.push({telefone:this.selecionado.telefone1});
    }
    if(this.selecionado.telefone2){
      this.itens.push({telefone:this.selecionado.telefone2});
    }
    if(this.selecionado.telefone1Cliente){
      this.itens.push({telefone:this.selecionado.telefone1Cliente});
    }
    if(this.selecionado.telefone2Cliente){
      this.itens.push({telefone:this.selecionado.telefone2Cliente});
    }
    //alert(this.itens[0].telefone)
    //console.log(this.selecionado)
  }

  fechar() {
    this.modalController.dismiss({
      'dismissed': true
    });
  }

   chamarWhats(telefone){    

    this.storage.get('usuario').then((valUser) => {
    if (this.platform.is("cordova")) {
      window['plugins'].socialsharing.shareViaWhatsAppToPhone("+55"+telefone.replace("(","").replace(")","").replace("-","").replace("_","").replace("-","").replace(" ",""), "Olá, aqui é o "+valUser.nome+" da DS Resíduos. ", null /* img */, null /* url */, function () { console.log('share ok') });
    }
  });
}
  chamarFone(telefone){
  //  window.plugins.CallNumber.callNumber(onSuccess, onError, number, bypassAppChooser);

   
  }
   onSuccess(result){
    console.log("Success:"+result);
  }
  
   onError(result) {
    console.log("Error:"+result);
  }

}
