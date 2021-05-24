import { Component, OnInit, Output, EventEmitter  } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { MatDialog } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { LoginDialogComponent } from '../login-dialog/login-dialog.component';
import { UserDialogComponent } from '../user-dialog/user-dialog.component';

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
    private service: AppService,
    public state: AppState
    ) { }

  ngOnInit(): void {
  }

  changeLang(lang: string) {
    this.service.changeLang(lang);
  }

  showLogin() {
    this.dialog.open(LoginDialogComponent, {
      width: '450px',
      panelClass: 'app-login-dialog',
      data: null
    });
  }

  showUser() {
    this.dialog.open(UserDialogComponent, {
      width: '450px',
      panelClass: 'app-login-dialog',
      data: null
    });
  }

  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;
    });
  }

  register() {
    // some method
  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }
  

}
