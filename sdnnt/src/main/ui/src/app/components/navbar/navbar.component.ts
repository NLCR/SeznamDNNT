import { Component, OnInit, Output, EventEmitter  } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogLoginComponent } from '../dialog-login/dialog-login.component';
import { DialogRegistrationComponent } from '../dialog-registration/dialog-registration.component';
import { User } from 'src/app/shared/user';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {
  
  // sidenav
  @Output() public sidenavToggle = new EventEmitter();

  now = new Date();

  constructor(
    private dialog: MatDialog,
    private router: Router,
    private service: AppService,
    public state: AppState
    ) { }

  ngOnInit(): void {
  }

  changeLang(lang: string) {
    this.service.changeLang(lang);
  }

  showLogin() {
    this.dialog.open(DialogLoginComponent, {
      width: '450px',
      panelClass: 'app-dialog-login',
      data: null
    });
  }

  showUser() {
    const d = this.dialog.open(DialogRegistrationComponent, {
      width: '600px',
      panelClass: 'app-dialog-login',
      data: {isRegister: false}
    });

    // d.afterClosed().subscribe((result: User) => {
    //   if (result) {
    //     // result je user
    //     result.isActive = false;
    //     this.service.saveUser(result).subscribe((res: User) => {
    //       if (res.error) {
    //         this.service.showSnackBar('user_saving_error', res.error, true);
    //       } else {
    //         this.service.showSnackBar('Ulozeni probehlo v poradku', '', false);
    //         this.state.user = JSON.parse(JSON.stringify(result));
    //       }
    //     });
    //   }
       
    //  });
  }

  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;
      this.state.currentZadost = {VVS: null, NZN: null,VVN:null};
      localStorage.removeItem('user');
      sessionStorage.clear();
      this.router.navigate(['/']);
    });
  }

  register() {
    
    const d = this.dialog.open(DialogRegistrationComponent, {
      width: '600px',
      panelClass: 'app-register-dialog',
      data: {isRegister: true}
    });

    // d.afterClosed().subscribe((result: User) => {
    //   if (result) {
    //     // result je user
    //     result.isActive = false;
    //     this.service.saveUser(result).subscribe((res: User) => {
    //       if (res.error) {
    //         this.service.showSnackBar('user_saving_error', res.error, true);
    //       } else {
    //         this.service.showSnackBar('registrace uspesna', '', false);
    //       }
    //     });
    //   }
       
    //  });
  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }
  
  resetPasswd() {
    this.router.navigate(['/userpswd'], {});
  }

}
