import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
    selector: 'app-dialog-forgotten-password',
    templateUrl: './dialog-forgotten-password.component.html',
    styleUrls: ['./dialog-forgotten-password.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatFormField, MatLabel, MatInput, FormsModule, MatDialogActions, MatButton, TranslateModule]
})
export class DialogForgottenPasswordComponent {
  dialogRef = inject<MatDialogRef<DialogForgottenPasswordComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  private service = inject(AppService);
  private router = inject(Router);


  user: string;


  resetPwd(): void {
    this.dialogRef.close();

    this.service.forgotPwd(this.user).subscribe((res: any) => {
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
