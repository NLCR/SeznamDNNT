import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

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
  styleUrls: ['./dialog-identifier.component.scss']
})
export class DialogIdentifierComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<DialogIdentifierComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DataDialogData) { }

  ngOnInit(): void {
  }

}
