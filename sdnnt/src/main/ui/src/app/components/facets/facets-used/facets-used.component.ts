import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppState } from 'src/app/app.state';
import { Filter } from 'src/app/shared/filter';

@Component({
  selector: 'app-facets-used',
  templateUrl: './facets-used.component.html',
  styleUrls: ['./facets-used.component.scss']
})
export class FacetsUsedComponent implements OnInit {

  constructor(
    private router: Router,
    public state: AppState) { }

  ngOnInit(): void {
  }

  removeFilter(filter: Filter) {
    const q: any = {};
    if (this.state.page > 0){
      q.page = 0;
      this.state.page = 0;
    }
    q[filter.field] = null;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }
  
}
