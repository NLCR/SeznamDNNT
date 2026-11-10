import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatDialog } from '@angular/material/dialog';
import { CookieService } from 'ngx-cookie';
import { AppComponent } from './app.component';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        AppComponent
      ],
      providers: [
        { provide: MatDialog, useValue: {} },
        { provide: CookieService, useValue: {} },
        { provide: AppConfiguration, useValue: {} },
        { provide: AppService, useValue: {} },
        { provide: AppState, useValue: {} }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
