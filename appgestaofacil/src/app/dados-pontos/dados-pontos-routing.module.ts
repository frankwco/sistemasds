import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DadosPontosPage } from './dados-pontos.page';

const routes: Routes = [
  {
    path: '',
    component: DadosPontosPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DadosPontosPageRoutingModule {}
