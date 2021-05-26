import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-zadost-info-dialog',
  templateUrl: './zadost-info-dialog.component.html',
  styleUrls: ['./zadost-info-dialog.component.scss']
})
export class ZadostInfoDialogComponent implements OnInit {

  docs: SolrDocument[];
  action: string;

  constructor(
    public dialogRef: MatDialogRef<ZadostInfoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Zadost,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    this.service.getZadost(this.data.identifiers).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      this.action = this.data.new_stav;
    });
  }

  approve(doc: SolrDocument) {

  }

}
