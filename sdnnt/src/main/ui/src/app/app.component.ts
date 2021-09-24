import { Component } from '@angular/core';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  public isConsent: boolean = false;

  constructor(
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private route: ActivatedRoute,
    private router: Router
  ) {

  }

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
}