import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PontosComAvisosPageRoutingModule } from './pontos-com-avisos-routing.module';

import { PontosComAvisosPage } from './pontos-com-avisos.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PontosComAvisosPageRoutingModule
  ],
  declarations: [PontosComAvisosPage]
})
export class PontosComAvisosPageModule {}
