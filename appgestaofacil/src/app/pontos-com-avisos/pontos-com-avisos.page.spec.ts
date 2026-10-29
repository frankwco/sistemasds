import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { PontosComAvisosPage } from './pontos-com-avisos.page';

describe('PontosComAvisosPage', () => {
  let component: PontosComAvisosPage;
  let fixture: ComponentFixture<PontosComAvisosPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PontosComAvisosPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(PontosComAvisosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
