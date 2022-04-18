import { Component, Inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { BehaviorSubject, interval, Subscription } from 'rxjs';


@Component({
  selector: 'app-dialog-session-expiration',
  templateUrl: './dialog-session-expiration.component.html',
  styleUrls: ['./dialog-session-expiration.component.scss']
})
export class DialogSessionExpirationComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<DialogSessionExpirationComponent>,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService,
    private router: Router,
    @Inject(MAT_DIALOG_DATA) public data: any,

  ) { }

   timer:number = 0;
   counter: number = 20;
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

      this.state.sort['sort'] = this.config.sorts.sort[0];
      this.state.sort['sort_account'] = this.config.sorts.sort_account[0];
      this.state.sort['user_sort_account'] = this.config.sorts.user_sort_account[0];

      localStorage.removeItem('user');
      this.dialogRef.close();
      //this.state.stopTrackSession(this.bnIdle);
    });

  }
}
