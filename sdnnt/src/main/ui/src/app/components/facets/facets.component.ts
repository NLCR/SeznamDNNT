import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
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
  @Input() stats:{ [field: string]: {min: any, max: any, count: number, from: any, until: any}};
  facets: string[];

  rokoddate = new FormControl(new Date());
  rokod: number;
  rokdodate = new FormControl(new Date());
  rokdo: number;

  showRoky: boolean;

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

    if (this.stats) {
      this.rokod = this.stats['rokvydani'].min;
      this.rokdo = this.stats['rokvydani'].max;
      this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
      this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
      this.showRoky = true;
    }

  }

  setNotis() {
    const q: any = {};
    q.withNotification = this.state.withNotification;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  addFilter(field: string, f:{name: string, type: string, value: number}) {
    const q: any = {};
    q[field] = f.name;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  chosenYearHandler(normalizedYear: Date, datepicker: any, field: string) {
    console.log(normalizedYear)
    if (field === 'from') {
      this.rokod = normalizedYear.getFullYear();
    } else {
      this.rokdo = normalizedYear.getFullYear();
    }

    this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
    this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
    console.log(this.rokod, this.rokdo)
    datepicker.close();
  }

  clickRokFacet() {
    const params: any = {};
    params['rokvydani'] = this.rokod + ',' + this.rokdo;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

}
