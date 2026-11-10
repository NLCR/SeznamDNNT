import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';

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
