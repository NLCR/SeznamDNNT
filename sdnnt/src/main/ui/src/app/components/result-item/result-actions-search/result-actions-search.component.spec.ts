import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultActionsSearchComponent } from './result-actions-search.component';

describe('ResultActionsSearchComponent', () => {
  let component: ResultActionsSearchComponent;
  let fixture: ComponentFixture<ResultActionsSearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResultActionsSearchComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResultActionsSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
