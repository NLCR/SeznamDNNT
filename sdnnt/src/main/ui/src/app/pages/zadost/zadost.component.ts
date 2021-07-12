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
      this.getDocs();
    });

  }

  getDocs() {

    this.service.getZadostRecords(this.zadost.id).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      const process = this.zadost.process;
      this.numFound = resp.response.numFound;
      this.docs.map(doc => {
        doc.isProcessed = process && process[doc.identifier];
      });
    });
  }

  process() {

    if (this.zadost.identifiers.length > Object.keys(this.zadost.process).length) {
      this.service.showSnackBar('alert.oznaceni_jako_zpracovane_error', 'desc.ne_vsechny_zaznamy_jsou_zpracovane', true);
      return;
    }
    this.service.processZadost(this.zadost).subscribe(res => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.zadost.state = 'processed';
      }
    });
  }

  removeDoc(identifier: string) {
    this.zadost.identifiers = this.zadost.identifiers.filter(id => id !== identifier);
    this.service.saveZadost(this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.getDocs();
      }
    });
  }


  processNavrh(data: { type: string, identifier: string, komentar: string }) {
    switch (data.type) {
      case 'approve': 
        this.service.approveNavrh(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs();
            // this.processed = { date: new Date(), state: 'approved', user: this.state.user.username };
          }
        });
        break;
      case 'approveLib': 
        this.service.approveNavrhLib(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs();
          }
        });
        break;
      case 'reject': 
        this.service.rejectNavrh(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.zamitnuti_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.zamitnuti_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs();
            // this.processed = { date: new Date(), state: 'rejected', user: this.state.user.username };
          }
        });
        break;
    }



  }

}
