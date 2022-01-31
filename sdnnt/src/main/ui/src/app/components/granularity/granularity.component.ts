import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-granularity',
  templateUrl: './granularity.component.html',
  styleUrls: ['./granularity.component.scss']
})
export class GranularityComponent implements OnInit {
  selection: any[] = [];
  isWhole: true;

  constructor(
    public dialogRef: MatDialogRef<GranularityComponent>,
    @Inject(MAT_DIALOG_DATA) public data) { }

  ngOnInit(): void {
    
  }

  setWhole(e) {
    console.log(e);
    this.isWhole = e;
  }

  setSelection(item) {
    console.log(item);
  }
  
  change() {
    this.dialogRef.close({selection: this.selection, change: true});
  }

}
