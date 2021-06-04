import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { User } from 'src/app/shared/user';

@Component({
  selector: 'app-user-dialog',
  templateUrl: './user-dialog.component.html',
  styleUrls: ['./user-dialog.component.scss']
})
export class UserDialogComponent implements OnInit {

  user: User;

  constructor(
    public dialogRef: MatDialogRef<UserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    if (this.data.isRegister) {
      this.user = new User();
    } else {
      this.user = JSON.parse(JSON.stringify(this.state.user));
    }
    
  }

  resetPwd() {
    this.service.resetPwd(null).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('resetPwd_error', res.error, true);
      } else {
        this.service.showSnackBar('resetPwd_uspesna', '', false);
        this.dialogRef.close();
      }
    });
  }

  save() {
    if (this.data.isRegister) {
      this.user.isActive = false;
      this.user.role = 'user';
      
      this.service.registerUser(this.user).subscribe((res: User) => {
        if (res.error) {
          this.service.showSnackBar('user_register_error', res.error, true);
        } else {
          this.service.showSnackBar('registrace_uspesna', '', false);
          this.dialogRef.close();
        }
      });
    } else {
      this.service.saveUser(this.user).subscribe((res: User) => {
      if (res.error) {
        this.service.showSnackBar('user_saving_error', res.error, true);
      } else {
        this.service.showSnackBar('ulozeni_uspesne', '', false);
        JSON.parse(JSON.stringify(this.user));
        this.dialogRef.close();
      }
    });
    }
    
  }

}
