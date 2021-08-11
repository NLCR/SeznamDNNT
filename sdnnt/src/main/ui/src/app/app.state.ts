import { Observable, Subject, BehaviorSubject, ReplaySubject } from 'rxjs';
import { Params, ParamMap } from '@angular/router';
import { Configuration, Sort } from './shared/configuration';
import { User } from './shared/user';
import { Filter } from './shared/filter';
import { Zadost } from './shared/zadost';

export class AppState {

  config: Configuration;

  private _paramsProcessed: ReplaySubject<string> = new ReplaySubject(3);
  public paramsProcessed: Observable<any> = this._paramsProcessed.asObservable();

  private _stateSubject = new Subject();
  public stateChanged: Observable<any> = this._stateSubject.asObservable();

  public _configSubject = new Subject();
  public configSubject: Observable<any> = this._configSubject.asObservable();

  private loggedSubject: Subject<boolean> = new Subject();
  public loggedChanged: Observable<boolean> = this.loggedSubject.asObservable();

  public currentLang: string;
  public activePage: string;
  public loading: boolean = true;

  public q: string;
  public page: number = 0;
  public rows: number = 20;
  public sort: Sort;

  // Seznam stavu zaznamu pro uzivatel
  public user: User;
  public logged = false;
  // public dntStates: string[] = ['PA', 'A', 'VS', 'VN', 'N', 'NZN', 'VVN', 'VVS'];

  public usedFilters: Filter[] = [];

  public fullCatalog: boolean;

  // Aktualni zadost kam se pridavaji navrhy
  currentZadost: {VVS: Zadost, VVN: Zadost, NZN: Zadost} = {VVS: null, NZN: null, VVN:null};

  setConfig(cfg: Configuration) {
    this.config = cfg;
    this.currentLang = cfg.lang;
  }

  processParams(searchParams: ParamMap) {
    this.rows = this.config.rows;
    this.page = 0;
    this.usedFilters = [];
    this.sort = this.config.sorts[0];
    this.fullCatalog = false;
    searchParams.keys.forEach(p => {
      const param = searchParams.get(p);
      if (p === 'q') {
        this.q = param;
      } else if (p === 'rows') {
        this.rows = parseInt(param);
      } else if (p === 'page') {
        this.page = parseInt(param);
      } else if (p === 'fullCatalog') {
        this.fullCatalog = param === 'true';
      } else if (p === 'sort') {
        this.sort = this.config.sorts.find(s => param === (s.field + " " + s.dir));
      } else {
        if (this.config.filterFields.includes(p)) {
          this.usedFilters.push({field: p, value: param});
        }
      }
    });
    this._paramsProcessed.next();
  }

  setLogged(res: any) {
    const changed = this.logged;
    if (res.error) {
      this.logged = false;
      this.user = null;
    } else {
      this.logged = true;
      this.user = res;
      if (res.zadost) {
        // res.zadost je Array max 3 elementy. Muze mit navrh=NZN nebo navrh=VVS 
        res.zadost.forEach(z => {
          if (z.navrh === 'NZN') {
            this.currentZadost.NZN = z;
          }else if (z.navrh === 'VVS') {
            this.currentZadost.VVS = z;
          }else if (z.navrh === 'VVN') {
            this.currentZadost.VVN = z;
          }
        }); 
        
      }
    }
    this.loggedSubject.next(changed === this.logged);
  }
}
