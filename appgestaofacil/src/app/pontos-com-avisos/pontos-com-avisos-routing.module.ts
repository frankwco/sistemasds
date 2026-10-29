import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PontosComAvisosPage } from './pontos-com-avisos.page';

const routes: Routes = [
  {
    path: '',
    component: PontosComAvisosPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PontosComAvisosPageRoutingModule {}
