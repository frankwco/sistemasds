import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ConstantesService {
  // /app (não /service): esses endpoints agora exigem a chave de API
  // enviada via tokenSeguranca() (ver ChaveApiInterceptor).
  enderecoServidor = "https://alcsistemas.com.br/gestaofacil/rest/app/";
  enderecoLocal = "http://192.168.0.102:8080/gestaofacil/rest/app/"
  // Precisa ser IDÊNTICA à chave configurada no backend
  // (services.ChaveApiFilter / -DappApiKey). Trocar os dois lados juntos.
  tokenApi = "gf-app-7f3a9c2e-51b6-4d8a-9c0f-2e6a8b1d4f77";
  constructor() { }

  enderecoApi() {
    return this.enderecoServidor;
    //return this.enderecoLocal;
  }

  tokenSeguranca() {
    return this.tokenApi;
  }
}
