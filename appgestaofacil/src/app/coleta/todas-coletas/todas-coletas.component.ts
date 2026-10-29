import { Component, Input, OnInit } from '@angular/core';
import { ConstantesService } from '../../api/constantes.service';
import { Headers, Http, RequestOptions } from '@angular/http';
import { ModalController } from '@ionic/angular';
import * as moment from 'moment';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-todas-coletas',
  templateUrl: './todas-coletas.component.html',
  styleUrls: ['./todas-coletas.component.scss'],
})
export class TodasColetasComponent implements OnInit {
  @Input() idPontoColeta: any;
  items=[];
  buscando=true;
  constructor(private constantes: ConstantesService, 
    private http: Http,
    public modalController: ModalController) { 
      this.buscando=true;
    }

  fechar() {
    this.modalController.dismiss({
      'dismissed': true
    });
  }
  
  ngOnInit() {
    var olderDate = moment(new Date()).subtract(365, 'day').toDate();
    const datepipe: DatePipe = new DatePipe('en-US')
    let formattedDate = datepipe.transform(olderDate, 'yyyy-MM-dd HH:mm:ss')
    //console.log("DATA: "+formattedDate)
    const headers = new Headers();
    headers.append('X-App-Key', this.constantes.tokenSeguranca());
    this.http.get(this.constantes.enderecoApi() + "todasColetas?idPontoColeta="+this.idPontoColeta+"&dataConsulta="+formattedDate, new RequestOptions({ headers }))
    .map(res => res.json())
    .subscribe(
      data => {
        this.items = data;
        //console.log(this.items)
        this.buscando=false;
      },
      error => {
        console.log(error);
        this.verificaConexaoInternet(error);
      });
  }

  verificaConexaoInternet(error) {
    if (error.status == 404) {
      console.log("falha achar página");
    } else if (error.status == 0) {
      console.log("erro internet");
    }
  }

}
