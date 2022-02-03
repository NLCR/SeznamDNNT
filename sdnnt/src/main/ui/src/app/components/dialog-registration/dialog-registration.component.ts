import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { User } from 'src/app/shared/user';
import { Router } from '@angular/router';
import { DialogRegistrationFormComponent } from '../dialog-registration-form/dialog-registration-form.component';
import { UserRegisterOption } from 'src/app/shared/user-register-option';
import { UserValidators } from 'src/app/shared/uservalidators';
import { error } from 'protractor';


@Component({
  selector: 'app-dialog-registration',
  templateUrl: './dialog-registration.component.html',
  styleUrls: ['./dialog-registration.component.scss']
})
export class DialogRegistrationComponent implements OnInit {

  user: User;
  focus: string;
  scroll: string;


  constructor(
    public dialogRef: MatDialogRef<DialogRegistrationComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    public state: AppState,
    private service: AppService,
    private router: Router) { }

  ngOnInit(): void {
    if (this.data.isRegister) {
      this.user = new User();
      this.user.registerOption = new UserRegisterOption();      
    } else {
      this.user = JSON.parse(JSON.stringify(this.state.user));
    }
    
  }


  //TODO: Change to validators
  save() {
    if (this.data.isRegister) {
      // basic fields validation


      let basicFieldsValidators:UserValidators = new UserValidators();
      let result =  basicFieldsValidators.basicFieldsValidation(this.user);
      if (result) {
        this.service.showSnackBar(result.errorTitle, result.errorMessag, true);
        this.focus = result.focus;
        return;
      }

      if (!this.user.registerOption?.condition1) {
        this.service.showSnackBar('alert.registrace_uzivatele_error', 'alert.condition1', true);
        this.focus = 'condition1';
        return;

      }
      if (!this.user.registerOption?.condition2) {
        this.service.showSnackBar('alert.registrace_uzivatele_error', 'alert.condition2', true);
        this.focus = 'condition2';
        return;

      }
      if (!this.user.registerOption?.condition3) {
        this.service.showSnackBar('alert.registrace_uzivatele_error', 'alert.condition3', true);
        this.focus = 'condition3';
        return;

      }


      this.user.username = this.user.username.trim();
      this.user.isActive = true;
      this.user.role = 'user';
      //this.user.type = ''

      this.service.registerUser(this.user).subscribe((res: User) => {
        if (res.error) {
          this.service.showSnackBar('alert.registrace_uzivatele_error', 'alert.'+res.error, true);
        } else {
          this.service.showSnackBar('alert.registrace_uzivatele_success', '', false);
          this.dialogRef.close();
        }
      });


    } else {

      let basicFieldsValidators:UserValidators = new UserValidators();
      let result =  basicFieldsValidators.basicFieldsValidation(this.user);
      if (result) {
        this.service.showSnackBar(result.errorTitle, result.errorMessag, true);
        this.focus = result.focus;
        return;
      }
      
      this.service.saveUser(this.user).subscribe((res: User) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_uzivatele_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_uzivatele_success', '', false);
        this.state.user = JSON.parse(JSON.stringify(this.user));
        this.dialogRef.close();
      }
    });
    }
    
  }
}
