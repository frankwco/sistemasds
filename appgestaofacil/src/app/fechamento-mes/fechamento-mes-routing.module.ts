import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { FechamentoMesPage } from './fechamento-mes.page';

const routes: Routes = [
  {
    path: '',
    component: FechamentoMesPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FechamentoMesPageRoutingModule {}
