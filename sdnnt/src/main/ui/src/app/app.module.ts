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
import { HistoryDialogComponent } from './components/history-dialog/history-dialog.component';
import { StatesDialogComponent } from './components/states-dialog/states-dialog.component';
import { LoginDialogComponent } from './components/login-dialog/login-dialog.component';
import { DataDialogComponent } from './components/data-dialog/data-dialog.component';
import { UserPswDialogComponent } from './components/user-pswdialog/user-pswdialog.component';

import { SidenavListComponent } from './components/sidenav-list/sidenav-list.component';
import { FacetsUsedComponent } from './components/facets/facets-used/facets-used.component';
import { AccountItemComponent } from './components/account-item/account-item.component';
import { UserFormComponent } from './components/user-form/user-form.component';
import { UserDialogComponent } from './components/user-dialog/user-dialog.component';
import { ZadostSendDialogComponent } from './components/zadost-send-dialog/zadost-send-dialog.component';
import { ZadostComponent } from './pages/zadost/zadost.component';
import { PromptDialogComponent } from './components/prompt-dialog/prompt-dialog.component';
import { ExpressionDialogComponent } from './components/expression-dialog/expression-dialog.component';
import { ImportsComponent } from './pages/imports/imports.component';
import { ImportComponent } from './pages/import/import.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { PasswordResetedComponent } from './pages/password-reseted/password-reseted.component';
import { MatPasswordStrengthModule } from '@angular-material-extensions/password-strength';
import { GranularityComponent } from './components/granularity/granularity.component';

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
    HistoryDialogComponent,
    StatesDialogComponent,
    LoginDialogComponent,
    DataDialogComponent,
    UserPswDialogComponent,

    SidenavListComponent,
    FacetsUsedComponent,
    AccountItemComponent,
    UserFormComponent,
    UserDialogComponent,
    ZadostSendDialogComponent,
    ZadostComponent,
    PromptDialogComponent,
    ExpressionDialogComponent,
    ImportsComponent,
    ImportComponent,
    ResetPasswordComponent,
    PasswordResetedComponent,
    GranularityComponent,
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
    AngularEditorModule
  ],
  providers,
  bootstrap: [AppComponent]
})
export class AppModule { }
