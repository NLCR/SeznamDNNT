import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-dialog-forgotten-password',
  templateUrl: './dialog-forgotten-password.component.html',
  styleUrls: ['./dialog-forgotten-password.component.scss']
})
export class DialogForgottenPasswordComponent implements OnInit {

  user: string;

  constructor(
    public dialogRef: MatDialogRef<DialogForgottenPasswordComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private router: Router
  ) { }

  ngOnInit(): void {
  }

  resetPwd(): void {
    this.dialogRef.close();

    this.service.forgotPwd(this.user).subscribe((res:any) => {
      if (res.error) {
        this.service.showSnackBar('alert.forgot_password_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.forgot_password_success', '', false);
      }

      this.router.navigate(['/home'], {});
    });
  }

  close(): void {
    this.dialogRef.close();

  }

}
