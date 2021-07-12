import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-prompt-dialog',
  templateUrl: './prompt-dialog.component.html',
  styleUrls: ['./prompt-dialog.component.scss']
})
export class PromptDialogComponent implements OnInit {

  result: string;

  constructor(@Inject(MAT_DIALOG_DATA) public data: {caption: string, label: string}) { }

  ngOnInit(): void {
  }

}
