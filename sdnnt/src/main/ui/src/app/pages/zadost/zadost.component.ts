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
import { MatDialog } from '@angular/material/dialog';
import { DialogCorrespondenceComponent } from 'src/app/components/dialog-correspondence/dialog-correspondence.component';
import { DialogPromptComponent } from 'src/app/components/dialog-prompt/dialog-prompt.component';

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
  
  invalidrecs:string[];

  kurators: User[];


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog) { }

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


  linkToAccount() {
    const p: any = {};
    if (this.state.user?.role === 'kurator' || this.state.user?.role === 'mainKurator') {
      p.sort_account = this.state.sort.sort_account.field + " " + this.state.sort.sort_account.dir;
    } else {
      p.user_sort_account = this.state.sort.user_sort_account.field + " " + this.state.sort.user_sort_account.dir;
    }

    if (this.state.navigationstore.contains('account')) {
      p.page = this.state.navigationstore.getPage('account')
      p.rows= this.state.navigationstore.getRows('account');
    }

    let navrh = this.state.facetsstore.getFacet('account','newStavFilter');
    let state = this.state.facetsstore.getFacet('account','stateFilter');
    let institution = this.state.facetsstore.getFacet('account','institutionFilter');
    let delegated = this.state.facetsstore.getFacet('account','delegatedFilter');
    let type_of_request = this.state.facetsstore.getFacet('account','typeOfRequestFilter');
    let priority = this.state.facetsstore.getFacet('account','priorityFilter');

    if (navrh) p.navrh = navrh;
    if (state) p.state = state;
    if (institution) p.institution = institution;
    if (delegated) p.delegated = delegated;
    if (type_of_request) p.type_of_request = type_of_request;
    if (priority) p.priority = priority;

    if (this.state.prefixsearch['account']?.length) {
      p.prefix = this.state.prefixsearch['account'];
    }

    this.router.navigate(['/account'], { queryParams: p, queryParamsHandling: 'merge' });
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  getDocs(params: Params) {
    this.docs = [];
    const p = Object.assign({}, params);
    p.id = this.zadost.id;
    if (this.state.isNavigationStoreKeyInitialized(this.zadost.id)) {
      p.page = this.state.navigationstore.getPage(this.zadost.id);
      p.rows = this.state.navigationstore.getRows(this.zadost.id);
    }

    this.service.getZadostRecords(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.docs = resp.response.docs;
      const process = this.zadost.process;
      this.numFound = resp.response.numFound;
      
      if (!this.state.isNavigationStoreKeyInitialized(this.zadost.id)) {
        this.state.initDefaultNavitionStoreKey(this.zadost.id);
      }



      const notifications = resp.notifications;
      // TODO: Should not be dependent on type 
      if (this.zadost.navrh=='VNL') {

        let stateKey = (this.zadost.desired_item_state ? this.zadost.desired_item_state : "_");
        let licenseKey = (this.zadost.desired_license ? this.zadost.desired_license : "_");
        let allProcessed: boolean = true;
        if (this.zadost && this.zadost.process) {
          this.docs.forEach(doc=> {
            let id = doc.identifier;
            let tablekey = id + "_(" + stateKey + "," + licenseKey + ")";
            let noworkflowkey =id+"_noworkflow";
            doc.isProcessed = this.zadost.process[tablekey] || this.zadost.process[noworkflowkey];
          });
        }
      } else {
        this.docs.map(doc => {
          doc.isProcessed = process && process[doc.identifier];
        });
      }

      this.docs.forEach(doc=> {
        const docId = doc.identifier;
        const flag:boolean = notifications.some(n=> docId === n.identifier);
        if (flag) {
          doc.hasNotifications = true;
        }
      });
  
      this.service.getZadostInvalidRecords(p as HttpParams).subscribe((res:any)=> {
        if (res.error) {
        } else {
          this.invalidrecs =  res["missing"];
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
          this.zadost = res;
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

    let allP: boolean = this.allProcessed();

    if (!allP) {
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

  

  allProcessed(): boolean {
    
    let stateKey = (this.zadost.desired_item_state ? this.zadost.desired_item_state : "_");
    let licenseKey = (this.zadost.desired_license ? this.zadost.desired_license : "_");
    let allProcessed: boolean = true;
    if (this.zadost && this.zadost.process) {
      this.zadost.identifiers?.forEach(id => {
        let tablekey = id + "_(" + stateKey + "," + licenseKey + ")";
        let noworkflowkey =id+"_noworkflow";
        if (!this.zadost.process[tablekey] && !this.zadost.process[noworkflowkey]) {
          if (this.invalidrecs) {
            if (!this.invalidrecs.includes(id)) {
              allProcessed = false;
            }
          } else {
            allProcessed = false;
          }
        }

      });
    } else {
      allProcessed = false;
    }
    return allProcessed;
  }

  removeDoc(identifier: string) {
    this.zadost.identifiers = this.zadost.identifiers?.filter(id => id !== identifier);
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


  processNavrh(data: { type: string, identifier: string, komentar: string, options:string }) {
    if (!data.komentar && data.type != 'reject') {
      this.service.showSnackBar('alert.ulozeni_zadosti_error', 'alert.komentar_chybi', true);
      return;
    } else if (!data.komentar && data.type === 'reject') {
      this.service.showSnackBar('alert.ulozeni_zadosti_error', 'alert.doduvodneni_chybi', true);
      return;
    }
    switch (data.type) {
      case 'approve': 
       // approve navrh 

      //  this.service.approveNavrh(data.identifier, this.zadost, data.komentar).subscribe((res: any) => {
        this.service.approveItem(data.identifier, this.zadost, data.komentar, null, data.options).subscribe((res: any) => {
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
        this.service.approveItem(data.identifier, this.zadost, data.komentar, 'dnntt', data.options).subscribe((res: any) => {
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
        this.service.approveItem(data.identifier, this.zadost, data.komentar, 'title_released', data.options).subscribe((res: any) => {
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

  isItemProcessed(id:string): boolean {
    let stateKey = (this.zadost.desired_item_state ?  this.zadost.desired_item_state : "_");
    let licenseKey = (this.zadost.desired_license ?  this.zadost.desired_license : "_");
    let tablekey = id +"_("+stateKey+","+licenseKey+")";
    if (this.zadost.process) return this.zadost.process[tablekey];
    else return false;
  }


  aproveAll() {
    const approveDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: {caption: 'komentar', label: 'komentar'},
      panelClass: 'app-register-dialog'
    });
    approveDialogRef.afterClosed().subscribe(result => {
      if (result) {
        let items = this.zadost.identifiers.filter(it => !this.isItemProcessed(it));
        
        this.service.approveItemsBatch(items, this.zadost,result, null, 'all').subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
            if (res.payload) {
              this.zadost = res.payload;
              this.getDocs(this.route.snapshot.queryParams);
            }
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });

        /*
        this.service.approveItems(items, this.zadost,result, null, 'all').subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
            if (res.payload) {
              this.zadost = res.payload;
              this.getDocs(this.route.snapshot.queryParams);
            }
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });
        */
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', 'alert.komentar_chybi', true);
      }
    });
  }


  rejectAll() {

    const rejectDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: { caption: 'duvod_pro_odmitnuti', label: 'duvod' },
      panelClass: 'app-register-dialog'
    });

    rejectDialogRef.afterClosed().subscribe(result => {
      if (result) {
        let items = this.zadost.identifiers.filter(it => !this.isItemProcessed(it));
        this.service.rejectItemsBatch(items, this.zadost,result).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.zamitnuti_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.zamitnuti_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });

        /*
        this.service.rejectItems(items, this.zadost,result).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.zamitnuti_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.zamitnuti_navrhu_success', '', false);
            this.zadost = res;
            this.getDocs(this.route.snapshot.queryParams);
          }
        });
        */
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', 'alert.oduvodneni_chybi', true);
      }
    });
  }

  /** Pokud je navrh vnl, respektujeme pouze prvni prechod - cilovy stav NL*/
  enBlockActionEnabled() {
    let allprocessed = this.allProcessed();
    if (this.zadost.navrh == 'VNL'){
      if (this.zadost.desired_item_state == 'NL') {
        return !allprocessed;
      } else return false;
    } else {
      return !allprocessed;
    }

  }


  showCorrespondence() {
    const dialogRef = this.dialog.open(DialogCorrespondenceComponent, {
      width: '750px',
      panelClass: 'app-history-identifier',
      data: this.zadost
    });

    dialogRef.afterClosed().subscribe(res => {
        if (res?.result) {
            this.zadost.email = res.result;

            this.service.saveKuratorZadost(this.zadost).subscribe((res: any) => {
              if (res.error) {
                this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
              } else {
                this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
                this.zadost = res;
                this.getDocs(this.route.snapshot.queryParams);
              }
            });
      
        }
    });
  }

}
