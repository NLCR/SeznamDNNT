import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-states-dialog',
  templateUrl: './states-dialog.component.html',
  styleUrls: ['./states-dialog.component.scss']
})
export class StatesDialogComponent implements OnInit {

  newState;

  constructor(
    public dialogRef: MatDialogRef<StatesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    this.newState = this.data.marc_990a;
  }

}
