import { TestBed } from '@angular/core/testing';

import { ConditionsService } from './conditions.service';

describe('ConditionsService', () => {
  let service: ConditionsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConditionsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
