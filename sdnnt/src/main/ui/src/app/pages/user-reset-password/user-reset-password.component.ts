import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Subscription } from 'rxjs';
import { MatPasswordStrengthComponent } from '@angular-material-extensions/password-strength';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { UserPswDialogComponent } from 'src/app/components/user-pswdialog/user-pswdialog.component';



@Component({
  selector: 'app-user-reset-password',
  templateUrl: './user-reset-password.component.html',
  styleUrls: ['./user-reset-password.component.scss']
})
export class UserResetPasswordComponent implements OnInit {



  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog 

  ) { }



  ngOnInit(): void {

    //*ngIf="state.user && state.user.role === 'admin'"
    console.log("State user "+this.state.user);

    if (this.state.user != null) {
      console.log("State user "+this.state.user);
      const data = {};
      const dialogRef = this.dialog.open(UserPswDialogComponent, {
        width: '800px',
        data,
        panelClass: 'app-data-dialog'
      });
    }

  }

  



}