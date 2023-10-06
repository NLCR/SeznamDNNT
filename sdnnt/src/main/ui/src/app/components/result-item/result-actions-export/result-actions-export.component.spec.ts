import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultActionsExportComponent } from './result-actions-export.component';

describe('ResultActionsExportComponent', () => {
  let component: ResultActionsExportComponent;
  let fixture: ComponentFixture<ResultActionsExportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResultActionsExportComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResultActionsExportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
