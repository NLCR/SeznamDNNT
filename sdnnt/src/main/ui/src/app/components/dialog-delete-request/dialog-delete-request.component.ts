import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

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
