import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PasswordResetedComponent } from './password-reseted.component';

describe('PasswordResetedComponent', () => {
  let component: PasswordResetedComponent;
  let fixture: ComponentFixture<PasswordResetedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PasswordResetedComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PasswordResetedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
