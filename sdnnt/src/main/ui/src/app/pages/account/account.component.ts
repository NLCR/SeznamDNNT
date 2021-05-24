import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
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

  displayedColumns = ['datum_zadani','stav','datum_vyrizeni','count', 'pozadavek','actions'];
  zadosti: Zadost[] = [];


  constructor(
    private route: ActivatedRoute,
    private service: AppService,
    public state: AppState
    ) { }


  ngOnInit(): void {
    this.state.activePage = 'Account';
    this.route.queryParams.subscribe(val => {
      this.search(val);
    });
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
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

  setStav(stav: string) {
    
  }


  setStavZadosti(stav: string) {
    
  }

  showInfo(zadost: Zadost) {
    
  }

  send(zadost: Zadost) {
    
  }
}
