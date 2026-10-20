import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-dialog-notifications-settings',
  templateUrl: './dialog-notifications-settings.component.html',
  styleUrls: ['./dialog-notifications-settings.component.scss']
})
export class DialogNotificationsSettingsComponent implements OnInit {

  notifications;
  //interval;

  usedNotificationFilter;

  constructor(
    public dialogRef: MatDialogRef<DialogNotificationsSettingsComponent>,
    public state: AppState,
    private service: AppService,
    private config: AppConfiguration,
    private router: Router,
    private route: ActivatedRoute

    ) {
  }

  ngOnInit(): void {
    if (this.state.user && this.state.user.administratorske_notifikace === undefined) {
      this.state.user.administratorske_notifikace = true;
    }

    this.service.getRuleNotifications().subscribe((res: any)=>{
      this.notifications = res.docs;
      this.notifications.forEach(notif => {
        const filters = [];
        if (notif.filters) {
          const parsed = JSON.parse(notif.filters);
          for (const k of Object.keys(parsed)) {
            const v = parsed[k];  // OK
            filters.push({
              field:k,
              value:v
            });
          }
        }
        notif.filters = filters;
      });

    });


    this.route.queryParams
      .subscribe(params => {
        this.usedNotificationFilter = params.notificationFilter;
      }
    );

  }

  administratorNotificationSettingsEnabled(): boolean {
    return this.state.user &&
      (this.state.user.role === 'admin' || this.state.user.role === 'kurator' || this.state.user.role === 'mainKurator');
  }

  deleteNotification(notification) {
    const index = this.notifications.indexOf(notification);
    if (index > -1) {
      this.notifications.splice(index, 1);
    }
  }


  saveNotificationSettings() {

    const savedNotications = [];
    this.notifications.forEach(notif=> {
        const notifFilters: {[k: string]: any} = {};
        notif.filters.forEach(filter=> {
          notifFilters[filter.field] = filter.value;
        });

        const nval = {
          name: notif.name,
          id: notif.id,
          user: notif.user,
          query: notif.query,
          type: notif.type,
          periodicity: notif.periodicity,
          filters: JSON.stringify(notifFilters)
        };
        savedNotications.push(nval);
    });

    const request =  {
      notification_interval:this.state.user.notifikace_interval,
      administratorske_notifikace:this.state.user.administratorske_notifikace,
      notifications:savedNotications
    };
    const user = this.state.user;

    this.service.savetRuleNotificationSettings(request).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_notifikaci_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_notifikaci_success', res.error, true);
        this.state.notificationSettings.all = res.all;
        this.router.navigate([], {
          queryParams: {
            notificationFilter: null
          },
          queryParamsHandling: 'merge'
        });
      }

      this.dialogRef.close();
    });

  }
}
