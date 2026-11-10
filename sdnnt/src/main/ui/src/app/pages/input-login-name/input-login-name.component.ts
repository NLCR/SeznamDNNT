import { Component, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogForgottenPasswordComponent } from 'src/app/components/dialog-forgotten-password/dialog-forgotten-password.component';

@Component({
    selector: 'app-input-login-name',
    templateUrl: './input-login-name.component.html',
    styleUrls: ['./input-login-name.component.scss']
})
export class InputLoginNameComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);
  dialog = inject(MatDialog);


  constructor() {

    const data = {};
    const dialogRef = this.dialog.open(DialogForgottenPasswordComponent, {
      width: '450px',
      data,
      panelClass: 'app-login-name-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.router.navigate(['/home'], {});
    });

   }


}
