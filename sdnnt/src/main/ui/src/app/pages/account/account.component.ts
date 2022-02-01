import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogDeleteRequestComponent } from 'src/app/components/dialog-delete-request/dialog-delete-request.component';
import { DialogSendRequestComponent } from 'src/app/components/dialog-send-request/dialog-send-request.component';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';


@Component({
  selector: 'app-account',
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.scss']
})
export class AccountComponent implements OnInit {

  filterState = [
    {id: "open", val: "neodeslano"},
    {id: "waiting", val: "ceka_na_posouzeni"},
    {id: "waiting_for_automatic_process", val: "ceka_na_automaticke_zpracovani"},
    {id: "processed", val: "zpracovano"}
  ];

  filterType = [
    {id: "NZN", val: "navrzeno_na_zarazeni"},
    {id: "VN", val: "navrzeno_na_vyrazeni"},
    {id: "VNZ", val: "navrzeno_na_omezeni_vnz"},
    {id: "VNL", val: "navrzeno_na_omezeni_vnl"}
  ];
  
  loading: boolean;
  items: SolrDocument[];
  searchResponse: SolrResponse;
  facets;
  numFound: number;
  escalated: boolean = false;
  expired: boolean = false;

  // displayedColumns = [
  //   'id',
  //   'datum_zadani',
  //   'user', 
  //   'institution', 
  //   'state', 
  //   'navrh',
  //   'datum_vyrizeni',
  //   'count', 
  //   'pozadavek',
  //   'poznamka',
  //   'deadline',
  //   'period',
  //   'actions'
  // ];

  displayedColumns =[];

  zadosti: Zadost[] = [];

  stateFilter: string;
  newStavFilter: string;
  institutionFilter:string;
  delegatedFilter: string;
  priorityFilter: string;
  typeOfRequestFilter: string;

  allResultInstitutions:string[] = [];
  allPriorities:string[] = [];
  allDelegated:string[] = [];
  allTypes:string[] = [];


  constructor(
    public dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState
    ) { }


  ngOnInit(): void {
    if (!this.state.user) {
      this.router.navigate(['/']);
      return;
    }

    if (this.state.user.role == 'kurator' || this.state.user.role==='mainKurator' || this.state.user.role === 'admin') {
      this.displayedColumns = [
        'id',
        'datum_zadani',
        'user', 
        'state', 
        'navrh',
        'datum_vyrizeni',
        'count', 
        'deadline',
        'desiredstate',
        'period',
        'actions'
      ];
    
    } else {
      this.displayedColumns = [
        'id',
        'datum_zadani',
        'user', 
        //'institution', 
        'state', 
        'navrh',
        'datum_vyrizeni',
        'count', 
        'pozadavek',
        'poznamka',
        'actions'
      ];

    }

    this.state.activePage = 'Account';
    this.route.queryParams.subscribe(val => {
      this.search(val);
      this.newStavFilter = val.navrh;
      this.stateFilter = val.state;
      this.priorityFilter = val.priority;
      this.institutionFilter = val.institution;
      this.delegatedFilter = val.delegated;
      this.typeOfRequestFilter = val.type_of_request;
    });
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);

    if (this.state.user?.role === 'kurator' || this.state.user?.role === 'mainKurator') {
      p.sort_account = this.state.sort.sort_account.field + " " + this.state.sort.sort_account.dir;
    } else {
      p.user_sort_account = this.state.sort.user_sort_account.field + " " + this.state.sort.user_sort_account.dir;
    }

    
    this.items = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.searchAccount(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
        this.zadosti = resp.response.docs;
        this.numFound = resp.response.numFound;
        this.loading = false;

        this.allResultInstitutions = resp.facet_counts.facet_fields.institution.filter((itm) => itm.value > 0 ).map( function(val, index){
            return val.name;
        });

        this.allPriorities = resp.facet_counts.facet_fields.priority.filter((itm) => itm.value > 0 ).map( function(val, index){
          return val.name;
        });

        this.allDelegated = resp.facet_counts.facet_fields.delegated.filter((itm) => itm.value > 0 ).map( function(val, index){
          return val.name;
        });
        this.allTypes = resp.facet_counts.facet_fields.type_of_request.filter((itm) => itm.value > 0 ).map( function(val, index){
          return val.name;
        });
      }
    });

  }


  isEscalated(z:Zadost): boolean {
    return this.state.user && (this.state.user.role==='kurator' || this.state.user.role==='mainKurator' || this.state.user.role==='admin') && z.state === 'waiting' && z.escalated
  }

  isExpired(z:Zadost): boolean {
    return this.state.user && (this.state.user.role==='kurator' || this.state.user.role==='mainKurator' || this.state.user.role==='admin') && z.state === 'waiting' && z.expired
  }

  setStav(navrh: string) {
    const q: any = {};
    // added by peter
    if (this.newStavFilter === navrh) {
      q.navrh = null;
    } else {
      q.navrh = navrh;
    }
    

    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }


  setStavZadosti(state: string) {
    const q: any = {};
    if (this.stateFilter === state) {
      q.state = null;
    } else {
      q.state = state;
    }
    
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  setInstitution(institution: string) {
    const q: any = {};
    q.institution = institution;
    if (this.institutionFilter === institution) {
      q.institution = null;
      this.institutionFilter = null;
    } else {
      this.institutionFilter  = institution;
      q.institution = institution;
    }
    //this.allResultInstitutions = this.allResultInstitutions.filter(obj => obj !== institution);
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }


  setDelegated(delegated: string) {
    const q: any = {};
    q.delegated = delegated;
    if (this.delegatedFilter === delegated) {
      q.delegated = null;
      this.delegatedFilter = null;
    } else {
      q.delegated = delegated;
      this.delegatedFilter  = delegated;
    }
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  setTypeOfRequest(typeOfReq: string) {
    const q: any = {};
    q.type_of_request = typeOfReq;
    if (this.typeOfRequestFilter === typeOfReq) {
      q.type_of_request = null;
      this.typeOfRequestFilter = null;
    } else {
      q.type_of_request = typeOfReq;
      this.typeOfRequestFilter  = typeOfReq;
    }
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });

  }

  setPriority(priority: string) {
    const q: any = {};
    q.priority = priority;
    if (this.priorityFilter === priority) {
      q.priority = null;
      this.priorityFilter = null;
    } else {
      q.priority = priority;
      this.priorityFilter  = priority;
    }
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  removeAllFilters() {
    this.stateFilter = null;
    this.newStavFilter = null;
    this.institutionFilter = null; 
    this.priorityFilter = null;
    this.delegatedFilter = null;
    this.typeOfRequestFilter = null;

    const q: any = {};
    q.navrh = null;
    q.state = null;
    q.institution = null;
    q.priority = null;
    q.delegated = null;
    q.page = null;
    q.type_of_request = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  showRecords(zadost: Zadost) {
    // const dialogRef = this.dialog.open(ZadostInfoDialogComponent, {
    //   width: '1150px',
    //   data: zadost,
    //   panelClass: 'app-states-dialog'
    // });
    this.router.navigate(['zadost', zadost.id], {});
  }

  send(zadost: Zadost) {
    // no identifiers
    if (zadost.identifiers) {
      const dialogRef = this.dialog.open(DialogSendRequestComponent, {
        width: '750px',
        data: zadost,
        panelClass: 'app-send-request'
      });

      dialogRef.afterClosed().subscribe(result => {
        this.route.queryParams.subscribe(val => {
          this.search(val);
          this.newStavFilter = val.navrh;
          this.stateFilter = val.state;
        });
      });
      
    }
  }

  countProcessed(zadost: Zadost) {
    if (zadost.process) {
      let count: number = 0;
      let stateKey = (zadost.desired_item_state ?  zadost.desired_item_state : "_");
      let licenseKey = (zadost.desired_license ?  zadost.desired_license : "_");
      zadost.identifiers.forEach(id => {
        let tablekey = id +"_("+stateKey+","+licenseKey+")";
        if ( !zadost.process[tablekey]) {
          count  += 1;
        }
      });    
      return count;
    } else return 0;
    //return zadost.process ?  Object.keys(zadost.process).length : 0;
  }

  process(zadost: Zadost) {

    let stateKey = (zadost.desired_item_state ?  zadost.desired_item_state : "_");
    let licenseKey = (zadost.desired_license ?  zadost.desired_license : "_");
    let allProcessed: boolean = true;

    if (zadost && zadost.process) {
      zadost.identifiers.forEach(id => {
        let tablekey = id +"_("+stateKey+","+licenseKey+")";
        if ( !zadost.process[tablekey]) {
          allProcessed = false;
        }
      });    
    } else {
      allProcessed = false;
    }

    if (!allProcessed) {
      this.service.showSnackBar('alert.oznaceni_jako_zpracovane_error', 'desc.ne_vsechny_zaznamy_jsou_zpracovane', true);
      return;
    }


    this.service.processZadost(zadost).subscribe(res => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        //zadost.state = 'processed';
        // type_of_deadline: string;
        // type_of_period: string;
        // deadline: Date;
        // desired_item_state: string;
        // desired_license: string;
      
        zadost.desired_license = res.desired_license;
        zadost.desired_item_state = res.desired_item_state;
        zadost.deadline = res.deadline;
        zadost.type_of_deadline = res.type_of_deadline;
        zadost.type_of_period = res.type_of_period;
        zadost.datum_vyrizeni = res.datum_vyrizeni;

        zadost.state = res.state;
      }
    });
  }

  public confirmDeleteRequest(zadost: Zadost) {
    const dialogRef = this.dialog.open(DialogDeleteRequestComponent, {
      width: '750px',
      data: zadost,
      panelClass: 'app-dialog-states'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.route.queryParams.subscribe(val => {
        this.search(val);
        this.newStavFilter = val.navrh;
        this.stateFilter = val.state;
      });
    });

  }

  public refresh() {
    
    this.route.queryParams.subscribe(val => {
      this.search(val);
      this.newStavFilter = val.navrh;
      this.stateFilter = val.state;
    });

  }

}
