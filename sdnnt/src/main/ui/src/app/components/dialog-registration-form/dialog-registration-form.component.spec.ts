import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogRegistrationFormComponent } from './dialog-registration-form.component';

describe('UserFormComponent', () => {
  let component: DialogRegistrationFormComponent;
  let fixture: ComponentFixture<DialogRegistrationFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogRegistrationFormComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogRegistrationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
