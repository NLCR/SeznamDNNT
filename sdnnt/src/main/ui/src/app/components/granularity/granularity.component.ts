import { Component, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-granularity',
    templateUrl: './granularity.component.html',
    styleUrls: ['./granularity.component.scss'],
    imports: [MatDialogTitle, NgIf, FormsModule, CdkScrollable, MatDialogContent, NgFor, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class GranularityComponent {
  dialogRef = inject<MatDialogRef<GranularityComponent>>(MatDialogRef);
  data = inject(MAT_DIALOG_DATA);

  selection: any[] = [];
  isWhole: true;


  setWhole(e) {
  }

  setSelection(item) {
    console.log(item.selected);
  }

  change() {
    this.dialogRef.close({selection: this.data.items.filter(item => item.selected), isWhole: this.isWhole});
  }


}
