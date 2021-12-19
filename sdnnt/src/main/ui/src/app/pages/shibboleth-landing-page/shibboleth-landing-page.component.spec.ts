import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShibbolethLandingPageComponent } from './shibboleth-landing-page.component';

describe('ShibbolethLandingPageComponent', () => {
  let component: ShibbolethLandingPageComponent;
  let fixture: ComponentFixture<ShibbolethLandingPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ShibbolethLandingPageComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ShibbolethLandingPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
