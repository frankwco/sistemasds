import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { MeusLancamentosPage } from './meus-lancamentos.page';

describe('MeusLancamentosPage', () => {
  let component: MeusLancamentosPage;
  let fixture: ComponentFixture<MeusLancamentosPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MeusLancamentosPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(MeusLancamentosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
