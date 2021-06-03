import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { ZadostInfoDialogComponent } from 'src/app/components/zadost-info-dialog/zadost-info-dialog.component';
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

  loading: boolean;
  items: SolrDocument[];
  searchResponse: SolrResponse;
  facets;
  numFound: number;

  displayedColumns = ['datum_zadani','user', 'state', 'new_stav','datum_vyrizeni','count', 'pozadavek','poznamka','actions'];
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

  setStav(new_stav: string) {
    const q: any = {};
    q.new_stav = new_stav;
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

  showRecords(zadost: Zadost) {
    const dialogRef = this.dialog.open(ZadostInfoDialogComponent, {
      width: '1150px',
      data: zadost,
      panelClass: 'app-states-dialog'
    });

  }

  send(zadost: Zadost) {
    const dialogRef = this.dialog.open(ZadostSendDialogComponent, {
      width: '1150px',
      data: zadost,
      panelClass: 'app-states-dialog'
    });
    
  }

  process(zadost: Zadost) {
    this.service.processZadost(zadost).subscribe(res => {

    });
  }
}
