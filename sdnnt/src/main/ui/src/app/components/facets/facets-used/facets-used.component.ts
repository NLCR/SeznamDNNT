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
    if (this.state.page > 0) {
      this.state.page = 0;
    }

    let vals: string[] = this.state.usedFilters
      .filter((f: Filter) => f.field === filter.field)
      .map((f: Filter) => f.value);


    vals = vals.filter(value => value !== filter.value);

    if (vals.length >= 1) {
      q[filter.field] = vals;
    } else {
      q[filter.field] = null;
    }
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  removeAll() {
    const q: any = {};
    this.state.page = 0;
    this.state.usedFilters.forEach(f => {
      q[f.field] = null;
    });
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });

  }

}
