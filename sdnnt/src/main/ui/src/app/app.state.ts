import { Observable, Subject, BehaviorSubject, ReplaySubject } from 'rxjs';
import { Params, ParamMap } from '@angular/router';
import { Configuration, Sort } from './shared/configuration';
import { User } from './shared/user';

export class AppState {

  config: Configuration;

  private _paramsProcessed: ReplaySubject<string> = new ReplaySubject(3);
  public paramsProcessed: Observable<any> = this._paramsProcessed.asObservable();

  private _stateSubject = new Subject();
  public stateChanged: Observable<any> = this._stateSubject.asObservable();

  public _configSubject = new Subject();
  public configSubject: Observable<any> = this._configSubject.asObservable();

  public currentLang: string;

  public q: string;
  public page: number = 0;
  public rows: number = 20;
  public sort: Sort;
  public sorts: Sort[];

  // Seznam stavu zaznamu pro uzivatel
  public user: User;
  public dntStates: string[] = ['PA', 'A', 'VS', 'VN', 'N', 'NZN', 'VVN', 'VVS'];

  setConfig(cfg: Configuration) {
    this.config = cfg;
    this.currentLang = cfg.lang;
    this.dntStates = cfg.dntStates[this.user ? this.user.role : 'user'];
  }

  processParams(searchParams: ParamMap) {
    searchParams.keys.forEach(p => {
      const param = searchParams.get(p);
      if (p === 'q') {
        this.q = param;
      } else {
        //this.addFilter(p, param, null, false);
      }
    });
    this._paramsProcessed.next();
  }
}
