import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InputLoginNameDialogComponent } from './input-login-name-dialog.component';

describe('InputLoginNameDialogComponent', () => {
  let component: InputLoginNameDialogComponent;
  let fixture: ComponentFixture<InputLoginNameDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InputLoginNameDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(InputLoginNameDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
