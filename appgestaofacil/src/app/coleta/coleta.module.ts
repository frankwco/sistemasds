import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { ColetaPageRoutingModule } from './coleta-routing.module';

import { ColetaPage } from './coleta.page';

import { PesquisarPontosComponent } from './pesquisar-pontos/pesquisar-pontos.component';
import { PesquisarCidadesComponent } from './pesquisar-cidades/pesquisar-cidades.component';
import { PesquisarColetasComponent } from './pesquisar-coletas/pesquisar-coletas.component';
import { AssinaturaComponent } from './assinatura/assinatura.component';
import {TodasColetasComponent} from './todas-coletas/todas-coletas.component';

import { BrMaskerModule, BrMaskDirective } from 'br-mask';
import { ReactiveFormsModule } from '@angular/forms';

import { SignaturePadModule } from 'angular2-signaturepad';
import { PesquisarTelefonesComponent } from './pesquisar-telefones/pesquisar-telefones.component';




@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    ColetaPageRoutingModule,
    BrMaskerModule,
    ReactiveFormsModule,
    SignaturePadModule
  ],
  declarations: [TodasColetasComponent, ColetaPage, PesquisarPontosComponent,PesquisarCidadesComponent, AssinaturaComponent, PesquisarColetasComponent, PesquisarTelefonesComponent],
  providers: [
BrMaskDirective
]
})
export class ColetaPageModule {}
