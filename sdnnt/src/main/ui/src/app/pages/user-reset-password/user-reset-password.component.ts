import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogChangePasswordComponent } from 'src/app/components/dialog-change-password/dialog-change-password.component';



@Component({
  selector: 'app-user-reset-password',
  templateUrl: './user-reset-password.component.html',
  styleUrls: ['./user-reset-password.component.scss']
})
export class UserResetPasswordComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);
  dialog = inject(MatDialog);




  ngOnInit(): void {

    //*ngIf="state.user && state.user.role === 'admin'"
    console.log('State user '+this.state.user);

    if (this.state.user != null) {
      console.log('State user '+this.state.user);
      const data = {};
      const dialogRef = this.dialog.open(DialogChangePasswordComponent, {
        width: '450px',
        data,
        panelClass: 'app-reset-password-dialog'
      });
    }

  }





}
