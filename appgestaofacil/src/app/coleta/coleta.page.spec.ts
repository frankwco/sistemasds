import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { IonicModule } from '@ionic/angular';

import { ColetaPage } from './coleta.page';

describe('ColetaPage', () => {
  let component: ColetaPage;
  let fixture: ComponentFixture<ColetaPage>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ColetaPage ],
      imports: [IonicModule.forRoot()]
    }).compileComponents();

    fixture = TestBed.createComponent(ColetaPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
