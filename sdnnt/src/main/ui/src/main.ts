import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, provideHttpClient, withInterceptorsFromDi, withXhr } from '@angular/common/http';
import { enableProdMode, importProvidersFrom, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { bootstrapApplication } from '@angular/platform-browser';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { BarChart, LineChart } from 'echarts/charts';
import { GridComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import 'echarts/theme/macarons.js';
import { CookieModule } from 'ngx-cookie';
import { NgxEchartsModule } from 'ngx-echarts';

import { AppConfiguration } from './app/app-configuration';
import { AppRoutingModule } from './app/app-routing.module';
import { AppComponent } from './app/app.component';
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
    provideHttpClient(withXhr(), withInterceptorsFromDi()),
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
