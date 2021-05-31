import { HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit, OnDestroy {

  subs: Subscription[] = [];

  loading: boolean;
  docs: SolrDocument[];
  searchResponse: SolrResponse;
  facets;
  numFound: number;
  hasStateFilter: boolean;

  constructor(
    private route: ActivatedRoute,
    private service: AppService,
    public state: AppState
  ) { }


  ngOnInit(): void {
    this.state.activePage = 'Search';
    this.subs.push(this.route.queryParams.subscribe(val => {
      this.search(val);
    }));
    this.subs.push(this.state.paramsProcessed.subscribe(val => {
      this.hasStateFilter = this.state.usedFilters.findIndex(f => f.field === 'marc_990a') > -1;
    }));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
    this.docs = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {

      this.searchResponse = resp;
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
      this.facets = resp.facet_counts.facet_fields;
      this.loading = false;
    });

  }

  addToZadost() {
    if (!this.hasStateFilter) {
      return;
    }
    let isZarazeno = false;
    isZarazeno = this.state.usedFilters.find(f => f.field === 'marc_990a').value.includes('A');
    const new_stav = isZarazeno ? 'VVS' : 'NZN'
    if (!this.state.currentZadost[new_stav]) {
      const z = new Zadost(new Date().getTime() + '', this.state.user.username);
      z.new_stav = new_stav;
      z.identifiers = [];
      this.state.currentZadost[new_stav] = z;
    }
    this.state.currentZadost[new_stav].identifiers = this.state.currentZadost[new_stav].identifiers.concat(this.docs.map(doc => doc.identifier));
    this.service.saveZadost(this.state.currentZadost[new_stav]).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('add_to_zadost_error', res.error, true);
      } else {
        this.service.showSnackBar('add_to_zadost_uspesna', '', false);
      }
    });
  }

}
