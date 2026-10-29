import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { FechamentoMesPage } from './fechamento-mes.page';

describe('FechamentoMesPage', () => {
  let component: FechamentoMesPage;
  let fixture: ComponentFixture<FechamentoMesPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FechamentoMesPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(FechamentoMesPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
