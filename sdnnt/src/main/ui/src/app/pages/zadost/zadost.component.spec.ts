import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZadostComponent } from './zadost.component';

describe('ZadostComponent', () => {
  let component: ZadostComponent;
  let fixture: ComponentFixture<ZadostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ZadostComponent]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ZadostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
