import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultActionsZadostComponent } from './result-actions-zadost.component';

describe('ResultActionsZadostComponent', () => {
  let component: ResultActionsZadostComponent;
  let fixture: ComponentFixture<ResultActionsZadostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResultActionsZadostComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResultActionsZadostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
