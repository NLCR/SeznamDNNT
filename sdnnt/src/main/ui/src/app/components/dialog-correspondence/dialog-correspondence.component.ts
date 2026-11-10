import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { TranslateModule } from '@ngx-translate/core';
import { Zadost } from 'src/app/shared/zadost';

@Component({
    selector: 'app-dialog-correspondence',
    templateUrl: './dialog-correspondence.component.html',
    styleUrls: ['./dialog-correspondence.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatFormField, MatLabel, MatInput, FormsModule, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogCorrespondenceComponent {
  dialogRef = inject<MatDialogRef<DialogCorrespondenceComponent>>(MatDialogRef);
  data = inject<Zadost>(MAT_DIALOG_DATA);


  result: string;


  constructor() {
      const data = this.data;


      this.result = data.email;
  }


  saveCorrespondence(): void {
    this.dialogRef.close({result: this.result});
  }
}
