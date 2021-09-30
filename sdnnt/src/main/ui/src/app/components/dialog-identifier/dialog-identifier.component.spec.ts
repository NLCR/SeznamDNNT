import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogIdentifierComponent } from './dialog-identifier.component';

describe('DataDialogComponent', () => {
  let component: DialogIdentifierComponent;
  let fixture: ComponentFixture<DialogIdentifierComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogIdentifierComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogIdentifierComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
