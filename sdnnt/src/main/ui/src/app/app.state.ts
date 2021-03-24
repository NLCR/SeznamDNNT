import { Observable, Subject, BehaviorSubject, ReplaySubject } from 'rxjs';
import { Params, ParamMap } from '@angular/router';
import { Configuration } from './shared/configuration';

export class AppState {

  config: Configuration;

  private _paramsProcessed: ReplaySubject<string> = new ReplaySubject(3);
  public paramsProcessed: Observable<any> = this._paramsProcessed.asObservable();

  private _stateSubject = new Subject();
  public stateChanged: Observable<any> = this._stateSubject.asObservable();

  public _configSubject = new Subject();
  public configSubject: Observable<any> = this._configSubject.asObservable();

  public currentLang: string;

  public q;

  setConfig(cfg: Configuration) {
    this.config = cfg;
    this.currentLang = cfg.lang;
  }

  processParams(searchParams: ParamMap) {
    searchParams.keys.forEach(p => {
      const param = searchParams.get(p);
      if (p === 'view') {

      } else {
        //this.addFilter(p, param, null, false);
      }
    });
    this._paramsProcessed.next();
  }
}
