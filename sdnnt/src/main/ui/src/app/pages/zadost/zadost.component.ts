import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-zadost',
  templateUrl: './zadost.component.html',
  styleUrls: ['./zadost.component.scss']
})
export class ZadostComponent implements OnInit {

  zadost: Zadost;
  numFound: number;
  docs: SolrDocument[];
  // action: string;
  hideProcessed: boolean;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    if (!this.state.user) {
      this.router.navigate(['/']);
      return;
    }
    const id = this.route.snapshot.paramMap.get('id');

    this.service.getZadost(id).subscribe((resp: any) => {
      this.zadost = resp.response.docs[0];
      // this.action = this.zadost.new_stav;
      this.getDocs();
    });

  }

  getDocs() {

    this.service.getZadostRecords(this.zadost.id).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      const process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      this.numFound = resp.response.numFound;
      this.docs.map(doc => {
        doc.isProcessed = process && process[doc.identifier];
      });
      // this.action = this.zadost.new_stav;
    });
  }

  approve(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.zadost = res; 
        // this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

  approveLib(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.zadost = res; 
        // this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

  reject(doc: SolrDocument) {
    this.service.rejectNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('reject_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('reject_navrh_success', '', false);
        this.zadost = res;
        // this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

  process() {
    this.service.processZadost(this.zadost).subscribe(res => {

    });
  }

}
