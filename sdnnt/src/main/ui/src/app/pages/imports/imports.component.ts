import { HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Import } from 'src/app/shared/import';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { FlexModule } from '@angular/flex-layout/flex';
import { PaginatorComponent } from '../../components/paginator/paginator.component';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { NgIf, NgClass, DatePipe } from '@angular/common';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatTable, MatColumnDef, MatHeaderCellDef, MatHeaderCell, MatCellDef, MatCell, MatHeaderRowDef, MatHeaderRow, MatRowDef, MatRow } from '@angular/material/table';
import { MatButtonToggleGroup, MatButtonToggle } from '@angular/material/button-toggle';
import { ExtendedModule } from '@angular/flex-layout/extended';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-imports',
    templateUrl: './imports.component.html',
    styleUrls: ['./imports.component.scss'],
    imports: [
      FlexModule,
      PaginatorComponent,
      MatFormField,
      MatLabel,
      MatInput,
      FormsModule,
      NgIf,
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
      NgClass,
      ExtendedModule,
      DatePipe,
      TranslateModule
    ]
})
export class ImportsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);

  private subject: Subject<string> = new Subject();

  filterState = [
    { id: 'open', val: 'neodeslano' },
    { id: 'waiting', val: 'ceka_na_posouzeni' },
    { id: 'processed', val: 'zpracovano' }
  ];

  filterType = [
    { id: 'NZN', val: 'navrzeno_na_zarazeni' },
    { id: 'VVS', val: 'navrzeno_na_vyrazeni' }
  ];

  loading: boolean;
  searchResponse: SolrResponse;
  facets;
  numFound: number;

  displayedColumns = ['import_date', 'import_url', 'import_origin', 'stav', 'total', 'na_seznamu', 'actions'];
  imports: Import[] = [];
  query: string;

  stats: {[import_id: string]: {total: number; na_vyrazeni: number}} = {};

  stateFilter: string;
  newStavFilter: string;

  ngOnInit(): void {
    if (!this.state.user) {
      this.router.navigate(['/']);
      return;
    }
    this.state.activePage = 'Imports';
    this.route.queryParams.subscribe(val => {
      this.query = this.route.snapshot.queryParamMap.get('importq');
      this.state.prefixsearch.import = this.query;
      this.search(val);
      this.newStavFilter = val.navrh;
      this.stateFilter = val.state;
    });

    this.subject.pipe(
      debounceTime(400)
    ).subscribe(searchTextValue => {
      this.state.navigationstore.setPage('imports', 0);
      this.router.navigate([], {
        queryParams: { importq: searchTextValue || null, page: null },
        queryParamsHandling: 'merge'
      });
    });
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
    if (this.query) {
      p.q = this.query;
    }
    delete p.importq;

    this.imports = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.searchImports(p as HttpParams).subscribe((resp: any) => {
      if (!resp.error) {
        this.searchResponse = resp;
        //this.imports = resp.response.docs;
        this.imports = this.assignGroupClasses(resp.response.docs);
        this.numFound = resp.response.numFound;
        this.loading = false;
      }
    });

  }

  onFilterImportsKeyUp(target) {
    this.subject.next(target.value);
  }

  cleanFilterImports() {
    this.subject.next('');
  }

  setStav(navrh: string) {
    const q: any = {};
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

  showImport(imp: Import) {
    this.router.navigate(['imports/import/'+ imp.id], {queryParams:{controlled: false}});
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


  assignGroupClasses(data: any[]): any[] {
    const groupMap = new Map<string, string>();
    let isOdd = true;

    return data.map((row) => {
      if (!row.group) {
        return {
          ...row,
          rowClass: 'default-group'
        };
      }

      if (!groupMap.has(row.group)) {
        groupMap.set(row.group, isOdd ? 'odd-group' : 'even-group');
        isOdd = !isOdd;
      }

      return {
        ...row,
        rowClass: groupMap.get(row.group)
      };
    });
  }
}
