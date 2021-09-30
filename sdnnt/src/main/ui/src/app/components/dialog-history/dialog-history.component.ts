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
  selector: 'app-dialog-history',
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss']
})
export class DialogHistoryComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<DialogHistoryComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DataDialogData) { }

  ngOnInit(): void {
  }

}
