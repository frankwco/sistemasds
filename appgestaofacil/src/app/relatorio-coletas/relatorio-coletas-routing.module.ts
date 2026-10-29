import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { RelatorioColetasPage } from './relatorio-coletas.page';

const routes: Routes = [
  {
    path: '',
    component: RelatorioColetasPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class RelatorioColetasPageRoutingModule {}
