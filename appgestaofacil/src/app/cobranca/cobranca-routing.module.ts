import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { CobrancaPage } from './cobranca.page';

const routes: Routes = [
  {
    path: '',
    component: CobrancaPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CobrancaPageRoutingModule {}
