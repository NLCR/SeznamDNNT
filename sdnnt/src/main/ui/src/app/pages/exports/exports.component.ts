import { HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Export } from 'src/app/shared/exports';
import { Import } from 'src/app/shared/import';
import { Subject, Subscription } from 'rxjs';
import { SolrResponse } from 'src/app/shared/solr-response';
import { map, startWith, debounce, debounceTime } from 'rxjs/operators';
import { FlexModule } from '@angular/flex-layout/flex';
import { PaginatorComponent } from '../../components/paginator/paginator.component';
import { DatePipe } from '@angular/common';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatTable, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow } from '@angular/material/table';
import { MatButtonToggleGroup, MatButtonToggle } from '@angular/material/button-toggle';
import { TranslateModule } from '@ngx-translate/core'; // autocomplete

@Component({
    selector: 'app-exports',
    templateUrl: './exports.component.html',
    styleUrls: ['./exports.component.scss'],
    imports: [
    FlexModule,
    PaginatorComponent,
    MatFormField,
    MatLabel,
    MatInput,
    FormsModule,
    MatIconButton,
    MatSuffix,
    MatTooltip,
    MatIcon,
    MatTable,
    MatColumnDef,
    MatHeaderCellDef,
    MatHeaderCell,
    MatCellDef,
    MatCell,
    MatButtonToggleGroup,
    MatButtonToggle,
    MatHeaderRowDef,
    MatHeaderRow,
    MatRowDef,
    MatRow,
    DatePipe,
    TranslateModule
]
})
export class ExportsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);


  private subject: Subject<string> = new Subject();

  loading: boolean;
  numFound: number;

  displayedColumns = ['indextime', 'id','export_type','stav','export_num_docs','actions'];

  exports: Export[] = [];
  query: string;

  ngOnInit(): void {
    if (!this.state.user) {
      this.router.navigate(['/']);
      return;
    }
    this.state.activePage = 'Exports';


    this.route.queryParams.subscribe(val => {
      this.query = this.route.snapshot.queryParamMap.get('exportq') ;
      this.state.prefixsearch.export = this.query;
      this.search(val);
    });

    this.subject.pipe(
      debounceTime(400)
    ).subscribe(searchTextValue => {

      this.state.navigationstore.setPage('exports', 0);

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
        queryParams = {sort: 'title_sort asc'};
      }  else {
         queryParams =  {  exportq: this.query };
      }
    } else {
      queryParams = {sort: 'title_sort asc'};

    }

    //const queryParams = (this.query && this.query.match(/^\s*euipo_/)) ? {} : {  exportq: this.query };
    this.router.navigate(['exports/export/'+ imp.id], { queryParams });

  }

  approveANDProcess(exp: Export) {

    this.service.processExport(exp.id).subscribe(() => {
      this.route.queryParams.subscribe(val => {
        this.query = this.route.snapshot.queryParamMap.get('exportq') ;
        this.state.prefixsearch.export = this.query;
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
