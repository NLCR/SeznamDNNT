import { Component, Input, OnInit, inject } from '@angular/core';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { Sort } from 'src/app/shared/configuration';

import { FormsModule } from '@angular/forms';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatOption, MatSelect, MatSelectTrigger } from '@angular/material/select';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-paginator',
    templateUrl: './paginator.component.html',
    styleUrls: ['./paginator.component.scss'],
    imports: [FlexModule, MatPaginator, MatFormField, MatLabel, MatSelect, FormsModule, MatSelectTrigger, MatIcon, MatOption, TranslateModule]
})
export class PaginatorComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  state = inject(AppState);
  config = inject(AppConfiguration);


  @Input() numFound;
  @Input() showSort: boolean ;
  @Input() sortType = 'sort'; // sort vs sort_account
  @Input() storeStateKey: string;
  @Input() rowsParam = 'rows';

  pageIndex: number;
  rows: number;
  page: number;

  ngOnInit(): void {
    this.pageIndex = this.state.page + 1;

    this.state.navigationstore.paginatorChanged.subscribe(r => {
      this.setPagParams();
    });
    this.setPagParams();

  }

  setPagParams() {
    if (this.storeStateKey && this.state.navigationstore.contains(this.storeStateKey)) {
      this.rows = this.state.navigationstore.getRows(this.storeStateKey);
      this.page = this.state.navigationstore.getPage(this.storeStateKey);
    } else {
      this.rows = this.state.config.rows;
      this.page = 0;
    }

  }

  pageChanged(e: PageEvent) {
    const params: any = {};
    params[this.rowsParam] = e.pageSize;
    if (this.rowsParam !== 'rows') {
      params.rows = null;
    }
    params.page = e.pageIndex;
    this.pageIndex = e.pageIndex + 1;

    if (this.storeStateKey && this.state.navigationstore.contains(this.storeStateKey)) {
      this.state.navigationstore.setPage(this.storeStateKey, params.page);
      this.state.navigationstore.setRows(this.storeStateKey, e.pageSize);
    }

    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setPage() {
    const params: any = {};
    params.page = this.pageIndex - 1;
    this.state.page = this.pageIndex - 1;

    if (this.storeStateKey && this.state.navigationstore.contains(this.storeStateKey)) {
      this.state.navigationstore.setPage(this.storeStateKey, params.page);
    }
    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  sortBy(sort: Sort) {
    this.state.sort[this.sortType]= sort;
    const queryParams: any = {};
    queryParams[this.sortType] = sort.field + ' ' + sort.dir;
    this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }



  configSorts() {
    return this.config.sorts[this.sortType];
  }


}
