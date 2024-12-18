import { Component } from '@angular/core';
import { ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { CookieService } from 'ngx-cookie';
import { interval, Subscription } from 'rxjs';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { DialogSessionExpirationComponent } from './components/dialog-session-expiration/dialog-session-expiration.component';



@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  


  constructor(
    private sesionDialog: MatDialog,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private route: ActivatedRoute,
    private router: Router,
    private cookieService: CookieService
  ) {}


  sessionDialogRef;


  ngOnInit() {

    /** Handle language */
    this.service.changeLang(this.state.currentLang);

    this.router.events.subscribe(val => {
      if (val instanceof NavigationEnd) {
        const params = this.route.snapshot.queryParamMap;
        if (params.has('lang')) {
          this.service.changeLang(params.get('lang'));
        } else if (localStorage.getItem('lang')) {
          this.service.changeLang(localStorage.getItem('lang'));
        }
        this.state.processParams(params, this.router.url);
      }
    });

    /** Handling sessions */
    interval(this.config.pinginterval*1000).subscribe(x => {
      if (this.state.user != null) {
        this.service.ping().subscribe((res)=>{

          if(res.pinginguser && res.remainingtime) {
            console.log("Remaining time "+res.remainingtime);

            if (res.remainingtime < 20) {
              if (!this.state.expirationDialog) {
                // dialog session expiration
                this.sessionDialogRef = this.sesionDialog.open(DialogSessionExpirationComponent, {
                  width: '300px',
                  panelClass: 'app-dialog-login',
                  data: {remainingtime: res.remainingtime}
                });

                this.sessionDialogRef.afterClosed().subscribe(() => {
                  this.state.expirationDialog = false;
                });
                
                this.state.expirationDialog = true;
              }                
            }
          } else {

            this.state.expirationDialog = false;

            if (this.sessionDialogRef != null) {
              this.sessionDialogRef.close();
            }

            this.service.logout().subscribe(res => {
              this.state.setLogged(res);
              this.state.logged = false;
              this.state.user = null;
              
              this.state.facetsstore.reinit();

        
              this.state.sort['sort'] = this.config.sorts.sort[0];
              this.state.sort['sort_account'] = this.config.sorts.sort_account[0];
              this.state.sort['user_sort_account'] = this.config.sorts.user_sort_account[0];
        
              localStorage.removeItem('user');
              this.router.navigate(['/home'], {});

            });
          }
        });
      }    
    });

  }

  isConsentEnabled() {
    var consent:string = localStorage.getItem("consent");
    return consent != null;
  }

  private rootRoute(route: ActivatedRoute): ActivatedRoute {
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route;
  }

}