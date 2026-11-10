import { Component, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-send-request',
    templateUrl: './dialog-send-request.component.html',
    styleUrls: ['./dialog-send-request.component.scss'],
    imports: [MatDialogTitle, MatFormField, MatLabel, MatInput, FormsModule, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogSendRequestComponent {
  dialogRef = inject<MatDialogRef<DialogSendRequestComponent>>(MatDialogRef);
  data = inject<Zadost>(MAT_DIALOG_DATA);
  private service = inject(AppService);


  newState: string;
  poznamka: string;
  pozadavek: string;


  send() {

    this.data.pozadavek = this.pozadavek;
    this.data.poznamka = this.poznamka;

    this.service.sendZadost(this.data).subscribe((res: any) => {
      if (res.error) {
        if (res.key) {
          this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
        } else {
          this.service.showSnackBar(res.key, '', true);

        }
      } else {
        this.data = res as Zadost;
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.dialogRef.close();
      }
    });


    /*
    this.data.datum_zadani = new Date();
    this.data.pozadavek = this.pozadavek;
    this.data.poznamka = this.poznamka;
    // sluzba
    this.data.state = 'waiting';
    this.service.saveZadost(this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.dialogRef.close();
      }
    });
    */

  }

}
