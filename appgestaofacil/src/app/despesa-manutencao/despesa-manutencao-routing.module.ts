import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { DespesaManutencaoPage } from './despesa-manutencao.page';

const routes: Routes = [
  {
    path: '',
    component: DespesaManutencaoPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DespesaManutencaoPageRoutingModule {}
