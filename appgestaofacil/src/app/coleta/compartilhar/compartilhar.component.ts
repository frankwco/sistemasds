import { Component, OnInit } from '@angular/core';
import { Platform } from "@ionic/angular";

@Component({
  selector: 'app-compartilhar',
  templateUrl: './compartilhar.component.html',
  styleUrls: ['./compartilhar.component.scss'],
})
export class CompartilharComponent implements OnInit {

  constructor(private platform: Platform) { }

  ngOnInit() { }

  mostrar() {
    if (this.platform.is("cordova")) {
      //window['plugins'].socialsharing.shareViaWhatsAppToPhone('+5544998341617', "teste.png", null /* img */, msg /* url */, function () { console.log('share ok') });
      window['plugins'].socialsharing.shareViaWhatsAppToPhone('+5544998341617', "DSResíduos\n\nColetas realizadas\n\n", null /* img */, null /* url */, function () { console.log('share ok') }, function (errormsg) { alert(errormsg) })
    }


  }

}
