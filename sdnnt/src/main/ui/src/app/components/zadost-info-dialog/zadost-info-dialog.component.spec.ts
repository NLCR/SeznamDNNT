import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZadostInfoDialogComponent } from './zadost-info-dialog.component';

describe('ZadostInfoDialogComponent', () => {
  let component: ZadostInfoDialogComponent;
  let fixture: ComponentFixture<ZadostInfoDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ZadostInfoDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ZadostInfoDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
