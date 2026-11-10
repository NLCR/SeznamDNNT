import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';

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
