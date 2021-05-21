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
    this.user = Object.assign(new User(), this.state.user);
  }

  save() {
    this.service.saveUser(this.user).subscribe((res: User) => {
      if (res.error) {
        this.service.showSnackBar('user_saving_error', res.error, true);
      } else {
        this.state.user = Object.assign(new User(), this.user);
        this.dialogRef.close();
      }
    });
  }

}
