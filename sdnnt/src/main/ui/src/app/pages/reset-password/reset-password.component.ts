import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Subscription } from 'rxjs';
import { MatPasswordStrengthComponent } from '@angular-material-extensions/password-strength';
import { Title } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { UserPswDialogComponent } from 'src/app/components/user-pswdialog/user-pswdialog.component';

enum Tokenvalidation {
  INVALID, VALID, NOT_FETCHED
}


@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss']
})
export class ResetPasswordComponent implements OnInit {



  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog 

  ) { }

  tokenValidation: Tokenvalidation = Tokenvalidation.NOT_FETCHED;
  content: string;

  ngOnInit(): void {
    this.route.paramMap.subscribe(val => {
      if (val.has("token")) {
        const token = val.get("token");
        this.service.validateToken(token).subscribe((res:any) => {
          if(res.valid) {
            this.tokenValidation = Tokenvalidation.VALID;
            const data = {"token":token};
            const dialogRef = this.dialog.open(UserPswDialogComponent, {
              width: '450px',
              data,
              panelClass: 'app-reset-password-dialog'
            });
          } else {
            this.tokenValidation = Tokenvalidation.INVALID;
            this.service.getText('invalid_token').subscribe(text => this.content = text);
          }
        });

      } 
    });
  }

  
  invalidToken(): boolean {
    return this.tokenValidation === Tokenvalidation.INVALID;
  }



  // getContent() {
  //   this.service.getText('invalid_token').subscribe(text => this.content = text);
  // }

  // onStrengthChanged(strength: number) {
  //   console.log('password strength = ', strength);
  // }

}
