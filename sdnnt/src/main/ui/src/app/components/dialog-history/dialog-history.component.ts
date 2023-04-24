import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { HistoryItem } from 'src/app/shared/history-item';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-dialog-history',
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss']
})
export class DialogHistoryComponent implements OnInit {

  history: HistoryItem[] = [];
  stavy: HistoryItem[] = [];
  kuratorskestavy: HistoryItem[] = [];
  granulaovaneStavy: HistoryItem[] = [];
  granularity: boolean = false;


  constructor(
    public dialogRef: MatDialogRef<DialogHistoryComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    this.stavy = this.data.historie_stavu;


    this.stavy = this.data.historie_stavu;
    this.stavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4)),
          m = parseInt(d.substr(4, 2)) - 1,
          day = parseInt(d.substr(6, 2));
        h.date = new Date(y, m, day);

      }
    });

    this.kuratorskestavy = this.data.historie_kurator_stavu;
    this.kuratorskestavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4)),
          m = parseInt(d.substr(4, 2)) - 1,
          day = parseInt(d.substr(6, 2));
        h.date = new Date(y, m, day);
      }
    });

    this.granularity = (this.data.fmt === 'SE' || this.data.fmt === 'BK') && this.data.granularity;

    this.granulaovaneStavy = this.data.historie_granulovaneho_stavu;
    this.granulaovaneStavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4)),
          m = parseInt(d.substr(4, 2)) - 1,
          day = parseInt(d.substr(6, 2));
        h.date = new Date(y, m, day);
      }
    });
    console.log(this.granulaovaneStavy);
  }
}
