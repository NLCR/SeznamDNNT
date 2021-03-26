import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { HistoryItem } from 'src/app/shared/history-item';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-history-dialog',
  templateUrl: './history-dialog.component.html',
  styleUrls: ['./history-dialog.component.scss']
})
export class HistoryDialogComponent implements OnInit {

  displayedColumns: ['indextime','user','from','to','poznamka']
  history: HistoryItem[] = [];
  stavy: HistoryItem[] = [];

  constructor(
    public dialogRef: MatDialogRef<HistoryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    private service: AppService) { }

  ngOnInit(): void {
    this.service.getHistory(this.data.identifier).subscribe(res => {
      this.history = res.response.docs;
      this.stavy = this.history.filter(item => {
        return item.changes.backward_patch.find(p => p.path.indexOf('992') > 0);
      });
    });
  }

}
