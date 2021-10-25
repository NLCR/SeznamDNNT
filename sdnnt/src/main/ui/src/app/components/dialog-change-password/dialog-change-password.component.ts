import { MatPasswordStrengthComponent } from '@angular-material-extensions/password-strength';
import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { analyzeAndValidateNgModules } from '@angular/compiler';


@Component({
  selector: 'dialog-change-password',
  templateUrl: './dialog-change-password.component.html',
  styleUrls: ['./dialog-change-password.component.scss']
})
export class DialogChangePasswordComponent implements OnInit {

  
  @ViewChild('passwordComponentWithConfirmation', {static: true})
  passwordComponentWithConfirmation: MatPasswordStrengthComponent;

  constructor(
    public dialogRef: MatDialogRef<DialogChangePasswordComponent>,
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    @Inject(MAT_DIALOG_DATA) public data: any) { 
      if (data.token) {
        this.token = data.token;
      }
    }

  token:string;

  ngOnInit(): void {}

  onStrengthChanged(st: number, form: any) {}

  close(): void {
    this.router.navigate(['/home'], {});
  }

  changePswd(pswdComponent: any): void {
      //console.log("New password "+pswdComponent.value);

      if (this.token != null) {
        this.service.changePasswordByToken(this.token, pswdComponent.value).subscribe((res:any) => {
          if (res.error) {
            this.service.showSnackBar('desc.password_changed_error', res.error, true);
            this.router.navigate(['/home'], {});
          } else {
            this.service.showSnackBar('desc.password_changed', '', false);
            this.router.navigate(['/home'], {});
          }
        });
      } else {
        this.service.changePasswordByUser( pswdComponent.value).subscribe((res:any) => {
          if (res.error) {
            this.service.showSnackBar('desc.password_changed_error', res.error, true);
            this.router.navigate(['/home'], {});
          } else {
            this.service.showSnackBar('desc.password_changed', '', false);
            this.router.navigate(['/home'], {});
          }

      });
    }
  }

} 
