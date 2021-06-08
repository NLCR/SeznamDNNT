import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-reject-dialog',
  templateUrl: './reject-dialog.component.html',
  styleUrls: ['./reject-dialog.component.scss']
})
export class RejectDialogComponent implements OnInit {

  reason: string;

  constructor() { }

  ngOnInit(): void {
  }

}
