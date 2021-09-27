import { Component } from '@angular/core';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { CookieService } from 'ngx-cookie';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  
  constructor(
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private route: ActivatedRoute,
    private router: Router,
    private cookieService: CookieService
  ) {}



  ngOnInit() {

    this.service.changeLang(this.state.currentLang);

    this.router.events.subscribe(val => {
      if (val instanceof NavigationEnd) {
        const params = this.route.snapshot.queryParamMap;
        if (params.has('lang')) {
          this.service.changeLang(params.get('lang'));
        }
        this.state.processParams(params);
      }
    });
  }

  isConsentEnabled() {
    var consent:string = this.cookieService.get("consent");
    return consent != null;
  }

}