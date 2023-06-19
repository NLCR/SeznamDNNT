import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExportItemComponent } from './export-item.component';

describe('ExportItemComponent', () => {
  let component: ExportItemComponent;
  let fixture: ComponentFixture<ExportItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ExportItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ExportItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
