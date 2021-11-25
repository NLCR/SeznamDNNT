import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { User } from 'src/app/shared/user';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-zadost',
  templateUrl: './zadost.component.html',
  styleUrls: ['./zadost.component.scss']
})
export class ZadostComponent implements OnInit {

  subs: Subscription[] = [];
  zadost: Zadost;
  numFound: number;
  docs: SolrDocument[];
  hideProcessed: boolean;
  escalated: boolean = false;
  expired: boolean = true;
  

  kurators: User[];


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

    if (this.state.user.role == 'mainKurator') {
      this.service.getUsersByRole('kurator').subscribe(res => {
        this.kurators = res.docs;
      });
    }

    const id = this.route.snapshot.paramMap.get('id');

    this.subs.push(this.route.queryParams.subscribe(val => {
      this.service.getZadost(id).subscribe((resp: any) => {
        this.zadost = resp.response ?   resp.response.docs[0] : resp;
        this.getDocs(val);
      });
    }));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  getDocs(params: Params) {
    this.docs = [];
    const p = Object.assign({}, params);
    p.id = this.zadost.id;
    this.service.getZadostRecords(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      const process = this.zadost.process;
      this.numFound = resp.response.numFound;

      const notifications = resp.notifications;
      this.docs.map(doc => {
        doc.isProcessed = process && process[doc.identifier];
      });

      this.docs.forEach(doc=> {
        const docId = doc.identifier;
        const flag:boolean = notifications.some(n=> docId === n.identifier);
        if (flag) {
          doc.hasNotifications = true;
        }
      });

    });
  }

  setPriority(priority: string) {
      this.zadost.priority = priority;
      this.service.saveKuratorZadost(this.zadost).subscribe((res: any) => {
        if (res.error) {
          this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
        } else {
          this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
          this.getDocs(this.route.snapshot.queryParams);
        }
      });
  }

  setTypNavrhu(navrh: string) {
    this.zadost.navrh = navrh;
    this.service.saveZadost(this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.service.getZadost(this.zadost.id).subscribe((res: Zadost) => {
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
        });
      }
    },
    (error) => {
      this.service.showSnackBar('alert.ulozeni_zadosti_error', error.message, true);
    });
}


  setDelegated(delegated: string) {
    this.zadost.delegated = delegated;
    this.service.saveKuratorZadost(this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.getDocs(this.route.snapshot.queryParams);
      }
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
        this.service.showSnackBar('alert.odstraneni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.odstraneni_zadosti_success', '', false);
        this.getDocs(this.route.snapshot.queryParams);

        this.service.getZadost(this.zadost.id).subscribe((res:any) => {
          this.zadost = res;
        });

      }
    },
    (error) => {
          if (error.status == 409)  {
            this.service.showSnackBar('alert.ulozeni_zadosti_error', this.service.getTranslation('alert.users_conflict'), true);
          } else {
            this.service.showSnackBar('alert.ulozeni_zadosti_error', error.message, true);
          }
      }
    );
  }


  processNavrh(data: { type: string, identifier: string, komentar: string }) {
    switch (data.type) {
      case 'approve': 
       // approve navrh 

      //  this.service.approveNavrh(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
        this.service.approveItem(data.identifier, this.zadost, data.komentar, null).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });
        break;
      case 'approveLib': 
        this.service.approveItem(data.identifier, this.zadost, data.komentar, 'dnntt').subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });
        break;
      case 'releasedProved': 
        this.service.approveItem(data.identifier, this.zadost, data.komentar, 'title_released').subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });
        break;
      case 'reject': 
        this.service.rejectItem(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.zamitnuti_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.zamitnuti_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
            // this.processed = { date: new Date(), state: 'rejected', user: this.state.user.username };
          }
        });
        break;
    }



  }

}
