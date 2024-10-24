import { BrowserModule } from '@angular/platform-browser';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { MAT_DATE_LOCALE } from '@angular/material/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomeComponent } from './pages/home/home.component';
import { HelpComponent } from './pages/help/help.component';
import { AccountComponent } from './pages/account/account.component';
import { AdminComponent } from './pages/admin/admin.component';
import { SearchComponent } from './pages/search/search.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SearchBarComponent } from './components/searchbar/searchbar.component';
import { ResultItemComponent } from './components/result-item/result-item.component';
import { FacetsComponent } from './components/facets/facets.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TranslateModule, TranslateLoader, TranslateService } from '@ngx-translate/core';
import { AppMaterialModule } from './app-material.module';
import { FlexLayoutModule } from '@angular/flex-layout';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { PaginatorComponent } from './components/paginator/paginator.component';
import { PaginatorI18n } from './components/paginator/paginator-i18n';
import { DialogHistoryComponent } from './components/dialog-history/dialog-history.component';
import { DialogStatesComponent } from './components/dialog-states/dialog-states.component';
import { DialogLoginComponent } from './components/dialog-login/dialog-login.component';
import { DialogIdentifierComponent } from './components/dialog-identifier/dialog-identifier.component';
import { DialogChangePasswordComponent } from './components/dialog-change-password/dialog-change-password.component';

import { SidenavListComponent } from './components/sidenav-list/sidenav-list.component';
import { FacetsUsedComponent } from './components/facets/facets-used/facets-used.component';
import { AccountItemComponent } from './components/account-item/account-item.component';
import { DialogRegistrationFormComponent } from './components/dialog-registration-form/dialog-registration-form.component';
import { DialogRegistrationComponent } from './components/dialog-registration/dialog-registration.component';
import { DialogSendRequestComponent } from './components/dialog-send-request/dialog-send-request.component';
import { ZadostComponent } from './pages/zadost/zadost.component';
import { DialogPromptComponent } from './components/dialog-prompt/dialog-prompt.component';
import { ExpressionDialogComponent } from './components/expression-dialog/expression-dialog.component';
import { ImportsComponent } from './pages/imports/imports.component';
import { ImportComponent } from './pages/imports/import/import.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';

import { MatPasswordStrengthModule } from '@angular-material-extensions/password-strength';
import { GranularityComponent } from './components/granularity/granularity.component';
import { InputLoginNameComponent } from './pages/input-login-name/input-login-name.component';
import { DialogForgottenPasswordComponent } from './components/dialog-forgotten-password/dialog-forgotten-password.component';
import { ConsentComponent } from './components/consent/consent.component';
import { CookieModule } from 'ngx-cookie';
import { DialogSessionExpirationComponent } from './components/dialog-session-expiration/dialog-session-expiration.component';
import { DialogDeleteRequestComponent } from './components/dialog-delete-request/dialog-delete-request.component';
import { DialogCorrespondenceComponent } from './components/dialog-correspondence/dialog-correspondence.component';
import { ShibbolethLandingPageComponent } from './pages/shibboleth-landing-page/shibboleth-landing-page.component';
import { DialogBulkProposalComponent } from './components/dialog-bulk-proposal/dialog-bulk-proposal.component';
import { GraphsComponent } from './components/graphs/graphs.component';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { NgxEchartsModule } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { LineChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components';
// Import the Canvas renderer, note that introducing the CanvasRenderer or SVGRenderer is a required step
import { CanvasRenderer } from 'echarts/renderers';
import 'echarts/theme/macarons.js';
import { DialogBulkNotificationsComponent } from './components/dialog-bulk-notifications/dialog-bulk-notifications.component';
import { DialogNotificationsSettingsComponent } from './components/dialog-notifications-settings/dialog-notifications-settings.component';
import { DialogSuccessorRecordsComponent } from './components/dialog-successor-records/dialog-successor-records.component';
import { ExportsComponent } from './pages/exports/exports.component';
import { ExportComponent } from './pages/exports/export/export.component';
import { ExportItemComponent } from './components/export-item/export-item.component';
import { DialogExportedFilesComponent } from './components/dialog-exported-files/dialog-exported-files.component';
import { ResultActionsSearchComponent } from './components/result-item/result-actions-search/result-actions-search.component';
import { ResultActionsExportComponent } from './components/result-item/result-actions-export/result-actions-export.component';
import { ResultActionsZadostComponent } from './components/result-item/result-actions-zadost/result-actions-zadost.component';
import { DialogAllPublishersComponent } from './components/dialog-all-publishers/dialog-all-publishers.component';
//import { NgxJsonViewerModule } from 'ngx-json-viewer';

echarts.use([TitleComponent, TooltipComponent, GridComponent, BarChart, LineChart,CanvasRenderer]);

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

export function createTranslateLoader(http: HttpClient) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json?'+AppConfiguration.sdnntClientVersion);
}

export function createCustomMatPaginatorIntl(
  translateService: TranslateService
  ) {return new PaginatorI18n(translateService);}

const providers: any[] =[
  { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
  AppState, AppConfiguration, HttpClient, 
  { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
  TranslateService,
  {
    provide: MatPaginatorIntl, deps: [TranslateService],
    useFactory: createCustomMatPaginatorIntl
  },
  DatePipe, DecimalPipe, AppService
];

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    HelpComponent,
    AccountComponent,
    AdminComponent,
    SearchComponent,
    FooterComponent,
    NavbarComponent,
    SearchBarComponent,
    ResultItemComponent,
    FacetsComponent,
    PaginatorComponent,
    DialogHistoryComponent,
    DialogStatesComponent,
    DialogLoginComponent,
    DialogIdentifierComponent,
    DialogChangePasswordComponent,
    DialogForgottenPasswordComponent,
    DialogSessionExpirationComponent,

    SidenavListComponent,
    FacetsUsedComponent,
    AccountItemComponent,
    DialogRegistrationFormComponent,
    DialogRegistrationComponent,
    DialogSendRequestComponent,
    ZadostComponent,
    DialogPromptComponent,
    ExpressionDialogComponent,
    ImportsComponent,
    ImportComponent,
    ResetPasswordComponent,

    GranularityComponent,
    InputLoginNameComponent,
    ConsentComponent,
    DialogDeleteRequestComponent,
    DialogCorrespondenceComponent,
    ShibbolethLandingPageComponent,
    DialogBulkProposalComponent,
    GraphsComponent,
    DialogBulkNotificationsComponent,
    DialogNotificationsSettingsComponent,
    DialogSuccessorRecordsComponent,
    ExportsComponent,
    ExportComponent,
    ExportItemComponent,
    DialogExportedFilesComponent,
    ResultActionsSearchComponent,
    ResultActionsExportComponent,
    ResultActionsZadostComponent,
    DialogAllPublishersComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    CommonModule,
    AppRoutingModule,
    AppMaterialModule,
    FlexLayoutModule,
    /*NgxJsonViewerModule,*/
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: (createTranslateLoader),
        deps: [HttpClient]
      }
    }),
    AppRoutingModule,
    MatPasswordStrengthModule.forRoot(),
    BrowserAnimationsModule,
    AngularEditorModule,
    CookieModule.forRoot(),
    
    NgxEchartsModule.forRoot({ 
      echarts
    }),

    //UserIdleModule.forRoot({idle: 600, timeout: 300, ping: 120})
  ],
  providers,
  
  bootstrap: [AppComponent]
})
export class AppModule { }
