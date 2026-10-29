import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { MeusLancamentosPageRoutingModule } from './meus-lancamentos-routing.module';

import { MeusLancamentosPage } from './meus-lancamentos.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    MeusLancamentosPageRoutingModule
  ],
  declarations: [MeusLancamentosPage]
})
export class MeusLancamentosPageModule {}
