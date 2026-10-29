import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { DespesaManutencaoPageRoutingModule } from './despesa-manutencao-routing.module';

import { DespesaManutencaoPage } from './despesa-manutencao.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    DespesaManutencaoPageRoutingModule
  ],
  declarations: [DespesaManutencaoPage]
})
export class DespesaManutencaoPageModule {}
