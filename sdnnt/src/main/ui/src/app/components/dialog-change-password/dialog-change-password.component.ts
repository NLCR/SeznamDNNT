import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { FormControl, Validators } from '@angular/forms';


@Component({
  standalone: false,
  selector: 'app-dialog-change-password',
  templateUrl: './dialog-change-password.component.html',
  styleUrls: ['./dialog-change-password.component.scss']
})
export class DialogChangePasswordComponent {

  readonly maxPasswordLength = 30;
  readonly passwordCriteria = [
    { label: 'reset_password.contains at_least_8_characters', test: (value: string) => value.length >= 8 && value.length <= this.maxPasswordLength },
    { label: 'reset_password.contains_at_least_one_lower_character', test: (value: string) => /[a-z]/.test(value) },
    { label: 'reset_password.contains_at_least_one_upper_character', test: (value: string) => /[A-Z]/.test(value) },
    { label: 'reset_password.contains_at_least_one_digit_character', test: (value: string) => /[0-9]/.test(value) },
    { label: 'reset_password.contains_at_least_one_special_character', test: (value: string) => /[\x20-\x2F\x3A-\x40\x5B-\x60\x7B-\x7E]/.test(value) }
  ];
  readonly passwordControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, control =>
      this.passwordCriteria.every(criterion => criterion.test(control.value)) ? null : {pattern: true}]
  });
  readonly confirmationControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, control =>
      control.value === this.passwordControl.value ? null : {notConfirmed: true}]
  });
  showPassword = false;
  showConfirmation = false;

  get strength(): number {
    return this.passwordCriteria.filter(criterion => criterion.test(this.passwordControl.value)).length * 20;
  }

  get strengthColor(): 'warn' | 'accent' | 'primary' {
    return this.strength < 21 ? 'warn' : this.strength < 81 ? 'accent' : 'primary';
  }

  constructor(
    public dialogRef: MatDialogRef<DialogChangePasswordComponent>,
    private router: Router,
    private service: AppService,
    public state: AppState,
    @Inject(MAT_DIALOG_DATA) public data: any) {
      if (data.token) {
        this.token = data.token;
      }
      this.passwordControl.valueChanges.subscribe(() => this.confirmationControl.updateValueAndValidity({emitEvent: false}));
    }

  token: string;

  close(): void {
    this.router.navigate(['/home'], {});
  }

  changePswd(): void {
      if (this.passwordControl.invalid || this.confirmationControl.invalid) {
        return;
      }
      const password = this.passwordControl.value;

      if (this.token != null) {
        this.service.changePasswordByToken(this.token, password).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('desc.password_changed_error', res.error, true);
            this.router.navigate(['/home'], {});
          } else {
            this.service.showSnackBar('desc.password_changed', '', false);
            this.router.navigate(['/home'], {});
          }
        });
      } else {
        this.service.changePasswordByUser(password).subscribe((res: any) => {
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
