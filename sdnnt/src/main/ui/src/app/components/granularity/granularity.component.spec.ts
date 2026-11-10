import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GranularityComponent } from './granularity.component';

describe('GranularityComponent', () => {
  let component: GranularityComponent;
  let fixture: ComponentFixture<GranularityComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [GranularityComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GranularityComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
