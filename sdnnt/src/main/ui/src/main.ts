import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { enableProdMode, importProvidersFrom, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { CookieModule } from 'ngx-cookie';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxEchartsModule } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import { GridComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import 'echarts/theme/macarons.js';

import { AppComponent } from './app/app.component';
import { AppRoutingModule } from './app/app-routing.module';
import { AppConfiguration } from './app/app-configuration';
import { AppService } from './app/app.service';
import { AppState } from './app/app.state';
import { PaginatorI18n } from './app/components/paginator/paginator-i18n';
import { environment } from './environments/environment';

echarts.use([TitleComponent, TooltipComponent, GridComponent, BarChart, LineChart, CanvasRenderer]);

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideZoneChangeDetection(),
    importProvidersFrom(
      AppRoutingModule,
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useFactory: (http: HttpClient) => new TranslateHttpLoader(
            http, 'assets/i18n/', '.json?' + AppConfiguration.sdnntClientVersion
          ),
          deps: [HttpClient]
        }
      }),
      CookieModule.forRoot(),
      FlexLayoutModule,
      AngularEditorModule,
      NgxEchartsModule.forRoot({ echarts })
    ),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
    provideNativeDateAdapter(),
    AppState,
    AppConfiguration,
    AppService,
    TranslateService,
    DatePipe,
    DecimalPipe,
    { provide: MatPaginatorIntl, useFactory: (translate: TranslateService) => new PaginatorI18n(translate), deps: [TranslateService] },
    provideAppInitializer(() => inject(AppConfiguration).load())
  ]
}).catch(err => console.error(err));
