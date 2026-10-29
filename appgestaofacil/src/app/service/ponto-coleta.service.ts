import { Injectable } from '@angular/core';
import { Storage } from '@ionic/storage';
import { AtualizarDadosService } from '../api/atualizar-dados.service';

@Injectable({
  providedIn: 'root'
})
export class PontoColetaService {
  constructor(private storage: Storage,
  ) {

  }

  delay(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
  presentAlert(msg) {
    alert(msg);
  }

  sleep(time) {
    return new Promise((resolve) => setTimeout(resolve, time));
  }

  

  public insert(key, ponto) {
    //ponto.key = "" + ponto.id;
    return this.storage.set(key, ponto);
  }

  

  public remove(key: string) {
    return this.storage.remove(key);
  }

  public getAll() {

    let pontos = [];



    return this.storage.forEach((value: PontoColeta, key: string, iterationNumber: Number) => {
      //let p = new PontoColetaLista();
      //p.key = key;
      // p.ponto = value;
      pontos.push(value);
    })
      .then(() => {
        return Promise.resolve(pontos);
      })
      .catch((error) => {
        return Promise.reject(error);
      });
  }

/*
  public getColetados(dia, mes, ano, cidade) {

    let pontos = [];

    return this.storage.forEach((value: PontoColeta, key: string, iterationNumber: Number) => {
      //let p = new PontoColetaLista();

      //p.key = key;
      // p.ponto = value;
      if (value.type == "pontoColeta") {
        // console.log(value)
        if (value.diaUltimaColeta == dia && value.mesUltimaColeta == mes && value.anoUltimaColeta == ano && value.idCidade == cidade) {
          pontos.push(value);
        }
      }
    })
      .then(() => {
        return Promise.resolve(pontos);
      })
      .catch((error) => {
        return Promise.reject(error);
      });
  }*/

  public getPontosColetas(){
    return this.storage.get("pontosColetas");
  }
/*
  public getNaoColetados(dia, mes, ano, cidade) {

    let pontos = [];

    //return this.storage.forEach((value: PontoColeta, key: string, iterationNumber: Number) => {
    return this.storage.forEach((val: any, key: string, iterationNumber: Number) => {
      //let p = new PontoColetaLista();
      //  p.key = key;
      // p.ponto = value;

      if (key == "pontosColeta") {
        

        if ((value.diaUltimaColeta != dia || value.mesUltimaColeta != mes || value.anoUltimaColeta != ano) && value.idCidade == cidade) {
          pontos.push(value);
        }
      }

    })
      .then(() => {
        return Promise.resolve(pontos);
      })
      .catch((error) => {
        return Promise.reject(error);
      });
  }*/

}


export class PontoColeta {
  type: string;
  id: number;
  key: string;
  acessoSistema: number;
  diaVencimentoFatura: number;
  emitirContrato: number;
  modeloCadastroPonto: string;
  status: number;
  cliente: any;
  razaoSocialCliente: string;
  ordem: number;
  periodicidadeColeta: string;
  rota: any;
  nomeRota: string;
  contratoAtivo: number;
  gruposColetados: string;
  anoUltimaColeta: number;
  dataPenultimaColeta: string;
  dataUltimaColeta: string;
  diaUltimaColeta: number;
  mesUltimaColeta: number;
  dataUltimaAlteracao: string;
  horaUltimaColeta: number;
  minutoUltimaColeta: number;
  segundoUltimaColeta: number;
  cidade: any;
  idCidade: number;
  endereco: string;
  numero: string;
  apelido: string;
  nomeFantasia: string;
  nomeRazaoSocial: string;
}

/*export class PontoColetaLista {
  key: string;
  ponto: PontoColeta;
}*/
