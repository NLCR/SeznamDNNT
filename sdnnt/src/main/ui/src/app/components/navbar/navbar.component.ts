import { Component, EventEmitter, Output, ViewChild, inject } from '@angular/core';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogLoginComponent } from '../dialog-login/dialog-login.component';
import { DialogNotificationsSettingsComponent } from '../dialog-notifications-settings/dialog-notifications-settings.component';
import { DialogRegistrationComponent } from '../dialog-registration/dialog-registration.component';
import { SearchBarComponent } from '../searchbar/searchbar.component';

import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-navbar',
    templateUrl: './navbar.component.html',
    styleUrls: ['./navbar.component.scss'],
    imports: [
    MatToolbar,
    FlexModule,
    RouterLink,
    SearchBarComponent,
    MatButton,
    MatIcon,
    RouterLinkActive,
    MatMenuTrigger,
    MatMenu,
    MatMenuItem,
    TranslateModule
]
})
export class NavbarComponent {
  private dialog = inject(MatDialog);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);
  config = inject(AppConfiguration);


  @ViewChild(SearchBarComponent) searchBarComponent: SearchBarComponent;

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();

  now = new Date();

  public isSearchBar = false;


  changeLang(lang: string) {
    this.service.changeLang(lang);
  }


  showLogin() {
    this.dialog.open(DialogLoginComponent, {
      width: '450px',
      panelClass: 'app-dialog-login',
      data: null
    }).afterClosed().subscribe(r=> {
      if (r.logged) {
        this.searchBarComponent.onLoginSuccess();
      }
    });
  }

  showUser() {
    if (this.state.user && !this.state.user.thirdpartyuser) {
      const d = this.dialog.open(DialogRegistrationComponent, {
        width: '600px',
        panelClass: 'app-dialog-login',
        data: {isRegister: false}
      });
    }
  }

  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;

      this.state.currentZadost={
        VN: null,
        NZN: null,
        VNX:null
      };

      this.state.facetsstore.reinit();

      this.state.sort.sort = this.config.sorts.sort[0];
      this.state.sort.sort_account = this.config.sorts.sort_account[0];
      this.state.sort.user_sort_account = this.config.sorts.user_sort_account[0];

      localStorage.removeItem('user');
      sessionStorage.clear();

      const redirectEndpoint = (res as { redirectEndpoint?: string }).redirectEndpoint;
      if (redirectEndpoint) {
        window.location.href = redirectEndpoint;
      } else {
        this.router.navigate(['/']);
      }
    });
  }

  linkToAccount() {
    const p: any = {};


    this.router.navigate(['/account'], { queryParams: p, queryParamsHandling: 'merge' });
  }

  register() {

    const d = this.dialog.open(DialogRegistrationComponent, {
      width: '600px',
      panelClass: 'app-register-dialog',
      data: {isRegister: true}
    });


  }


  wayfLink() {
    // configuration is on the server its redirect to identity provider or wayflink
    window.location.href = '/api/user/shib_login_redirect';
  }

  profileEnabled(): boolean {
    if (this.state.user && this.state.user.thirdpartyuser) {return false;}
    else {return true;}
  }

  settingsMenuEnabled(): boolean {
    if (this.state.user)  {return true;}
    else {return false;}
  }

  changePassEnabled(): boolean {
    if (this.state.user && this.state.user.thirdpartyuser) {return false;}
    else {return true;}
  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  };

  resetPasswd() {
    if (this.state.user && !this.state.user.thirdpartyuser) {
      this.router.navigate(['/userpswd'], {});
    }
  }

  showingSearchBar() {
    this.isSearchBar =! this.isSearchBar;
  }

  openNotificationsSettings() {
    const dialogRef = this.dialog.open(DialogNotificationsSettingsComponent, {
      width: '600px',
      panelClass: 'app-dialog-notifications-settings'
    });
  }
}
