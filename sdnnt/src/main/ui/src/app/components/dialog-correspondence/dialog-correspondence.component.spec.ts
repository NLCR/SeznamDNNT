import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogCorrespondenceComponent } from './dialog-correspondence.component';

describe('DialogCorrespondenceComponent', () => {
  let component: DialogCorrespondenceComponent;
  let fixture: ComponentFixture<DialogCorrespondenceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogCorrespondenceComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogCorrespondenceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
