import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogForgottenPasswordComponent } from './dialog-forgotten-password.component';

describe('InputLoginNameDialogComponent', () => {
  let component: DialogForgottenPasswordComponent;
  let fixture: ComponentFixture<DialogForgottenPasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogForgottenPasswordComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogForgottenPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
