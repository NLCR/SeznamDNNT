import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Export } from 'src/app/shared/exports';
import { Import } from 'src/app/shared/import';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-exports',
  templateUrl: './exports.component.html',
  styleUrls: ['./exports.component.scss']
})
export class ExportsComponent implements OnInit {

  // id: string;
  // date: Date;
  // processed: boolean;
  // num_docs: number;


  filterState = [
    { id: "open", val: "neodeslano" },
    { id: "waiting", val: "ceka_na_posouzeni" },
    { id: "processed", val: "zpracovano" }
  ];

  filterType = [
    { id: "NZN", val: "navrzeno_na_zarazeni" },
    { id: "VVS", val: "navrzeno_na_vyrazeni" }
  ];

  loading: boolean;
  searchResponse: SolrResponse;
  facets;
  numFound: number;

  displayedColumns = ['indextime', 'id','export_type','stav','export_num_docs','actions'];

  exports: Export[] = [];


  stateFilter: string;
  newStavFilter: string;

  constructor(
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
    this.state.activePage = 'Exports';
    this.route.queryParams.subscribe(val => {
      this.search(val);
      this.newStavFilter = val.navrh;
      this.stateFilter = val.state;
    });

    this.state.prefixsearch['account'] = '';
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);

    this.exports = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.searchExports(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
        this.searchResponse = resp;
        this.exports = resp.response.docs;
        this.numFound = this.exports.length;
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
    q.navrh = null;
    q.state = null;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  showExport(imp: Export) {
    this.router.navigate(['exports/export/'+ imp.id], {queryParams:{controlled: false}});
  }

  process(imp: Export) {
    this.service.getImportNotControlled(imp.id).subscribe(resp => {
      if (resp.response.numFound > 0) {
        this.service.showSnackBar('nejsou vsechny zkontrollovane');
      } else {
        this.service.setImportProcessed(imp.id).subscribe(res => {
          this.search(this.route.snapshot.queryParams);
        });
      }
    });
  }

  // not delete
  cleanFilterExport() {
   // to do
  }
}
