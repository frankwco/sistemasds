import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConstantesService } from './constantes.service';

/**
 * Adiciona o header X-App-Key em toda requisição feita via HttpClient.
 * Cobre atualizar-dados.service.ts (cadastrarColeta, processos,
 * todosPontosColetaReduzidos, cidades, veiculos, motoristas).
 *
 * Não cobre login.service.ts, dados-pontos.service.ts, login.page.ts e
 * todas-coletas.component.ts — esses ainda usam a API antiga Http do
 * @angular/http, que não passa pelos HTTP_INTERCEPTORS do HttpClient. Neles
 * o header foi adicionado manualmente em cada chamada.
 */
@Injectable()
export class ChaveApiInterceptor implements HttpInterceptor {

  constructor(private constantes: ConstantesService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Só nas chamadas ao backend do gestaofacil — não em recursos locais do
    // app (ex.: assets/banco.sql, carregado via HttpClient também).
    if (!req.url.startsWith(this.constantes.enderecoApi())) {
      return next.handle(req);
    }

    const clonado = req.clone({
      setHeaders: { 'X-App-Key': this.constantes.tokenSeguranca() }
    });
    return next.handle(clonado);
  }
}
