import { Component, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { CdkScrollable } from '@angular/cdk/scrolling';

import { MatCard } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-expression-dialog',
    templateUrl: './expression-dialog.component.html',
    styleUrls: ['./expression-dialog.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatCard, MatIcon, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class ExpressionDialogComponent {
  dialogRef = inject<MatDialogRef<ExpressionDialogComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);


}
