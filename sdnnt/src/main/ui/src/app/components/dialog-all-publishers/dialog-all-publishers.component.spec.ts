import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogAllPublishersComponent } from './dialog-all-publishers.component';

describe('DialogAllPublishersComponent', () => {
  let component: DialogAllPublishersComponent;
  let fixture: ComponentFixture<DialogAllPublishersComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogAllPublishersComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogAllPublishersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
