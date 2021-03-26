import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { HistoryDialogComponent } from '../history-dialog/history-dialog.component';

@Component({
  selector: 'app-result-item',
  templateUrl: './result-item.component.html',
  styleUrls: ['./result-item.component.scss']
})
export class ResultItemComponent implements OnInit {

  @Input() doc: SolrDocument;

  states = new FormControl();

  constructor(
    public dialog: MatDialog,
    public state: AppState
  ) { }

  ngOnInit(): void {
  }

  showHistory() {
    const dialogRef = this.dialog.open(HistoryDialogComponent, {
      width: '550px',
      data: this.doc
    });

    // dialogRef.afterClosed().subscribe(result => {
    //   console.log('The dialog was closed', result);
    // });
  }

}
