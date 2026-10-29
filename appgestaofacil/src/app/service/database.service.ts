import { Platform } from '@ionic/angular';
import { Injectable } from '@angular/core';
import { SQLitePorter } from '@ionic-native/sqlite-porter/ngx';
import { HttpClient } from '@angular/common/http';
import { SQLite, SQLiteObject } from '@ionic-native/sqlite/ngx';
import { BehaviorSubject, Observable } from 'rxjs';

export interface PontoColeta {
  id: number,
  acessoSistema: number,
  diaVencimentoFatura: number,
  emitirContrato: number,
  modeloCadastroPonto: string,
  status: number,
  cliente: string,
  razaoSocialCliente: string,
  ordem: number,
  periodicidadeColeta: string,
  rota: string,
  nomeRota: string,
  contratoAtivo: number,
  gruposColetados: string,
  anoUltimaColeta: number,
  dataPenultimaColeta: string,
  dataUltimaColeta: string,
  diaUltimaColeta: number,
  mesUltimaColeta: number,
  dataUltimaAlteracao: string,
  horaUltimaColeta: number,
  minutoUltimaColeta: number,
  segundoUltimaColeta: number,
  cidade: string,
  idCidade: number,
  endereco: string,
  numero: string,
  apelido: string,
  nomeFantasia: string,
  nomeRazaoSocial: string
}


@Injectable({
  providedIn: 'root'
})
export class DatabaseService {
  private database: SQLiteObject;
  private dbReady: BehaviorSubject<boolean> = new BehaviorSubject(false);

  pontosColetas = new BehaviorSubject([]);
  //products = new BehaviorSubject([]);

  constructor(private plt: Platform, private sqlitePorter: SQLitePorter, private sqlite: SQLite, private http: HttpClient) {
   // this.inicia();
  }
  inicia() {
    this.plt.ready().then(() => {
      this.sqlite.create({
        name: 'appdstransporte5.db',
        location: 'default'
      })
        .then((db: SQLiteObject) => {
          this.database = db;
          this.seedDatabase();
        });
    });
  }

  seedDatabase() {
    this.http.get('assets/banco.sql', { responseType: 'text' })
      .subscribe(sql => {
        this.sqlitePorter.importSqlToDb(this.database, sql)
          .then(_ => {
            //alert("SEED")
            //this.addPontoColeta();  
            this.loadPontos();
            this.dbReady.next(true);
            //alert("NEST")
          })
          .catch(e => console.error(e));
      });
  }

  getDatabaseState() {
    return this.dbReady.asObservable();
  }

  getPontos(): Observable<PontoColeta[]> {
    return this.pontosColetas.asObservable();
  }

  /*getProducts(): Observable<any[]> {
    return this.products.asObservable();
  }*/



  loadPontos() {

    return this.database.executeSql('SELECT * FROM ponto_coleta', []).then(data => {
      let pontos: PontoColeta[] = [];
      //alert("LOADss "+data.rows.length)
      if (data.rows.length > 0) {
        for (var i = 0; i < data.rows.length; i++) {
          /*let skills = [];
          if (data.rows.item(i).skills != '') {
            skills = JSON.parse(data.rows.item(i).skills);
          }*/

          pontos.push({
            id: data.rows.item(i).id,
            acessoSistema: data.rows.item(i).acessoSistema,
            diaVencimentoFatura: data.rows.item(i).diaVencimentoFatura,
            emitirContrato: data.rows.item(i).emitirContrato,
            modeloCadastroPonto: data.rows.item(i).modeloCadastroPonto,
            status: data.rows.item(i).status,
            cliente: data.rows.item(i).cliente,
            razaoSocialCliente: data.rows.item(i).razaoSocialCliente,
            ordem: data.rows.item(i).ordem,
            periodicidadeColeta: data.rows.item(i).periodicidadeColeta,
            rota: data.rows.item(i).rota,
            nomeRota: data.rows.item(i).nomeRota,
            contratoAtivo: data.rows.item(i).contratoAtivo,
            gruposColetados: data.rows.item(i).gruposColetados,
            anoUltimaColeta: data.rows.item(i).anoUltimaColeta,
            dataPenultimaColeta: data.rows.item(i).dataPenultimaColeta,
            dataUltimaColeta: data.rows.item(i).dataUltimaColeta,
            diaUltimaColeta: data.rows.item(i).diaUltimaColeta,
            mesUltimaColeta: data.rows.item(i).mesUltimaColeta,
            dataUltimaAlteracao: data.rows.item(i).dataUltimaAlteracao,
            horaUltimaColeta: data.rows.item(i).horaUltimaColeta,
            minutoUltimaColeta: data.rows.item(i).minutoUltimaColeta,
            segundoUltimaColeta: data.rows.item(i).segundoUltimaColeta,
            cidade: data.rows.item(i).cidade,
            idCidade: data.rows.item(i).cidade.id,
            endereco: data.rows.item(i).endereco,
            numero: data.rows.item(i).numero,
            apelido: data.rows.item(i).apelido,
            nomeFantasia: data.rows.item(i).nomeFantasia,
            nomeRazaoSocial: data.rows.item(i).nomeRazaoSocial
          });
        }
      }
      this.pontosColetas.next(pontos);
    });
  }

  addPontoColeta(id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial) {
    // let data = [name, JSON.stringify(skills), img];
    let datas = [id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial];
    //alert("ADD "+this.database)
    return this.database.executeSql('INSERT INTO ponto_coleta (id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', datas).then(data => {
      //return this.database.executeSql('SELECT * FROM ponto_coleta', []).then(data => {
      //let pontos: PontoColeta[] = [];
      //alert("LOADssssd "+data.rows.length);  

      this.loadPontos();
    });
  }

  addUpdatePontoColeta(id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial) {
    // let data = [name, JSON.stringify(skills), img];
    let datas = [id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial];
    //alert("ADD "+this.database)
    return this.database.executeSql('INSERT OR REPLACE INTO ponto_coleta (id, acessoSistema, diaVencimentoFatura, emitirContrato, modeloCadastroPonto, status, cliente, razaoSocialCliente, ordem, periodicidadeColeta, rota, nomeRota, contratoAtivo, gruposColetados, anoUltimaColeta, dataPenultimaColeta, dataUltimaColeta, diaUltimaColeta, mesUltimaColeta, dataUltimaAlteracao, horaUltimaColeta, minutoUltimaColeta, segundoUltimaColeta, cidade, idCidade, endereco, numero, apelido, nomeFantasia, nomeRazaoSocial ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', datas).then(data => {
      //return this.database.executeSql('SELECT * FROM ponto_coleta', []).then(data => {
      //let pontos: PontoColeta[] = [];
      //alert("LOADssssd "+data.rows.length);  

      this.loadPontos();
    });
  }

  pontoColetasDiaMesAnoIgual(dia, mes, ano, idCidade) {

    return this.database.executeSql('SELECT * FROM ponto_coleta WHERE diaUltimaColeta=? and mesUltimaColeta=? and anoUltimaColeta=? and idCidade=?', [dia, mes, ano, idCidade]).then(data => {
      let pontos: PontoColeta[] = [];
      //alert("LOADss::: "+data.rows.length)
      if (data.rows.length > 0) {
        for (var i = 0; i < data.rows.length; i++) {
          /*let skills = [];
          if (data.rows.item(i).skills != '') {
            skills = JSON.parse(data.rows.item(i).skills);
          }*/

          pontos.push({
            id: data.rows.item(i).id,
            acessoSistema: data.rows.item(i).acessoSistema,
            diaVencimentoFatura: data.rows.item(i).diaVencimentoFatura,
            emitirContrato: data.rows.item(i).emitirContrato,
            modeloCadastroPonto: data.rows.item(i).modeloCadastroPonto,
            status: data.rows.item(i).status,
            cliente: data.rows.item(i).cliente,
            razaoSocialCliente: data.rows.item(i).razaoSocialCliente,
            ordem: data.rows.item(i).ordem,
            periodicidadeColeta: data.rows.item(i).periodicidadeColeta,
            rota: data.rows.item(i).rota,
            nomeRota: data.rows.item(i).nomeRota,
            contratoAtivo: data.rows.item(i).contratoAtivo,
            gruposColetados: data.rows.item(i).gruposColetados,
            anoUltimaColeta: data.rows.item(i).anoUltimaColeta,
            dataPenultimaColeta: data.rows.item(i).dataPenultimaColeta,
            dataUltimaColeta: data.rows.item(i).dataUltimaColeta,
            diaUltimaColeta: data.rows.item(i).diaUltimaColeta,
            mesUltimaColeta: data.rows.item(i).mesUltimaColeta,
            dataUltimaAlteracao: data.rows.item(i).dataUltimaAlteracao,
            horaUltimaColeta: data.rows.item(i).horaUltimaColeta,
            minutoUltimaColeta: data.rows.item(i).minutoUltimaColeta,
            segundoUltimaColeta: data.rows.item(i).segundoUltimaColeta,
            cidade: data.rows.item(i).cidade,
            idCidade: data.rows.item(i).cidade.id,
            endereco: data.rows.item(i).endereco,
            numero: data.rows.item(i).numero,
            apelido: data.rows.item(i).apelido,
            nomeFantasia: data.rows.item(i).nomeFantasia,
            nomeRazaoSocial: data.rows.item(i).nomeRazaoSocial
          });
        }
      }
      return pontos;
      //this.pontosColetas.next(pontos);
    });
  }

  pontoColetasDiaMesAnoDiferente(dia, mes, ano, idCidade) {
    //alert(idCidade)

    return this.database.executeSql('SELECT * FROM ponto_coleta WHERE (diaUltimaColeta<>? or mesUltimaColeta<>? or anoUltimaColeta<>?) and idCidade=?', [dia, mes, ano, idCidade]).then(data => {
      let pontos: PontoColeta[] = [];
     // alert("LOADss..::: "+data.rows.length)
      if (data.rows.length > 0) {
        for (var i = 0; i < data.rows.length; i++) {
          /*let skills = [];
          if (data.rows.item(i).skills != '') {
            skills = JSON.parse(data.rows.item(i).skills);
          }*/
          if (i == 0) {
            //alert("Cidade 0: "+data.rows.item(0).idCidade)
          }

          pontos.push({
            id: data.rows.item(i).id,
            acessoSistema: data.rows.item(i).acessoSistema,
            diaVencimentoFatura: data.rows.item(i).diaVencimentoFatura,
            emitirContrato: data.rows.item(i).emitirContrato,
            modeloCadastroPonto: data.rows.item(i).modeloCadastroPonto,
            status: data.rows.item(i).status,
            cliente: data.rows.item(i).cliente,
            razaoSocialCliente: data.rows.item(i).razaoSocialCliente,
            ordem: data.rows.item(i).ordem,
            periodicidadeColeta: data.rows.item(i).periodicidadeColeta,
            rota: data.rows.item(i).rota,
            nomeRota: data.rows.item(i).nomeRota,
            contratoAtivo: data.rows.item(i).contratoAtivo,
            gruposColetados: data.rows.item(i).gruposColetados,
            anoUltimaColeta: data.rows.item(i).anoUltimaColeta,
            dataPenultimaColeta: data.rows.item(i).dataPenultimaColeta,
            dataUltimaColeta: data.rows.item(i).dataUltimaColeta,
            diaUltimaColeta: data.rows.item(i).diaUltimaColeta,
            mesUltimaColeta: data.rows.item(i).mesUltimaColeta,
            dataUltimaAlteracao: data.rows.item(i).dataUltimaAlteracao,
            horaUltimaColeta: data.rows.item(i).horaUltimaColeta,
            minutoUltimaColeta: data.rows.item(i).minutoUltimaColeta,
            segundoUltimaColeta: data.rows.item(i).segundoUltimaColeta,
            cidade: data.rows.item(i).cidade,
            idCidade: data.rows.item(i).cidade.id,
            endereco: data.rows.item(i).endereco,
            numero: data.rows.item(i).numero,
            apelido: data.rows.item(i).apelido,
            nomeFantasia: data.rows.item(i).nomeFantasia,
            nomeRazaoSocial: data.rows.item(i).nomeRazaoSocial
          });
        }
      }
      return pontos;
      //this.pontosColetas.next(pontos);
    });
  }

  getPontoColeta(id): Promise<PontoColeta> {
    //alert("as")
    return this.database.executeSql('SELECT * FROM ponto_coleta WHERE id = ?', [id]).then(data => {
      //alert("aas")
      if (data.rows.length > 0) {
        return {
          id: data.rows.item(0).id,
          acessoSistema: data.rows.item(0).acessoSistema,
          diaVencimentoFatura: data.rows.item(0).diaVencimentoFatura,
          emitirContrato: data.rows.item(0).emitirContrato,
          modeloCadastroPonto: data.rows.item(0).modeloCadastroPonto,
          status: data.rows.item(0).status,
          cliente: data.rows.item(0).cliente,
          razaoSocialCliente: data.rows.item(0).razaoSocialCliente,
          ordem: data.rows.item(0).ordem,
          periodicidadeColeta: data.rows.item(0).periodicidadeColeta,
          rota: data.rows.item(0).rota,
          nomeRota: data.rows.item(0).nomeRota,
          contratoAtivo: data.rows.item(0).contratoAtivo,
          gruposColetados: data.rows.item(0).gruposColetados,
          anoUltimaColeta: data.rows.item(0).anoUltimaColeta,
          dataPenultimaColeta: data.rows.item(0).dataPenultimaColeta,
          dataUltimaColeta: data.rows.item(0).dataUltimaColeta,
          diaUltimaColeta: data.rows.item(0).diaUltimaColeta,
          mesUltimaColeta: data.rows.item(0).mesUltimaColeta,
          dataUltimaAlteracao: data.rows.item(0).dataUltimaAlteracao,
          horaUltimaColeta: data.rows.item(0).horaUltimaColeta,
          minutoUltimaColeta: data.rows.item(0).minutoUltimaColeta,
          segundoUltimaColeta: data.rows.item(0).segundoUltimaColeta,
          cidade: data.rows.item(0).cidade,
          idCidade: data.rows.item(0).cidade.id,
          endereco: data.rows.item(0).endereco,
          numero: data.rows.item(0).numero,
          apelido: data.rows.item(0).apelido,
          nomeFantasia: data.rows.item(0).nomeFantasia,
          nomeRazaoSocial: data.rows.item(0).nomeRazaoSocial
        }
      } else {
        return null;
      }
    });
  }
  /*
   getDeveloper(id): Promise<Dev> {
     return this.database.executeSql('SELECT * FROM developer WHERE id = ?', [id]).then(data => {
       let skills = [];
       if (data.rows.item(0).skills != '') {
         skills = JSON.parse(data.rows.item(0).skills);
       }
  
       return {e
         id: data.rows.item(0).id,
         name: data.rows.item(0).name, 
         skills: skills, 
         img: data.rows.item(0).img
       }
     });
   }*/

  deletePontos(id) {
    return this.database.executeSql('DELETE FROM ponto_coleta WHERE id = ?', [id]).then(_ => {
      this.loadPontos();
      //this.loadProducts();
    });
  }

  updatePonto(ponto: PontoColeta) {
    //let data = [dev.name, JSON.stringify(dev.skills), dev.img];
    //alert()
  //  let data = [ponto.id, ponto.acessoSistema, ponto.diaVencimentoFatura, ponto.emitirContrato, ponto.modeloCadastroPonto, ponto.status, ponto.cliente, ponto.razaoSocialCliente, ponto.ordem, ponto.periodicidadeColeta, ponto.rota, ponto.nomeRota, ponto.contratoAtivo, ponto.gruposColetados, ponto.anoUltimaColeta, ponto.dataPenultimaColeta, ponto.dataUltimaColeta, ponto.diaUltimaColeta, ponto.mesUltimaColeta, ponto.anoUltimaColeta, ponto.dataUltimaAlteracao, ponto.horaUltimaColeta, ponto.minutoUltimaColeta, ponto.segundoUltimaColeta, ponto.cidade, ponto.idCidade, ponto.endereco, ponto.numero, ponto.apelido, ponto.nomeFantasia, ponto.nomeRazaoSocial];
  let data = [ponto.diaUltimaColeta, ponto.mesUltimaColeta, ponto.anoUltimaColeta, ponto.dataUltimaAlteracao, ponto.horaUltimaColeta, ponto.minutoUltimaColeta, ponto.segundoUltimaColeta];
     
  //alert(JSON.stringify(data))
    //return this.database.executeSql('UPDATE ponto_coleta SET id = ?, acessoSistema = ?, diaVencimentoFatura = ?, emitirContrato = ?, modeloCadastroPonto = ?, status = ?, cliente = ?, razaoSocialCliente = ?, ordem = ?, periodicidadeColeta = ?, rota = ?, nomeRota = ?, contratoAtivo = ?, gruposColetados = ?, anoUltimaColeta = ?, dataPenultimaColeta = ?, dataUltimaColeta = ?, diaUltimaColeta = ?, mesUltimaColeta = ?, anoUltimaColeta = ?, dataUltimaAlteracao = ?, horaUltimaColeta = ?, minutoUltimaColeta = ?, segundoUltimaColeta = ?, cidade = ?, idCidade = ?, endereco = ?, numero = ?, apelido = ?, nomeFantasia = ?, nomeRazaoSocial = ? WHERE id = ' + ponto.id, data).then(data => {
      return this.database.executeSql('UPDATE ponto_coleta SET diaUltimaColeta = ?, mesUltimaColeta = ?, anoUltimaColeta = ?, dataUltimaAlteracao = ?, horaUltimaColeta = ?, minutoUltimaColeta = ?, segundoUltimaColeta = ? WHERE id = ' + ponto.id, data).then(data => {
      
    //alert("AQUI UO")
      this.loadPontos();
    })
  }

  /* loadProducts() {
     let query = 'SELECT product.name, product.id, developer.name AS creator FROM product JOIN developer ON developer.id = product.creatorId';
     return this.database.executeSql(query, []).then(data => {
       let products = [];
       if (data.rows.length > 0) {
         for (var i = 0; i < data.rows.length; i++) {
           products.push({ 
             name: data.rows.item(i).name,
             id: data.rows.item(i).id,
             creator: data.rows.item(i).creator,
            });
         }
       }
       this.products.next(products);
     });
   }*/

  /* addProduct(name, creator) {
     let data = [name, creator];
     return this.database.executeSql('INSERT INTO product (name, creatorId) VALUES (?, ?)', data).then(data => {
       this.loadProducts();
     });
   }*/
}