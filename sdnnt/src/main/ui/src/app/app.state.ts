import { ParamMap } from '@angular/router';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { Configuration, Sort } from './shared/configuration';
import { FacetsStore } from './shared/facetstore';
import { Filter } from './shared/filter';
import { NavigationStore } from './shared/navigationstore';
import { NotifSettings } from './shared/notifsettings';
import { User } from './shared/user';
import { Zadost } from './shared/zadost';

export class AppState {

  config!: Configuration;

  private _paramsProcessed: ReplaySubject<void> = new ReplaySubject(3);
  public paramsProcessed: Observable<void> = this._paramsProcessed.asObservable();

  private _stateSubject = new Subject();
  public stateChanged: Observable<any> = this._stateSubject.asObservable();

  public _configSubject = new Subject();
  public configSubject: Observable<any> = this._configSubject.asObservable();

  private loggedSubject: Subject<boolean> = new Subject();
  public loggedChanged: Observable<boolean> = this.loggedSubject.asObservable();

  public currentLang = '';
  public activePage = '';
  public loading = true;

  public q = '';
  public page = 0;
  public rows = 20;


  // page,rows
  public navigationstore: NavigationStore = new NavigationStore();
  // facets
  public facetsstore: FacetsStore = new FacetsStore();
  // prefix search
  public prefixsearch: {[key: string]: string} = {};


  // store for sort
  public sort: {[key: string]: Sort} = {};

  //public prefix: {[key: string]: Sort} = {};

  // vyhodit ??
  public rokvydani = '';

  // Seznam stavu zaznamu pro uzivatel
  public user: User | null = null;
  public logged = false;
  public expirationDialog = false;


  public consent = true;

  public usedFilters: Filter[] = [];

  public fullCatalog = false;
  public withNotification = false;

  // Notifications filter
  public notificationSettings: NotifSettings = new NotifSettings();



  // Aktualni zadost kam se pridavaji navrhy; zmenit
  // NZN, VN, VN_, VNL (VNL a VNZ je typ na omezeni na terminal tedy jeden typ )
  // uzivatel ma jednu zadost na omezeni na terminal a rozhodne se podle ceho ji posle
  //currentZadost: {VVS: Zadost, VVN: Zadost, NZN: Zadost} = {VVS: null, NZN: null, VVN:null};
  currentZadost: Record<'NZN' | 'VN' | 'VNX', Zadost | null> = {
    VN: null,
    NZN: null,
    VNX:null
  };

  setConfig(cfg: Configuration) {
    this.config = cfg;
    this.currentLang = cfg.lang;
  }


  processParams(searchParams: ParamMap, url: string) {

    const navigationKey = this.navigationstore.findKeyFromUrl(url);

    this.usedFilters = [];

    if (!this.sort.sort) {
      this.sort.sort = this.config.sorts.sort[0];
    }
    if (!this.sort.sort_account) {
      this.sort.sort_account = this.config.sorts.sort_account[0];
    }
    if (!this.sort.user_sort_account) {
      this.sort.user_sort_account = this.config.sorts.user_sort_account[0];
    }


    this.fullCatalog = false;
    this.withNotification = false;
    this.notificationSettings.selected = null;

    searchParams.keys.forEach(p => {
      const param = searchParams.get(p);
      const params = searchParams.getAll(p);
      if (param === null) {
        return;
      }

      if (p === 'q') {
        this.q = param;
      } else if (p === 'rows') {
        this.rows = parseInt(param, 10);
        if (navigationKey) {
          this.navigationstore.setRows(navigationKey, this.rows);
        }
      } else if (p === 'importRows') {
        if (navigationKey) {
          this.navigationstore.setRows(navigationKey, parseInt(param, 10));
        }
      } else if (p === 'page') {
        this.page = parseInt(param, 10);
        if (navigationKey) {
          this.navigationstore.setPage(navigationKey, this.page);
        }
      } else if (p === 'fullCatalog') {
        this.fullCatalog = param === 'true';
      } else if (p === 'withNotification') {
        this.withNotification = param === 'true';
      } else if (p === 'sort') {
        //this.sort = this.config.sorts.find(s => param === (s.field + " " + s.dir));
        this.sort.sort = this.config.sorts.sort.find(s => param === (s.field + ' ' + s.dir)) ?? this.sort.sort;
      } else if (p === 'sort_account') {
        //this.sort = this.config.sorts.find(s => param === (s.field + " " + s.dir));
        this.sort.sort_account = this.config.sorts.sort_account.find(s => param === (s.field + ' ' + s.dir)) ?? this.sort.sort_account;
      } else if (p === 'user_sort_account') {
        //this.sort = this.config.sorts.find(s => param === (s.field + " " + s.dir));
        this.sort.user_sort_account = this.config.sorts.user_sort_account.find(s => param === (s.field + ' ' + s.dir)) ?? this.sort.user_sort_account;
      } else if (p === 'notificationFilter') {

         const selectedNotification = this.notificationSettings.all.find(n=> n.id === param);
         this.notificationSettings.selected = selectedNotification ?? null;

      } else {
        if (this.config.filterFields.includes(p)) {
          if (params.length>1) {
            params.forEach(op=> {
              this.usedFilters.push({field: p, value: op});
            });
          } else {
            this.usedFilters.push({field: p, value: param});
          }

        }
      }
    });
    this._paramsProcessed.next();
  }


  setNavigationStateUI() {

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
        (res.zadost as Zadost[]).forEach((z: Zadost) => {
          if (z.navrh === 'NZN') {
            this.currentZadost.NZN = z;
          }else if (z.navrh === 'VN') {
            this.currentZadost.VN = z;
          }else if (z.navrh === 'VNZ' || z.navrh === 'VNL') {
            this.currentZadost.VNX = z;
          }
        });

      }

      if (res.notificationsettings) {
        this.notificationSettings = res.notificationsettings;
      }
    }
    this.loggedSubject.next(changed === this.logged);
  }


  isNavigationStoreKeyInitialized(key: string){
    return this.navigationstore.contains(key);
  }

  initDefaultNavitionStoreKey(key: string) {
    this.navigationstore.initDefault(key);
  }
}
