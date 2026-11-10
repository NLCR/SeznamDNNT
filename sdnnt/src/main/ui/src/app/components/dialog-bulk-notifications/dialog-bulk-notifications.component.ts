import { Component, OnInit, inject } from '@angular/core';
import { FormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatFormField, MatLabel, MatError } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';

import { MatDivider } from '@angular/material/list';
import { MatRadioGroup, MatRadioButton } from '@angular/material/radio';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-bulk-notifications',
    templateUrl: './dialog-bulk-notifications.component.html',
    styleUrls: ['./dialog-bulk-notifications.component.scss'],
    imports: [
    MatDialogTitle,
    CdkScrollable,
    MatDialogContent,
    FlexModule,
    MatFormField,
    MatLabel,
    MatInput,
    FormsModule,
    ReactiveFormsModule,
    MatError,
    MatDivider,
    MatRadioGroup,
    MatRadioButton,
    MatDialogActions,
    MatButton,
    MatDialogClose,
    TranslateModule
]
})
export class DialogBulkNotificationsComponent implements OnInit {
  dialogRef = inject<MatDialogRef<DialogBulkNotificationsComponent>>(MatDialogRef);
  state = inject(AppState);
  private service = inject(AppService);
  private config = inject(AppConfiguration);


  periodicity = 'den';
  name = '';

  notificationNameControl = new FormControl<string>('', { nonNullable: true, validators: [Validators.required, Validators.nullValidator] });


  ngOnInit(): void {
    this.notificationNameControl.valueChanges.subscribe(selValue => {
      this.name = selValue;
    });
  }

  doBulkNotification() {
   const bulkNotif = {
      query: this.state.q,
      user:this.state.user.username,
      name:this.name,
      type:'rule',
      periodicity:this.periodicity,
      filters: this.filtersToJSON()
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

    const map: Map<string,string> = new Map();
    // console.log(this.state.usedFilters);
    this.state.usedFilters.forEach(f=> {
      const key = f.field;
      const val = f.value;
      map.set(key,val);
    });
    const json = JSON.stringify(
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
