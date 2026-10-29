import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PontosSemColetaPageRoutingModule } from './pontos-sem-coleta-routing.module';

import { PontosSemColetaPage } from './pontos-sem-coleta.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PontosSemColetaPageRoutingModule
  ],
  declarations: [PontosSemColetaPage]
})
export class PontosSemColetaPageModule {}
