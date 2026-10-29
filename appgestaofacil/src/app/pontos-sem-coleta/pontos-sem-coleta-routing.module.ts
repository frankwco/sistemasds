import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PontosSemColetaPage } from './pontos-sem-coleta.page';

const routes: Routes = [
  {
    path: '',
    component: PontosSemColetaPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PontosSemColetaPageRoutingModule {}
