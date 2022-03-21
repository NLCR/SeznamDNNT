import { Component, OnInit, Inject } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Router } from '@angular/router';


@Component({
  selector: 'app-dialog-login',
  templateUrl: './dialog-login.component.html',
  styleUrls: ['./dialog-login.component.scss']
})
export class DialogLoginComponent implements OnInit {

  user: string;
  pwd: string;
  loginError: boolean;
  loading: boolean;
  keepLogged: boolean;

  hidePassword:boolean = true;

  constructor(
    public dialogRef: MatDialogRef<DialogLoginComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private router: Router


  ) { }

  ngOnInit(): void {
  }

  login() {
    this.loading = true;
    this.service.login(this.user.trim(), this.pwd).subscribe(res => {
      this.state.setLogged(res);
      if (res.error) {
        this.loginError = true;
      } else {
        this.loginError = false;
        if (this.keepLogged) {
          localStorage.setItem('user', JSON.stringify({username: this.user, pwd: this.pwd, timeStamp: Date.now()}));
        }
        this.user = '';
        this.pwd = '';
        this.loading = false;
        this.dialogRef.close();

        //this.state.startTrackSession(this.bnIdle);
      }
    });
  }

  resetPwd() {
    this.dialogRef.close();
    this.router.navigate(['/fgtpswd'], {});
  }


  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;
      localStorage.removeItem('user');
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
