import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-dialog-prompt',
  templateUrl: './dialog-prompt.component.html',
  styleUrls: ['./dialog-prompt.component.scss']
})
export class DialogPromptComponent implements OnInit {

  result: string = null;

  constructor(@Inject(MAT_DIALOG_DATA) public data: {caption: string, label: string}) { }

  ngOnInit(): void {
  }

}
