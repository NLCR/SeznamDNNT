import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-granularity',
  templateUrl: './granularity.component.html',
  styleUrls: ['./granularity.component.scss']
})
export class GranularityComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<GranularityComponent>,
    @Inject(MAT_DIALOG_DATA) public data) { }

  ngOnInit(): void {
    console.log(this.data.items)
  }

}
