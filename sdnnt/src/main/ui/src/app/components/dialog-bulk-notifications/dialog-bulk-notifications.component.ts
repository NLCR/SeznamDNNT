import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-dialog-bulk-notifications',
  templateUrl: './dialog-bulk-notifications.component.html',
  styleUrls: ['./dialog-bulk-notifications.component.scss']
})
export class DialogBulkNotificationsComponent implements OnInit {
  notificationNameControl = new FormControl('', [Validators.required, Validators.nullValidator]);

  constructor(public state: AppState) { }

  ngOnInit(): void {
    
  }

  doBulkNotification() {
    // do bulk notification
  }
}
