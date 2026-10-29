import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { CapturaIaPage } from './captura-ia.page';

const routes: Routes = [
  {
    path: '',
    component: CapturaIaPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CapturaIaPageRoutingModule {}
