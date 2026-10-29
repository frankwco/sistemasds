import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { RelatorioColetasPageRoutingModule } from './relatorio-coletas-routing.module';

import { RelatorioColetasPage } from './relatorio-coletas.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    RelatorioColetasPageRoutingModule
  ],
  declarations: [RelatorioColetasPage]
})
export class RelatorioColetasPageModule {}
