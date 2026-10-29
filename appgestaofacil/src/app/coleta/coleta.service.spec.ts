import { TestBed } from '@angular/core/testing';

import { ColetaService } from './coleta.service';

describe('ColetaService', () => {
  let service: ColetaService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ColetaService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
