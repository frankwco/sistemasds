import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { ColetaPage } from './coleta.page';

const routes: Routes = [
  {
    path: '',
    component: ColetaPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ColetaPageRoutingModule {}
