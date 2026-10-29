import { Injectable } from '@angular/core';
import { ConstantesService } from './constantes.service';
import 'rxjs/add/operator/map';
import { Headers, Http, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { HttpClient } from '@angular/common/http';
import { switchMap } from 'rxjs/operators'

@Injectable({
  providedIn: 'root'
})
export class DadosPontosService {

  constructor(private httpClient: HttpClient, private contantes: ConstantesService, private http: Http) { }

  /** API antiga (@angular/http) não passa pelos HTTP_INTERCEPTORS do HttpClient — header manual aqui. */
  private opcoesComChave(): RequestOptions {
    const headers = new Headers();
    headers.append('X-App-Key', this.contantes.tokenSeguranca());
    return new RequestOptions({ headers });
  }

  proximaColeta(idPontoColeta): Observable<any> {
    return this.http.get(this.contantes.enderecoApi() + "proximaColeta?idPontoColeta=" + idPontoColeta, this.opcoesComChave()).map(res => res.json());
  }

  semColetasMesAno(mes, ano){
    return this.http.get(this.contantes.enderecoApi() + "semColetas?mes=" + mes+"&ano="+ano, this.opcoesComChave()).map(res => res.json());
  }

  diasSemColetaPontos(){
    return this.http.get(this.contantes.enderecoApi() + "diasSemColetaPontos", this.opcoesComChave()).map(res => res.json());
  }

  verificaConexaoInternet(error) {
    if (error.status == 404) {
      console.log("falha achar página");
    } else if (error.status == 0) {
      console.log("erro internet");
    }
  }
}
