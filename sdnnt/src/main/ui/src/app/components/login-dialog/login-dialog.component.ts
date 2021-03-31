import { Component, OnInit, Inject } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-login-dialog',
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.scss']
})
export class LoginDialogComponent implements OnInit {

  user: string;
  pwd: string;
  loginError: boolean;
  loading: boolean;

  constructor(
    public dialogRef: MatDialogRef<LoginDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
  }

  login() {
    this.loading = true;
    this.service.login(this.user, this.pwd).subscribe(res => {
      this.state.setLogged(res);
      if (res.error) {
        this.loginError = true;
      } else {
        this.loginError = false;
        this.user = '';
        this.pwd = '';
        this.loading = false;
        this.dialogRef.close();
      }
    });
  }

  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;
      this.dialogRef.close();
    });
  }

  showFav() {
    alert('TODO');
  }

  focusp(e, el) {
    el.focus();
  }

}
