import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { SelecionarVeiculoPageRoutingModule } from './selecionar-veiculo-routing.module';

import { SelecionarVeiculoPage } from './selecionar-veiculo.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    SelecionarVeiculoPageRoutingModule
  ],
  declarations: [SelecionarVeiculoPage]
})
export class SelecionarVeiculoPageModule {}
