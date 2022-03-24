import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-dialog-bulk-notifications',
  templateUrl: './dialog-bulk-notifications.component.html',
  styleUrls: ['./dialog-bulk-notifications.component.scss']
})
export class DialogBulkNotificationsComponent implements OnInit {

  periodicity:string = "den";
  name: string = "";

  notificationNameControl = new FormControl('', [Validators.required, Validators.nullValidator]);


  constructor(
    public dialogRef: MatDialogRef<DialogBulkNotificationsComponent>,
    public state: AppState,
    private service: AppService,
    private config: AppConfiguration) {
  }


  ngOnInit(): void {
    this.notificationNameControl.valueChanges.subscribe(selValue => {
      this.name = selValue;
    });
  }

  doBulkNotification() {
   let bulkNotif = {
      "query": this.state.q,
      "user":this.state.user.username,
      "name":this.name,
      "type":"rule",
      "periodicity":this.periodicity,
      "filters": this.filtersToJSON()
    };

    if (!this.state.q) {
      delete bulkNotif.query;
    }


    this.service.saveRuleNotification(
      bulkNotif
    ).subscribe((res: any) => {
        if (res.error) {
          this.service.showSnackBar('alert.sledovat_zaznam_error', res.error, true);
        } else {
          this.service.showSnackBar('alert.sledovat_zaznam_success', res.error, true);
          this.state.notificationSettings.all = res.all;
        }
        this.dialogRef.close();
      });

  }

  filtersToJSON() {

    let map:Map<string,string> = new Map();
    // console.log(this.state.usedFilters);
    this.state.usedFilters.forEach(f=> {
      let key = f.field;
      let val = f.value;
      map.set(key,val);
    });    
    let json = JSON.stringify(
      Array.from(
        map.entries()
      )
      .reduce((o, [key, value]) => { 
        o[key] = value; 
        return o; 
      }, {})
    );
    return json;
  }
}
