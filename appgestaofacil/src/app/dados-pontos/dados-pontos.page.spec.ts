import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { DadosPontosPage } from './dados-pontos.page';

describe('DadosPontosPage', () => {
  let component: DadosPontosPage;
  let fixture: ComponentFixture<DadosPontosPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DadosPontosPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(DadosPontosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
