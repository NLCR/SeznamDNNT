import { AfterViewInit, Component, ElementRef, ViewChild, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

import { CdkScrollable } from '@angular/cdk/scrolling';
import { FlexModule } from '@angular/flex-layout/flex';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatProgressBar } from '@angular/material/progress-bar';
import { TranslateModule } from '@ngx-translate/core';


@Component({
    selector: 'app-dialog-login',
    templateUrl: './dialog-login.component.html',
    styleUrls: ['./dialog-login.component.scss'],
    imports: [
    MatProgressBar,
    MatDialogTitle,
    CdkScrollable,
    MatDialogContent,
    FlexModule,
    MatFormField,
    MatLabel,
    MatInput,
    FormsModule,
    MatIcon,
    MatSuffix,
    MatCheckbox,
    MatDialogActions,
    MatButton,
    MatDialogClose,
    TranslateModule
]
})
export class DialogLoginComponent implements AfterViewInit {
  dialogRef = inject<MatDialogRef<DialogLoginComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  private service = inject(AppService);
  private router = inject(Router);


  @ViewChild('loginUserInput') loginUserInput: ElementRef<HTMLInputElement>;

  user: string;
  pwd: string;
  loginError: boolean;
  loading: boolean;
  rememberMe: boolean;
  keepLoggedIn: boolean;

  hidePassword = true;


  ngAfterViewInit(): void {
    this.dialogRef.afterOpened().subscribe(() => {
      this.loginUserInput?.nativeElement.focus({preventScroll: true});
    });
  }

  login() {
    this.loading = true;
    this.service.login(this.user.trim(), this.pwd, this.rememberMe, this.keepLoggedIn).subscribe(res => {
      this.state.setLogged(res);
      if (res.error) {
        this.loginError = true;
      } else {
        this.loginError = false;
        this.user = '';
        this.pwd = '';
        this.loading = false;

        this.dialogRef.close({user:this.user, logged:true});
      }
    });
  }

  resetPwd() {
    this.dialogRef.close({user:null, logged:false});
    this.router.navigate(['/fgtpswd'], {});
  }


  logout() {
    this.service.logout().subscribe(res => {
      // clear state
      this.state.setLogged(res);

      this.state.logged = false;
      this.state.user = null;

      this.state.facetsstore.reinit();


      this.state.sort.sort = this.config.sorts.sort[0];
      this.state.sort.sort_account = this.config.sorts.sort_account[0];
      this.state.sort.user_sort_account = this.config.sorts.user_sort_account[0];


      this.dialogRef.close({user:null, logged:false});

    });
  }

  showFav() {
    alert('TODO');
  }

  focusp(e, el) {
    el.focus();
  }

}
