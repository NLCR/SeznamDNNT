import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExpressionDialogComponent } from './expression-dialog.component';

describe('ExpressionDialogComponent', () => {
  let component: ExpressionDialogComponent;
  let fixture: ComponentFixture<ExpressionDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ExpressionDialogComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ExpressionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
