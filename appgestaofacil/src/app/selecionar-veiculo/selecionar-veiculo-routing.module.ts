import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { SelecionarVeiculoPage } from './selecionar-veiculo.page';

const routes: Routes = [
  {
    path: '',
    component: SelecionarVeiculoPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SelecionarVeiculoPageRoutingModule {}
