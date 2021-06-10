import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { ZadostSendDialogComponent } from 'src/app/components/zadost-send-dialog/zadost-send-dialog.component';
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
    {id: "processed", val: "zpracovano"}
  ];

  filterType = [
    {id: "NZN", val: "navrzeno_na_zarazeni"},
    {id: "VVS", val: "navrzeno_na_vyrazeni"}
  ];
  
  loading: boolean;
  items: SolrDocument[];
  searchResponse: SolrResponse;
  facets;
  numFound: number;

  displayedColumns = ['datum_zadani','user', 'state', 'navrh','datum_vyrizeni','count', 'pozadavek','poznamka','actions'];
  zadosti: Zadost[] = [];

  stateFilter: string;
  newStavFilter: string;


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
    this.state.activePage = 'Account';
    this.route.queryParams.subscribe(val => {
      this.search(val);
      this.newStavFilter = val.new_stav;
      this.stateFilter = val.state;
    });
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
    
    // Docasne pro testovani
    p.user = this.state.user.username;
    this.items = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.searchAccount(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
      this.zadosti = resp.response.docs;
      this.loading = false;
      }
    });

  }

  setStav(navrh: string) {
    const q: any = {};
    // added by peter
    if (this.newStavFilter === navrh) {
      q.navrh = null;
    } else {
      q.navrh = navrh;
    }
    // end added by peter

    //q.new_stav = new_stav;  original

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

  removeAllFilters() {
    const q: any = {};
    q.new_stav = null;
    q.state = null;
    q.page = null;
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
    const dialogRef = this.dialog.open(ZadostSendDialogComponent, {
      width: '1150px',
      data: zadost,
      panelClass: 'app-states-dialog'
    });
    
  }

  process(zadost: Zadost) {

    if (zadost.identifiers.length > Object.keys(zadost.process).length) {
      this.service.showSnackBar('alert.oznaceni_jako_zpracovane_error', 'desc.ne_vsechny_zaznamy_jsou_zpracovane', true);
      return;
    }
    this.service.processZadost(zadost).subscribe(res => {

    });
  }
}
