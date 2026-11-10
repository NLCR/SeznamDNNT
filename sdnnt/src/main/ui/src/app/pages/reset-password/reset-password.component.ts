import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogChangePasswordComponent } from 'src/app/components/dialog-change-password/dialog-change-password.component';


enum Tokenvalidation {
  INVALID, VALID, NOT_FETCHED
}


@Component({
    selector: 'app-reset-password',
    templateUrl: './reset-password.component.html',
    styleUrls: ['./reset-password.component.scss'],
    imports: []
})
export class ResetPasswordComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);
  dialog = inject(MatDialog);


  tokenValidation: Tokenvalidation = Tokenvalidation.NOT_FETCHED;
  content: string;

  ngOnInit(): void {
    this.route.paramMap.subscribe(val => {
      if (val.has('token')) {
        const token = val.get('token');
        this.service.validateToken(token).subscribe((res: any) => {
          if(res.valid) {
            this.tokenValidation = Tokenvalidation.VALID;
            const data = {token};
            const dialogRef = this.dialog.open(DialogChangePasswordComponent, {
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




}
