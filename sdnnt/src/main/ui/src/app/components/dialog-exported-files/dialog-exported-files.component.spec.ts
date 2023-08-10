import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogExportedFilesComponent } from './dialog-exported-files.component';

describe('DialogExportedFilesComponent', () => {
  let component: DialogExportedFilesComponent;
  let fixture: ComponentFixture<DialogExportedFilesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DialogExportedFilesComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DialogExportedFilesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
