import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogNotificationsSettingsComponent } from './dialog-notifications-settings.component';

describe('DialogNotificationsSettingsComponent', () => {
  let component: DialogNotificationsSettingsComponent;
  let fixture: ComponentFixture<DialogNotificationsSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogNotificationsSettingsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogNotificationsSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
