import { Injectable } from '@angular/core';
// import {Http, Response, URLSearchParams} from '@angular/http';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AppState } from './app.state';
import { TranslateService } from '@ngx-translate/core';

@Injectable()
export class AppService {

    // basefolder: string = '/home/kudela/.ntk/balicky/';
    constructor(
        private http: HttpClient,
        private translate: TranslateService,
        private state: AppState) { }


    private get<T>(url: string, params: HttpParams = new HttpParams(), responseType?): Observable<T> {
        // const r = re ? re : 'json';
        const options = { params, responseType, withCredentials: true };
        return this.http.get<T>(`api/${url}`, options);
    }

    private post(url: string, obj: any) {
        return this.http.post<any>(`api${url}`, obj);
    }



  changeLang(lang: string) {
    this.translate.use(lang).subscribe(val => {
      this.state.currentLang = lang;
    });
  }

    getBalicky(update: boolean = false): Observable<any> {
        let url = 'candidates/balicky';
        // url = '/assets/balicky.json'; // comment
        const params: HttpParams = new HttpParams();
        if (update) {
            params.set('update', 'true');
        }
        return this.get(url, params);
    }
}
