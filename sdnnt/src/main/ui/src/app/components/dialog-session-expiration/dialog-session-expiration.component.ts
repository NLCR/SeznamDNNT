import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { interval } from 'rxjs';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

import { FlexModule } from '@angular/flex-layout/flex';
import { MatButton } from '@angular/material/button';
import { MatLabel } from '@angular/material/form-field';
import { TranslateModule } from '@ngx-translate/core';


@Component({
    selector: 'app-dialog-session-expiration',
    templateUrl: './dialog-session-expiration.component.html',
    styleUrls: ['./dialog-session-expiration.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, FlexModule, MatLabel, MatDialogActions, MatButton, TranslateModule]
})
export class DialogSessionExpirationComponent implements OnInit {
  dialogRef = inject<MatDialogRef<DialogSessionExpirationComponent>>(MatDialogRef);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  private service = inject(AppService);
  private router = inject(Router);
  data = inject(MAT_DIALOG_DATA);


   timer = 0;
   counter = 20;
   //playing = new BehaviorSubject(false);


  ngOnInit(): void {

    const int =  interval(1000);
    int.subscribe(x=>{
      this.timer += 1;
      this.counter = Math.max(this.data.remainingtime - this.timer, 0);
    });
  }


  keepAlive() {
    this.state.expirationDialog = false;
    this.service.pong().subscribe(()=> {
      this.dialogRef.close();
    });
  }

  close() {
    this.state.expirationDialog = false;

    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;

      this.state.facetsstore.reinit();

      this.state.sort.sort = this.config.sorts.sort[0];
      this.state.sort.sort_account = this.config.sorts.sort_account[0];
      this.state.sort.user_sort_account = this.config.sorts.user_sort_account[0];

      localStorage.removeItem('user');
      this.dialogRef.close();
      //this.state.stopTrackSession(this.bnIdle);
    });

  }
}
