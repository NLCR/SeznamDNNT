import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogStatesComponent } from './dialog-states.component';

describe('StatesDialogComponent', () => {
  let component: DialogStatesComponent;
  let fixture: ComponentFixture<DialogStatesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogStatesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogStatesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
