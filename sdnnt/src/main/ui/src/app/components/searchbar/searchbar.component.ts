import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-searchbar',
  templateUrl: './searchbar.component.html',
  styleUrls: ['./searchbar.component.scss']
})
export class SearchBarComponent implements OnInit {

  currentPath: string;


  constructor(
    private router: Router,
    public state: AppState,
    public config: AppConfiguration,
    private route: ActivatedRoute
    ) { 


    }

  ngOnInit(): void {
  }

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

    
    let currentUrl = this.router.url;
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
