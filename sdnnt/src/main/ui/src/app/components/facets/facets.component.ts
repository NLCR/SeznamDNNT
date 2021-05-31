import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
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
    public config: AppConfiguration,
    public state: AppState
  ) { }

  ngOnInit(): void {
    //this.facets = Object.keys(this.facet_fields);
    this.facets = [];
    this.config.filterFields.forEach(f => {
      if (this.facet_fields[f]) {
        this.facets.push(f);
      }
    });
  }

  addFilter(field: string, f:{name: string, type: string, value: number}) {
    const q: any = {};
    q[field] = f.name;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }


}
