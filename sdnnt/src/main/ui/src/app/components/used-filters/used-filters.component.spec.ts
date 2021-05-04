import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsedFiltersComponent } from './used-filters.component';

describe('UsedFiltersComponent', () => {
  let component: UsedFiltersComponent;
  let fixture: ComponentFixture<UsedFiltersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UsedFiltersComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UsedFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
