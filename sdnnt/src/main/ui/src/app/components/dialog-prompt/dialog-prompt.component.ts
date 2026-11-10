import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogTitle } from '@angular/material/dialog';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-prompt',
    templateUrl: './dialog-prompt.component.html',
    styleUrls: ['./dialog-prompt.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatFormField, MatLabel, MatInput, FormsModule, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogPromptComponent {
  data = inject<{
    caption: string;
    label: string;
}>(MAT_DIALOG_DATA);


  result: string = null;


}
