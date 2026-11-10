import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';

@Component({
    selector: 'app-dialog-delete-request',
    templateUrl: './dialog-delete-request.component.html',
    styleUrls: ['./dialog-delete-request.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatCard, MatCardContent, MatIcon, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogDeleteRequestComponent {
  private router = inject(Router);
  private service = inject(AppService);
  data = inject<Zadost>(MAT_DIALOG_DATA);


  deleteRequest() {
    this.service.deleteZadost(this.data).subscribe(res => {
      if (res.error) {
        this.service.showSnackBar('alert.smazani_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.smazani_zadosti_success', '', false);
      }
    });
  }

}
