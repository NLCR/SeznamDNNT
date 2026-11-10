import { Component, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { CdkScrollable } from '@angular/cdk/scrolling';

import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

export interface DataDialogData {
  title: string;
  items: {
    label: string;
    value: any;
  }[];
}

@Component({
    selector: 'app-dialog-identifier',
    templateUrl: './dialog-identifier.component.html',
    styleUrls: ['./dialog-identifier.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogIdentifierComponent {
  dialogRef = inject<MatDialogRef<DialogIdentifierComponent>>(MatDialogRef);
  data = inject<DataDialogData>(MAT_DIALOG_DATA);


}
