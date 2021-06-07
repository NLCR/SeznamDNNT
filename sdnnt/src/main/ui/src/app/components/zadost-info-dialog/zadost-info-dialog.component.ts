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
  process: {[key: string]: string};
  imgSrc: string;

  constructor(
    public dialogRef: MatDialogRef<ZadostInfoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Zadost,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    this.service.getZadostRecords(this.data.id).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      this.action = this.data.navrh;
    });
    this.process = this.data.process ? JSON.parse(this.data.process) : {};
  }

  approve(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.data = res; 
        this.process = this.data.process ? JSON.parse(this.data.process) : {};
      }
    });
  }

  approveLib(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.data = res; 
        this.process = this.data.process ? JSON.parse(this.data.process) : {};
      }
    });
  }

  reject(doc: SolrDocument) {
    this.service.rejectNavrh(doc.identifier, this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('reject_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('reject_navrh_success', '', false);
        this.data = res;
        this.process = this.data.process ? JSON.parse(this.data.process) : {};
      }
    });
  }

}
