import { Injectable } from '@angular/core';
// import {Http, Response, URLSearchParams} from '@angular/http';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, Subject, throwError  } from 'rxjs';
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
  private langSubject: Subject<boolean> = new Subject();
  public langChanged: Observable<boolean> = this.langSubject.asObservable();

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
      finalize(() => {
        if (showingLoading) {
          this.stopLoading();
        }
      })
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
      console.error(
        `Backend returned code ${error.status}, body was: `, error.error);
    }
    // Return an observable with a user-facing error message.
    return throwError({'status':error.status, 'message': error.message});
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
      localStorage.setItem("lang", this.state.currentLang);
      this.langSubject.next();
    });
  }

  /** catalog serach  */
  search(params: HttpParams): Observable<any> {
    let url = 'search/catalog';
    return this.get(url, params);
  }

  details(id:string[]): Observable<any> {
    let url = 'search/details';
    let ids = id.join(",");
    const params: HttpParams = new HttpParams().set('identifiers', ids);
    return this.get(url, params);
  }

  searchAccount(params: HttpParams): Observable<any> {
    let url = 'account/search';
    return this.get(url, params);
  }


  processExport(id: string): Observable<any> {
    let url = '/iexports/process_export';
    const params: HttpParams = new HttpParams().set('export', id);
    return this.get(url, params);
  }


  searchExports(params: HttpParams): Observable<any> {
    let url = '/iexports/search';
    return this.get(url, params);
  }

  searchInExports(params: HttpParams): Observable<any> {
    let url = '/iexports/search_export';
    return this.get(url, params);
  }

  getExport(id: string): Observable<any> {
    let url = '/iexports/export';
    const params: HttpParams = new HttpParams().set('export', id);
    return this.get(url, params);
  }

  getExportFiles(id: string): Observable<any> {
    let url = '/iexports/exported_files_desc';
    const params: HttpParams = new HttpParams().set('export', id);
    return this.get(url, params);
  }

  

  searchImports(params: HttpParams): Observable<any> {
    let url = 'search/imports';
    return this.get(url, params);
  }

  getImport(id: string): Observable<any> {
    let url = 'search/import';
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(url, params);
  }

  getImportDocuments(params: HttpParams): Observable<any> {
    let url = 'search/import_documents';
    return this.get(url, params);
  }

  getImportNotControlled(id: string): Observable<any> {
    let url = 'search/import_not_controlled';
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(url, params);
  }

  getGitInfo(): Observable<any>{
    let url = 'info/git';
    return this.get(url);
  }

  approveNavrhInImport(identifier: string, importId: string): Observable<any> {
    let url = '/account/approve_navrh_in_import';
    const params: HttpParams = new HttpParams();
      //.set('user', this.state.user.username);
    return this.post(url, { identifier, importId }, params);
  }

  getHistory(identifier: string): Observable<any> {
    let url = 'search/history';
    const params: HttpParams = new HttpParams().set('identifier', identifier);
    return this.get(url, params);
  }

  getCatalogDoc(identifier: string): Observable<any> {
    let url = 'search/catalog_doc';
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

  changeStavDirect(identifier: string, newStav: string, newLicense: string, poznamka: string, granularity: any[]): Observable<any> {
    console.log(granularity);
    let url = '/account/change_stav_direct';
    return this.post(url, {identifier, newStav, newLicense, poznamka, granularity});
  }

  saveZadost(zadost: Zadost): Observable<any> {
    let url = '/account/save_zadost';
    const params: HttpParams = new HttpParams();
    return this.post(url, zadost, params);
  }

  sendZadost(zadost: Zadost): Observable<any> {
    let url = '/account/send_zadost';
    const params: HttpParams = new HttpParams();
    return this.post(url, zadost, params);
  }



  saveKuratorZadost(zadost: Zadost): Observable<any> {
    let url = '/account/save_kurator_zadost';
    const params: HttpParams = new HttpParams();
      //.set('user', this.state.user.username);
    return this.post(url, zadost, params);
  }



  addFRBRToZadost(zadost: Zadost, frbr: string): Observable<any> {
    let url = '/account/add_frbr_to_zadost';
    const params: HttpParams = new HttpParams()
      //.set('user', this.state.user.username)
      .set('frbr', frbr);
    return this.post(url, zadost, params);
  }

  deleteZadost(zadost: Zadost): Observable<any> {
    let url = '/account/delete';
    const params: HttpParams = new HttpParams();
    return this.post(url, zadost, params);
  }

  processZadost(zadost: Zadost): Observable<any> {
    let url = '/account/process_zadost';
    const params: HttpParams = new HttpParams();
    return this.post(url, zadost, params);
  }

  approveItem(identifier: string, zadost: Zadost, reason: string, alternative: string, options: string): Observable<string> {
    let url = '/account/approve';
    let params: HttpParams = new HttpParams();
    if (alternative != null) {
      params = params.append('alternative', alternative);
    }
    return this.post(url, { identifier, zadost, reason, options }, params);
  }

  approveItems(identifiers: string[], zadost: Zadost, reason: string, alternative: string, options: string): Observable<string> {
    let url = '/account/approve';
    let params: HttpParams = new HttpParams();
    if (alternative != null) {
      params = params.append('alternative', alternative);
    }
    return this.post(url, { identifiers, zadost, reason, options}, params);
  }

  approveItemsBatch(identifiers: string[], zadost: Zadost, reason: string, alternative: string, options: string): Observable<string> {
    let url = '/account/approve_batch';
    let params: HttpParams = new HttpParams();
    if (alternative != null) {
      params = params.append('alternative', alternative);
    }
    return this.post(url, { identifiers, zadost, reason, options}, params);
  }

  rejectItem(identifier: string, zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/reject';
    const params: HttpParams = new HttpParams();
    return this.post(url, { identifier, zadost, reason }, params);
  }

  rejectItems(identifiers: string[], zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/reject';
    const params: HttpParams = new HttpParams();
    return this.post(url, { identifiers, zadost, reason }, params);
  }



  rejectItemsBatch(identifiers: string[], zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/reject_batch';
    const params: HttpParams = new HttpParams();
    return this.post(url, { identifiers, zadost, reason }, params);
  }




  /** approve navrh lib */
  approveNavrhLib(identifier: string, zadost: Zadost, reason: string): Observable<string> {
    let url = '/account/approve_navrh_lib';
    const params: HttpParams = new HttpParams();
    return this.post(url, { identifier, zadost, reason}, params);
  }

  
  

  getZadost(id: string): Observable<any> {
    let url = 'account/get_zadost';
    const params: HttpParams = new HttpParams().set('id', id);
    return this.get(url, params);
  }

  prepareZadost(navrhy: string[]): Observable<any> {
    let url = 'account/prepare_zadost';
    let params: HttpParams = new HttpParams();

    for (let navrh of navrhy) {
      params = params.append('navrh', navrh);
    }

    return this.get(url, params);
  }

  getZadostRecords(params: HttpParams): Observable<any> {
    let url = 'account/get_zadost_records';
    return this.get(url, params);
  }

  getZadostInvalidRecords(params: HttpParams): Observable<any> {
    let url = 'account/get_zadost_invalid_records';
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

 

  validateToken(token: string): Observable<User> {
    const url = '/user/validate_pwd_token?token='+token;
    return this.get(url);
  }

  changePasswordByToken(token: string, pswd: string): Observable<User> {
    const url = '/user/change_pwd_token';
    return this.post(url, { "resetPwdToken":token, "pswd":pswd });
  }

  changePasswordByUser( pswd: string): Observable<User> {
    const url = '/user/change_pwd_user';
    return this.post(url, {  "pswd":pswd });
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


  thirdPartyUser(): Observable<User> {
    const url = '/user/shib_user_info';
    return this.get(url);
  }

  getUsers(): Observable<any> {
    let url = 'user/all';
    return this.get(url);
  }


  getUsersByPrefix(prefix: string): Observable<any> {
    let url = 'user/users_by_prefix?prefix='+prefix;
    return this.get(url);
  }

  getUsersByRole(role: string): Observable<any> {
    let url = 'user/users_by_role?role='+role;
    return this.get(url);
  }

  getInstitutions(): Observable<any> {
    let url = 'user/institutions';
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

  forgotPwd(user: string): Observable<User> {
    let url = '/user/forgot_pwd';
    return this.post(url, { username: user });
  }
  
  adminResetPwd(user: string): Observable<User> {
    let url = '/user/admin_reset_pwd';
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

  getStatsHistory(interval: string): Observable<any> {
    let url = 'search/stats_history';
    const params: HttpParams = new HttpParams().set('interval', interval);
    return this.get(url, params, false);
  }

  setImportControlled(doc): Observable<User> {
    let url = '/account/import_document_controlled';
    return this.post(url, doc);
  }

  setImportProcessed(id: string): Observable<User> {
    let url = '/account/import_processed';
    return this.post(url, {id});
  }

  changeStavImport(doc): Observable<User> {
    let url = '/account/import_stav';
    return this.post(url, doc);
  }


  /** === Notications, settings, rule notification, etc.. ===  */
  /** Save rule notification */ 
  saveRuleNotification( notification:any): Observable<string> {
    let url = '/notifications/save_rule_notification';
    return this.post(url, notification);
  }

  /** Save rule notification */ 
  getRuleNotifications(): Observable<string> {
    let url = '/notifications/get_rule_notifications';
    return this.get(url);
  }
  /** save noitification settings */ 
  savetRuleNotificationSettings(settings:any): Observable<string> {
    let url = '/notifications/save_notification_settings';
    return this.post(url, settings);
  }

  /** follow record */
  followRecord(identifier: string, follow: boolean): Observable<string> {
    let url = '/account/follow_record';
    const params: HttpParams = new HttpParams()
    .set('identifier', identifier)
    .set('follow', follow+'');
    return this.get(url, params);
  }



  /** === Keep session; ping pong, methods ===  */
  ping(): Observable<any> {
    let url = 'user/ping';
    return this.get(url, new HttpParams(), false);
  }

  pong(): Observable<any> {
    let url = 'user/pong';
    return this.get(url, new HttpParams(), false);
  }

}
