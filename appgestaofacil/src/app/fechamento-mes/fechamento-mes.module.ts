import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { FechamentoMesPageRoutingModule } from './fechamento-mes-routing.module';

import { FechamentoMesPage } from './fechamento-mes.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    FechamentoMesPageRoutingModule
  ],
  declarations: [FechamentoMesPage]
})
export class FechamentoMesPageModule {}
