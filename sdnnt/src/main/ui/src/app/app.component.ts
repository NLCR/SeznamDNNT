import { Component } from '@angular/core';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { CookieService } from 'ngx-cookie';
import { interval, Subscription } from 'rxjs';

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

    interval(10000).subscribe(x => {
      if (this.state.user != null) {
        this.service.ping().subscribe((res)=>{
          if(res.pinginguser && res.remainingtime) {
            console.log("Remaining time in secods "+res.remainingtime)
          } else {
            console.log("Expired; user is log out")
          }
        });
      }    
    });
  }

  isConsentEnabled() {
    var consent:string = localStorage.getItem("consent");
    return consent != null;
  }


}