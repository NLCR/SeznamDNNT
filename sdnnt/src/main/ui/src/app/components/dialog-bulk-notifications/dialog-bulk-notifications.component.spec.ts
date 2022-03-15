import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogBulkNotificationsComponent } from './dialog-bulk-notifications.component';

describe('DialogBulkNotificationsComponent', () => {
  let component: DialogBulkNotificationsComponent;
  let fixture: ComponentFixture<DialogBulkNotificationsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogBulkNotificationsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogBulkNotificationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
