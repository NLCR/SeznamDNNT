import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Import } from 'src/app/shared/import';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-exports',
  templateUrl: './exports.component.html',
  styleUrls: ['./exports.component.scss']
})
export class ExportsComponent implements OnInit {

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

  displayedColumns = ['import_date', 'import_url', 'actions'];
  imports: Import[] = [];
  stats: {[import_id: string]: {total: number, na_vyrazeni: number}} = {};

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
    this.state.activePage = 'Imports';
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

    this.imports = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.searchImports(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
        this.searchResponse = resp;
        this.imports = resp.response.docs;
        this.numFound = this.imports.length;
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

  showExport(imp: Import) {
    this.router.navigate(['exports/export/'+ imp.id], {queryParams:{controlled: false}});
  }

  process(imp: Import) {
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
