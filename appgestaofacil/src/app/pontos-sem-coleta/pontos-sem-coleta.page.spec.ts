import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { PontosSemColetaPage } from './pontos-sem-coleta.page';

describe('PontosSemColetaPage', () => {
  let component: PontosSemColetaPage;
  let fixture: ComponentFixture<PontosSemColetaPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PontosSemColetaPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(PontosSemColetaPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
