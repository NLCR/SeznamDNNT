import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-searchbar',
  templateUrl: './searchbar.component.html',
  styleUrls: ['./searchbar.component.scss']
})
export class SearchBarComponent implements OnInit {

  constructor(
    
    private router: Router,
    public state: AppState,
    public config: AppConfiguration
    ) { }

  ngOnInit(): void {
  }

  search() {
    const p: any = {};
    p.q = this.state.q ? (this.state.q !== '' ? this.state.q : null) : null;
    p.page = 0;
    this.router.navigate(['/search'], { queryParams: p, queryParamsHandling: 'merge' });
  }

  clearQuery() {
    this.state.q = null;
  }

}
