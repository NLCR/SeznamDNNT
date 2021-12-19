import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-dialog-correspondence',
  templateUrl: './dialog-correspondence.component.html',
  styleUrls: ['./dialog-correspondence.component.scss']
})
export class DialogCorrespondenceComponent implements OnInit {

  result: string;


  constructor(
    public dialogRef: MatDialogRef<DialogCorrespondenceComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Zadost) { 
    
      this.result = data.email;
  }

  ngOnInit(): void {
  }

  saveCorrespondence(): void {
    this.dialogRef.close({result: this.result});
  }
}
