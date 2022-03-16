import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-dialog-notifications-settings',
  templateUrl: './dialog-notifications-settings.component.html',
  styleUrls: ['./dialog-notifications-settings.component.scss']
})
export class DialogNotificationsSettingsComponent implements OnInit {

  constructor(public state: AppState) { }

  ngOnInit(): void {
  }

  saveNotificationSettings() {
    // to do
  }

}
