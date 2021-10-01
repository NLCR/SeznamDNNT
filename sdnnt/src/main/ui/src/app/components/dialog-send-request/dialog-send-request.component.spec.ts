import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogSendRequestComponent } from './dialog-send-request.component';

describe('ZadostSendDialogComponent', () => {
  let component: DialogSendRequestComponent;
  let fixture: ComponentFixture<DialogSendRequestComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogSendRequestComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogSendRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
