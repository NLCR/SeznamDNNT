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

  
  
  pageIndex: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public state: AppState,
    public config: AppConfiguration) { }

  ngOnInit(): void {
    this.pageIndex = this.state.page + 1;
  }

  pageChanged(e: PageEvent) {
    const params: any = {};
    params.rows = e.pageSize;
    params.page = e.pageIndex;
    this.pageIndex = e.pageIndex + 1;
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


  // get currentSort(): Sort {
  //   return this.state.sortmap.get(this.sortType);
  // }
  
  // set currentSort(s: Sort) {
  //   // this.state.sortmap.set(this.sortType, s);
  //   // // console.log("current sort map is "+this.state.sortmap)
  //   // // console.log("Setting current  sort to "+this.state.sortmap.get(this.sortType))
  // }

  // getSorts():Sort[] {
  //   if (this.sortType === 'sort') {
  //     return this.config.sorts
  //   } else {
  //     return this.config.sortsAccount;
  //   }
  // }

}
