import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';

import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-searchbar',
    templateUrl: './searchbar.component.html',
    styleUrls: ['./searchbar.component.scss'],
    imports: [FormsModule, MatTooltip, MatIcon, TranslateModule]
})
export class SearchBarComponent {
  private router = inject(Router);
  state = inject(AppState);
  config = inject(AppConfiguration);
  private route = inject(ActivatedRoute);


  currentPath: string;


  search() {
    const p: any = {};
    p.q = this.state.q ? (this.state.q !== '' ? this.state.q : null) : null;
    p.page = 0;
    this.router.navigate(['/search'], {
        queryParams:  { ...p, controlled: null },
        queryParamsHandling: 'merge'
      });
  }

  clearQuery() {
    this.state.q = null;
  }

  onLoginSuccess() {


    const currentUrl = this.router.url;
    // reload is necessary of if we are on the search page
    if (currentUrl.startsWith('/search')) {
      const p: any = {};
      p.q = this.state.q ? (this.state.q !== '' ? this.state.q : null) : null;
      p.page = 0;
      p.timestamp = new Date().getTime();
      this.router.navigate(['/search'], { queryParams: p, queryParamsHandling: 'merge' });
    }
  }
}
