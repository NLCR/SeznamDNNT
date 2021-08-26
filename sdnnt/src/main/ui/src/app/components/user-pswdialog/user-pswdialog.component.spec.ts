import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserPswDialogComponent } from './user-pswdialog.component';

describe('UserPswDialogComponent', () => {
  let component: UserPswDialogComponent;
  let fixture: ComponentFixture<UserPswDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserPswDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(UserPswDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
