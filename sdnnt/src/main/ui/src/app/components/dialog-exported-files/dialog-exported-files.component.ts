import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { CdkScrollable } from '@angular/cdk/scrolling';

import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

export interface ExportedFilesData {
  exportname: string;
  files: {
    name: string;
    path: string;
  }[];
}

@Component({
    selector: 'app-dialog-exported-files',
    templateUrl: './dialog-exported-files.component.html',
    styleUrls: ['./dialog-exported-files.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogExportedFilesComponent {
  data = inject<ExportedFilesData>(MAT_DIALOG_DATA);


  public files: any = [
  ];


  encodePath(path) {
    return encodeURIComponent(path);
  }

}
