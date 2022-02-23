import { Component, Input, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { ActivatedRoute, Router } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from 'src/app/shared/configuration';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-paginator',
  templateUrl: './paginator.component.html',
  styleUrls: ['./paginator.component.scss']
})
export class PaginatorComponent implements OnInit {

  @Input() numFound;
  @Input() showSort: boolean ;
  @Input() sortType: string = 'sort'; // sort vs sort_account
  @Input() storeStateKey: string;
  
  pageIndex: number;
  rows: number;
  page: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public state: AppState,
    public config: AppConfiguration) { }

  ngOnInit(): void {
    this.pageIndex = this.state.page + 1;
 
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
    params.rows = e.pageSize;
    params.page = e.pageIndex;
    this.pageIndex = e.pageIndex + 1;

    if (this.storeStateKey && this.state.navigationstore.contains(this.storeStateKey)) {
      this.state.navigationstore.setPage(this.storeStateKey, params.page);
      this.state.navigationstore.setRows(this.storeStateKey, params.rows);
    }

    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  setPage() {
    const params: any = {};
    params.page = this.pageIndex - 1;
    this.state.page = this.pageIndex - 1;
    // document.getElementById('scroll-wrapper').scrollTop = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  sortBy(sort: Sort) {

    this.state.sort[this.sortType]= sort;
    const queryParams:any = {};
    queryParams[this.sortType] = sort.field + ' ' + sort.dir;
    this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }


  

 

}
