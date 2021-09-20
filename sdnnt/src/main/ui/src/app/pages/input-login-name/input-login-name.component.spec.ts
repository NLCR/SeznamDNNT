import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InputLoginNameComponent } from './input-login-name.component';

describe('InputLoginNameComponent', () => {
  let component: InputLoginNameComponent;
  let fixture: ComponentFixture<InputLoginNameComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ InputLoginNameComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(InputLoginNameComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
