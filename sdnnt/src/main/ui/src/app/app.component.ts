import { Component, inject, OnInit } from '@angular/core';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { ActivatedRoute, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { CookieService } from 'ngx-cookie';
import { interval } from 'rxjs';
import { AppConfiguration } from './app-configuration';
import { AppService } from './app.service';
import { AppState } from './app.state';
import { DialogSessionExpirationComponent } from './components/dialog-session-expiration/dialog-session-expiration.component';
import { FooterComponent } from './components/footer/footer.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SidenavListComponent } from './components/sidenav-list/sidenav-list.component';



@Component({
    selector: 'app-root',
    standalone: true,
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    imports: [SidenavListComponent, NavbarComponent, FlexModule, RouterOutlet, FooterComponent, MatSidenavModule, MatListModule]
})
export class AppComponent implements OnInit {
  private sesionDialog = inject(MatDialog);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  private service = inject(AppService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cookieService = inject(CookieService);



  sessionDialogRef: MatDialogRef<DialogSessionExpirationComponent> | null = null;


  ngOnInit() {

    /** Handle language */
    this.service.changeLang(this.state.currentLang);

    this.router.events.subscribe(val => {
      if (val instanceof NavigationEnd) {
        const params = this.route.snapshot.queryParamMap;
        const queryLang = params.get('lang');
        const storedLang = localStorage.getItem('lang');
        if (queryLang) {
          this.service.changeLang(queryLang);
        } else if (storedLang) {
          this.service.changeLang(storedLang);
        }
        this.state.processParams(params, this.router.url);
      }
    });

    /** Handling sessions */
    interval(this.config.pinginterval*1000).subscribe(x => {
      if (this.state.user != null) {
        this.service.ping().subscribe((res)=>{

          if(res.pinginguser) {
            if (res.remainingtime) {
              console.log('Remaining time '+res.remainingtime);

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
            }
          } else {

            this.state.expirationDialog = false;

            if (this.sessionDialogRef != null) {
              this.sessionDialogRef.close();
            }

            // A persistent login cookie can transparently create a fresh short-lived session.
            this.service.rememberLogin().subscribe((remembered: any) => {
              if (remembered && remembered.username) {
                this.state.setLogged(remembered);
              } else {
                this.state.logged = false;
                this.state.user = null;
                this.state.facetsstore.reinit();
                this.state.sort.sort = this.config.sorts.sort[0];
                this.state.sort.sort_account = this.config.sorts.sort_account[0];
                this.state.sort.user_sort_account = this.config.sorts.user_sort_account[0];
                this.router.navigate(['/home'], {});
              }
            });
          }
        });
      }
    });

  }

  isConsentEnabled() {
    const consent = localStorage.getItem('consent');
    return consent != null;
  }

  private rootRoute(route: ActivatedRoute): ActivatedRoute {
    while (route.firstChild) {
      route = route.firstChild;
    }
    return route;
  }

}
