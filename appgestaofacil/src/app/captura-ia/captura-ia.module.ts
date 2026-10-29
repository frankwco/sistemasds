import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { CapturaIaPageRoutingModule } from './captura-ia-routing.module';

import { CapturaIaPage } from './captura-ia.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    CapturaIaPageRoutingModule
  ],
  declarations: [CapturaIaPage]
})
export class CapturaIaPageModule {}
