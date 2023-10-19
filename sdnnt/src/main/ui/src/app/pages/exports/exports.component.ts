import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Export } from 'src/app/shared/exports';
import { Import } from 'src/app/shared/import';
import { Subject, Subscription } from 'rxjs';
import { SolrResponse } from 'src/app/shared/solr-response';
import { map, startWith, debounce, debounceTime } from 'rxjs/operators'; // autocomplete

@Component({
  selector: 'app-exports',
  templateUrl: './exports.component.html',
  styleUrls: ['./exports.component.scss']
})
export class ExportsComponent implements OnInit {

  private subject: Subject<string> = new Subject();

  loading: boolean;
  numFound: number;

  displayedColumns = ['indextime', 'id','export_type','stav','export_num_docs','actions'];

  exports: Export[] = [];
  query:string;


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
      this.query = this.route.snapshot.queryParamMap.get('exportq') ;
      this.state.prefixsearch['export'] = this.query;
      this.search(val);
    });

    this.subject.pipe(
      debounceTime(400)
    ).subscribe(searchTextValue => {

      this.state.navigationstore.setPage("export", 0);

      const req: any = {};
      req.exportq = searchTextValue;

      
      this.router.navigate([], { queryParams: req, queryParamsHandling: 'merge' });
    });

  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
    if (this.query) {
      p.q = this.query;
    }
    this.exports = [];
    this.service.searchExports(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
        this.exports = resp.response.docs;
        this.numFound = resp.response.numFound;
        this.loading = false;
     }
    });
  }

  removeAllFilters() {
    const q: any = {};
    q.navrh = null;
    q.state = null;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  showExport(imp: Export) {
    let queryParams = {};
    if (this.query) {
      if (this.query.match(/^\s*euipo_/)) {
        queryParams = {sort: "title_sort asc"}; 
      }  else {
         queryParams =  {  exportq: this.query };
      }     
    } else {
      queryParams = {sort: "title_sort asc"}; 

    }

    //const queryParams = (this.query && this.query.match(/^\s*euipo_/)) ? {} : {  exportq: this.query };
    this.router.navigate(['exports/export/'+ imp.id], { queryParams });

  }

  approveANDProcess(exp: Export) {

    this.service.processExport(exp.id).subscribe((exp)=> {
      this.route.queryParams.subscribe(val => {
        this.query = this.route.snapshot.queryParamMap.get('exportq') ;
        this.state.prefixsearch['export'] = this.query;
        this.search(val);
      });
  });
    /*
    this.service.getImportNotControlled(imp.id).subscribe(resp => {
      if (resp.response.numFound > 0) {
        this.service.showSnackBar('nejsou vsechny zkontrollovane');
      } else {
        this.service.app(imp.id).subscribe(res => {
          this.search(this.route.snapshot.queryParams);
        });
      }
    });
    */
  }

  onFilterExportsKeyUp(target) {
    this.subject.next(target.value);
  }

  cleanFilterExports() {
    this.subject.next('');
  }
}
