import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { DadosPontosPageRoutingModule } from './dados-pontos-routing.module';

import { DadosPontosPage } from './dados-pontos.page';

//import { PesquisarPontosComponent } from '../coleta/';
//import { PesquisarCidadesComponent } from './pesquisar-cidades/pesquisar-cidades.component';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    DadosPontosPageRoutingModule
  ],
  declarations: [DadosPontosPage]
})
export class DadosPontosPageModule {}
