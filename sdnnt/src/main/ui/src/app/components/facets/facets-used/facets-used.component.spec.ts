import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FacetsUsedComponent } from './facets-used.component';

describe('FacetsUsedComponent', () => {
  let component: FacetsUsedComponent;
  let fixture: ComponentFixture<FacetsUsedComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FacetsUsedComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FacetsUsedComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
