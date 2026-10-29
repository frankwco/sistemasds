import { Injectable } from '@angular/core';
import {ConstantesService} from '../api/constantes.service';
import 'rxjs/add/operator/map';
import { Headers, Http, RequestOptions } from '@angular/http';
import { Observable } from  'rxjs/Observable';
import { HttpClient } from  '@angular/common/http';
import { switchMap } from 'rxjs/operators'

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  constructor(private  httpClient : HttpClient, private contantes:ConstantesService, private http:Http) { }

  /** API antiga (@angular/http) não passa pelos HTTP_INTERCEPTORS do HttpClient — header manual aqui. */
  private opcoesComChave(): RequestOptions {
    const headers = new Headers();
    headers.append('X-App-Key', this.contantes.tokenSeguranca());
    return new RequestOptions({ headers });
  }

retornaLogin(email, senha): Observable<any> {

       var body = {"email":""+email+"", "senha":""+senha+""};

return this.http.post(this.contantes.enderecoApi()+ "usuarioAu", body, this.opcoesComChave()).map(res=>res.json());
}

recuperarSenha(email): Observable<any> {
       var body = {"email":""+email+""};

       return this.http.post(this.contantes.enderecoApi()+ "recuperarSenha", body, this.opcoesComChave()).map(res=>res.json());
}

    // login() {
    //   let autenticado = false;
    //   this.http.get(LoginPage.urlBase + "autenticar?email=" + this.usuario + "&senha=" + this.senha)
    //     .map(res => res.json())
    //     .subscribe(
    //       data => {
    //         console.log("dasdd: " + data.status);
    //         if (data.id > 0) {
    //           this.storage.set('usuario', data);
    //           this.http.get(LoginPage.urlBase + "atividades")
    //             .map(res => res.json())
    //             .subscribe(
    //               data => {
    //                 this.storage.set('atividades', data);
    //               },
    //               error => {
    //                 console.log(error);
    //                 this.verificaConexaoInternet(error);
    //               });
    //
    //           this.http.get(LoginPage.urlBase + "indicadores")
    //             .map(res => res.json())
    //             .subscribe(
    //               data => {
    //                 this.storage.set('indicadores', data);
    //               },
    //               error => {
    //                 console.log(error);
    //                 this.verificaConexaoInternet(error);
    //               });
    //
    //           this.http.get(LoginPage.urlBase + "variaveis")
    //             .map(res => res.json())
    //             .subscribe(
    //               data => {
    //                 this.storage.set('variaveis', data);
    //               },
    //               error => {
    //                 console.log(error);
    //                 this.verificaConexaoInternet(error);
    //               });
    //
    //           if (this.salvarSenha == true) {
    //             this.storage.set('salvarSenha', true);
    //           }
    //           this.navCtrl.setRoot(TabsPage);
    //         }
    //       },
    //       error => {
    //         console.log("Erro:: " + error.status);
    //         if (error.status == 401) {
    //           this.toastCtrl.create({
    //             message: 'Usuário ou Senha Incorretos',
    //             duration: 3000
    //           }).present();
    //         }
    //         this.verificaConexaoInternet(error);
    //       });
    //
    // }


    verificaConexaoInternet(error) {
      if (error.status == 404) {
        console.log("falha achar página");
      } else if (error.status == 0) {
        console.log("erro internet");
      }
    }
}
