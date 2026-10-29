import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { MeusLancamentosPage } from './meus-lancamentos.page';

const routes: Routes = [
  {
    path: '',
    component: MeusLancamentosPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class MeusLancamentosPageRoutingModule {}
