import { Injectable } from '@angular/core';
// import {Http, Response, URLSearchParams} from '@angular/http';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError  } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';

import { AppState } from './app.state';
import { TranslateService } from '@ngx-translate/core';
import { User } from './shared/user';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AppConfiguration } from './app-configuration';
import { Zadost } from './shared/zadost';
import { SolrDocument } from './shared/solr-document';
import { Overlay } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { MatSpinner } from '@angular/material/progress-spinner';


@Injectable()
export class AppService {

  private spinnerTopRef = this.cdkSpinnerCreate();
  private numLoading: number = 0;

  constructor(
    private http: HttpClient,
    private translate: TranslateService,
    private snackBar: MatSnackBar,
    private overlay: Overlay,
    private config: AppConfiguration,
    private state: AppState) { }


  private get<T>(url: string, params: HttpParams = new HttpParams(), showingLoading: boolean = true, responseType?): Observable<T> {
    // const r = re ? re : 'json';
    if (showingLoading) {
      this.showLoading();
    }
    const options = { params, responseType, withCredentials: true };
    return this.http.get<T>(`api/${url}`, options).pipe(
      finalize(() => this.stopLoading())
    );
  }

  private post(url: string, obj: any, params: HttpParams = new HttpParams(), showingLoading: boolean = true) {
    if (showingLoading) {
      this.showLoading();
    }
    return this.http.post<any>(`api${url}`, obj, { params }).pipe(
      finalize(() => this.stopLoading())
    ).pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    if (error.status === 0) {
      // A client-side or network error occurred. Handle it accordingly.
      console.error('An error occurred:', error.error);
    } else {
      // The backend returned an unsuccessful response code.
      // The response body may contain clues as to what went wrong.
      console.error(
        `Backend returned code ${error.status}, body was: `, error.error);
    }
    // Return an observable with a user-facing error message.
    return throwError(
      'Something bad happened; please try again later.');
  }



  private cdkSpinnerCreate() {
    return this.overlay.create({
      hasBackdrop: true,
      // backdropClass: 'dark-backdrop',
      positionStrategy: this.overlay.position()
        .global()
        .centerHorizontally()
        .centerVertically()
    })
  }

  showLoading() {
    this.numLoading++;
    if (!this.spinnerTopRef.hasAttached()) {
      this.spinnerTopRef.attach(new ComponentPortal(MatSpinner))
    }
  }

  stopLoading() {
    this.numLoading--;
    if (this.numLoading === 0 && this.spinnerTopRef.hasAttached()) {
      this.spinnerTopRef.detach();
    }
  }

  getTranslation(s: string): string {
    return this.translate.instant(s);
  }

  showSnackBar(s: string, r: string = '', error: boolean = false) {
    const right = r !== '' ? this.getTranslation(r) : '';
    const clazz = error ? 'app-snack-error' : 'app-snack-success';
    this.snackBar.open(this.getTranslation(s), right, {
      duration: this.config.snackDuration,
      verticalPosition: 'top',
      panelClass: clazz
    });
  }

  changeLang(lang: string) {
    this.translate.use(lang).subscribe(val => {
      this.state.currentLang = lang;
    });
  }

  search(params: HttpParams): Observable<any> {
    let url = 'search/catalog';
    return this.get(url, params);
  }

  searchAccount(params: HttpParams): Observable<any> {
    let url = 'account/search';
    return this.get(url, params);
  }

  searchImports(params: HttpParams): Observable<any> {
    let url = 'search/imports';
    return this.get(url, params);
  }

  // getImport(id: string, onlyA: boolean, onlyNoEAN: boolean, onlyNoHits: boolean): Observable<any> {
  getImport(params: HttpParams): Observable<any> {
    let url = 'search/import';
    // const params: HttpParams = new HttpParams().set('id', id)
    // .set('onlyA', onlyA+'').set('onlyNoEAN', onlyNoEAN+'').set('onlyNoHits', onlyNoHits+'');
    return this.get(url, params);
  }

  approveNavrhInImport(identifier: string, importId: string): Observable<any> {
    let url = '/account/approve_navrh_in_import';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, { identifier, importId }, params);
  }

  getHistory(identifier: string): Observable<any> {
    let url = 'search/history';
    const params: HttpParams = new HttpParams().set('identifier', identifier);
    return this.get(url, params);
  }

  getExpression(frbr: string): Observable<any> {
    let url = 'search/frbr';
    const params: HttpParams = new HttpParams().set('frbr', frbr);
    return this.get(url, params);
  }

  saveRecord(id: string, raw: any): Observable<any> {
    let url = '/index/save?id=' + id;
    return this.post(url, raw);
  }

  saveZadost(zadost: Zadost): Observable<any> {
    let url = '/account/save_zadost';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, zadost, params);
  }

  addFRBRToZadost(zadost: Zadost, frbr: string): Observable<any> {
    let url = '/account/add_frbr_to_zadost';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username).set('frbr', frbr);
    return this.post(url, zadost, params);
  }

  processZadost(zadost: Zadost): Observable<any> {
    let url = '/account/process_zadost';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, zadost, params);
  }

  approveNavrh(identifier: string, zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/approve_navrh';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, { identifier, zadost, reason }, params);
  }

  approveNavrhLib(identifier: string, zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/approve_navrh_lib';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, { identifier, zadost, reason}, params);
  }

  rejectNavrh(identifier: string, zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/reject_navrh';
    const params: HttpParams = new HttpParams()
      .set('user', this.state.user.username);
    return this.post(url, { identifier, zadost, reason }, params);
  }

  getZadost(id: string): Observable<any> {
    let url = 'account/get_zadost';
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(url, params);
  }

  getZadostRecords(params: HttpParams): Observable<any> {
    let url = 'account/get_zadost_records';
    return this.get(url, params);
  }

  getText(id: string): Observable<string> {
    let url = '/texts/read';
    const params: HttpParams = new HttpParams()
      .set('id', id).set('lang', this.state.currentLang);
    return this.get(url, params, false, 'plain/text');
  }

  saveText(id: string, text: string): Observable<string> {
    let url = '/texts/write?id=' + id + '&lang=' + this.state.currentLang;
    return this.post(url, text);
  }

  activateToken(token: string): Observable<User> {
    const url = '/user/activate_pwd_token';
    // login fail 
    return this.post(url, { "resetPwdToken":token });
  }

  login(user: string, pwd: string): Observable<User> {
    const url = '/user/login';
    return this.post(url, { user, pwd });
  }

  logout() {
    const url = '/user/logout';
    return this.get(url);
    // return of({name: "", role: ""})
  }

  getUsers(): Observable<any> {
    let url = 'user/all';
    return this.get(url);
  }

  registerUser(user: User): Observable<User> {
    let url = '/user/register';
    return this.post(url, user);
  }

  saveUser(user: User): Observable<User> {
    let url = '/user/save';
    return this.post(url, user);
  }

  resetPwd(user: string): Observable<User> {
    let url = '/user/reset_pwd';
    return this.post(url, { username: user });
  }

  forgotPwd(user: string): Observable<User> {
    let url = '/user/forgot_pwd';
    return this.post(url, { username: user });
  }


  findGoogleBook(id: string): Observable<any> {
    let url = 'search/googlebooks';
    const params: HttpParams = new HttpParams().set('id', id);
    // let url = 'https://www.googleapis.com/books/v1/volumes';

    //let url = `https://books.google.com/books?jscmd=viewapi&bibkeys=${id},&callback=display_google`;
    // const params: HttpParams = new HttpParams().set('q', id);
    // const corsHeaders = new HttpHeaders({
    //   'Content-Type': 'application/json',
    //   'Accept': '*/*',
    //   'Access-Control-Allow-Origin': 'http://localhost:4200/',
    //   'sec-fetch-mode': 'no-cors'
    // });
    return this.get(url, params, false);
  }
}
