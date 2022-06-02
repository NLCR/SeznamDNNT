import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogSuccessorRecordsComponent } from './dialog-successor-records.component';

describe('DialogSuccessorRecordsComponent', () => {
  let component: DialogSuccessorRecordsComponent;
  let fixture: ComponentFixture<DialogSuccessorRecordsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogSuccessorRecordsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogSuccessorRecordsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
