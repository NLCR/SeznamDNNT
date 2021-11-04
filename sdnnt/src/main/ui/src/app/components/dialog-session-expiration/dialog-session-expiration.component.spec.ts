import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogSessionExpirationComponent } from './dialog-session-expiration.component';

describe('DialogSessionExpirationComponent', () => {
  let component: DialogSessionExpirationComponent;
  let fixture: ComponentFixture<DialogSessionExpirationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogSessionExpirationComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogSessionExpirationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
