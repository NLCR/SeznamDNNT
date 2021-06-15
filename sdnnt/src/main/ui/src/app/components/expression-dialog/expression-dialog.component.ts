import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-expression-dialog',
  templateUrl: './expression-dialog.component.html',
  styleUrls: ['./expression-dialog.component.scss']
})
export class ExpressionDialogComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<ExpressionDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) { }

  ngOnInit(): void {
  }

}
