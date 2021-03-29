import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatesDialogComponent } from './states-dialog.component';

describe('StatesDialogComponent', () => {
  let component: StatesDialogComponent;
  let fixture: ComponentFixture<StatesDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StatesDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StatesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
