import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppState } from 'src/app/app.state';
import { Filter } from 'src/app/shared/filter';

@Component({
  selector: 'app-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.scss']
})
export class FacetsComponent implements OnInit {

  @Input() facet_fields: {[field: string]: {name: string, type: string, value: number}[]};
  facets: string[];

  constructor(
    private router: Router,
    public state: AppState
  ) { }

  ngOnInit(): void {
    this.facets = Object.keys(this.facet_fields);
  }

  addFilter(field: string, f:{name: string, type: string, value: number}) {
    const q: any = {};
    q[field] = f.name;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
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
