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
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { AppMaterialModule } from './app-material.module';
import { FlexLayoutModule } from '@angular/flex-layout';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { PaginatorComponent } from './components/paginator/paginator.component';
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
import { ImportComponent } from './pages/import/import.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';

import { MatPasswordStrengthModule } from '@angular-material-extensions/password-strength';
import { GranularityComponent } from './components/granularity/granularity.component';
import { InputLoginNameComponent } from './pages/input-login-name/input-login-name.component';
import { DialogForgottenPasswordComponent } from './components/dialog-forgotten-password/dialog-forgotten-password.component';
import { ConsentComponent } from './components/consent/consent.component';
import { CookieModule } from 'ngx-cookie';
import { DialogSessionExpirationComponent } from './components/dialog-session-expiration/dialog-session-expiration.component';


export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http);
}

export function createTranslateLoader(http: HttpClient) {
  return new TranslateHttpLoader(http, 'assets/i18n/', '.json');
}

const providers: any[] =[
  { provide: MAT_DATE_LOCALE, useValue: 'cs-CZ' },
  AppState, AppConfiguration, HttpClient, 
  { provide: APP_INITIALIZER, useFactory: (config: AppConfiguration) => () => config.load(), deps: [AppConfiguration], multi: true },
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
    CookieModule.forRoot()

    //UserIdleModule.forRoot({idle: 600, timeout: 300, ping: 120})
  ],
  providers,
  
  bootstrap: [AppComponent]
})
export class AppModule { }
