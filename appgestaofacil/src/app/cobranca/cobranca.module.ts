import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { CobrancaPageRoutingModule } from './cobranca-routing.module';

import { CobrancaPage } from './cobranca.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    CobrancaPageRoutingModule
  ],
  declarations: [CobrancaPage]
})
export class CobrancaPageModule {}
