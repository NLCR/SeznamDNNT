import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AppState } from 'src/app/app.state';
import { Filter } from 'src/app/shared/filter';
import { NgIf, NgFor, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';
import { MatList, MatListSubheaderCssMatStyler, MatListItem } from '@angular/material/list';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-facets-used',
    templateUrl: './facets-used.component.html',
    styleUrls: ['./facets-used.component.scss'],
    imports: [NgIf, MatList, MatListSubheaderCssMatStyler, FlexModule, MatTooltip, MatIcon, NgFor, MatListItem, NgSwitch, NgSwitchCase, NgSwitchDefault, TranslateModule]
})
export class FacetsUsedComponent {
  private router = inject(Router);
  state = inject(AppState);


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
