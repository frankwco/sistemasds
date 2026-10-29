import { IonicModule } from '@ionic/angular';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Tab1Page } from './tab1.page';
import { ExploreContainerComponentModule } from '../explore-container/explore-container.module';
import { VeiculosComponent } from './veiculos/veiculos.component';
import { MotoristasComponent } from './motoristas/motoristas.component';

import { Tab1PageRoutingModule } from './tab1-routing.module';


@NgModule({
  imports: [
    IonicModule,
    CommonModule,
    FormsModule,
    ExploreContainerComponentModule,
    Tab1PageRoutingModule,
 
  ],
  declarations: [Tab1Page, VeiculosComponent, MotoristasComponent]
})
export class Tab1PageModule {


}
