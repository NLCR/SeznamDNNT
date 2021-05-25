import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZadostSendDialogComponent } from './zadost-send-dialog.component';

describe('ZadostSendDialogComponent', () => {
  let component: ZadostSendDialogComponent;
  let fixture: ComponentFixture<ZadostSendDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ZadostSendDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ZadostSendDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
