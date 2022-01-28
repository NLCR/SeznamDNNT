import { HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDatepickerContent } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogBulkProposalComponent } from 'src/app/components/dialog-bulk-proposal/dialog-bulk-proposal.component';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit, OnDestroy {

  subs: Subscription[] = [];
  // all actions
  actions: string[] = [];
  // ident x actions mapping
  identifiersAndActionsMapping = new Map<string, string[]>(); 



  loading: boolean;
  docs: SolrDocument[];
  searchResponse: SolrResponse;
  facets;
  numFound: number;
  hasStateFilter: boolean;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog
  ) { }


  ngOnInit(): void {
    this.state.activePage = 'Search';
    this.subs.push(this.route.queryParams.subscribe(val => {
      this.search(val);
    }));
    this.subs.push(this.state.paramsProcessed.subscribe(val => {
      this.hasStateFilter = this.state.usedFilters.findIndex(f => f.field === 'dntstav') > -1;
    }));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  search(params: Params) {
    this.loading = true;
    // this.service.showLoading();
    const p = Object.assign({}, params);
    this.docs = [];
    this.searchResponse = null;
    this.facets = null;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {

      this.searchResponse = resp;
      this.docs = resp.response.docs;
      if (resp.zadosti) {
        this.docs.forEach(doc => {
          const identifier = doc.identifier;
          resp.zadosti.forEach(z => {
            if (z.state !== 'processed') {
              if (z.identifiers.includes(identifier)) {
                doc.zadost = z;
              }
            }  
          });
        });
      }

      // global actions
      this.actions = [];
      this.identifiersAndActionsMapping = new Map<string, string[]>(); 
      if (resp.actions) {
        for (let key in resp.actions) {
          let object = resp.actions[key];
          if (object.workflows) {
            let workflows: string[] = object.workflows;
            this.identifiersAndActionsMapping.set(key, workflows);
            object.workflows.forEach(workflow=> {
              let acts: string[] = this.actions;
              if (acts.indexOf(workflow) < 0 ) {
                this.actions.push(workflow);
              }
            });
          }
        }
      }
      if (resp.notifications) {
        this.docs.forEach(doc => {
          const identifier = doc.identifier;
          resp.notifications.forEach(z => {
            if (z.identifier.includes(identifier)) {
              doc.hasNotifications = true;
            }
          });
        });
      }
      this.numFound = resp.response.numFound;
      this.facets = resp.facet_counts.facet_fields;
      this.loading = false;
    });

  }

  hromadnaZadostEnabled() {
    return this.identifiersAndActionsMapping.size  > 0;
  } 

  /** Pripravi a ulozi zadost po te reload */
  saveZadost(navrh: string, identifiers: string[]) {
    const key =  navrh === 'VNL' || navrh === 'VNZ' ?  'VNX' : navrh;
    if  (!this.state.currentZadost[key].identifiers) {
      this.state.currentZadost[key].identifiers = [];
    }    
    identifiers.forEach(ident=> {
      this.state.currentZadost[key].identifiers.push(ident);
    });

    this.service.saveZadost(this.state.currentZadost[key]).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);

        // reload docs
        this.subs.push(this.route.queryParams.subscribe(val => {
          this.search(val);
        }));

      }
    });
  }

  /**
   * Hromadna zadost 
   */
  addHromadnaZadost() {
    let identiefiers = [];
    let selectedAction = this.actions[0];
    this.identifiersAndActionsMapping.forEach((actions: string[], ident: string) => {
        if(actions.indexOf(selectedAction) > -1 ) {
          identiefiers.push(ident);
        }
    });
    if (identiefiers && identiefiers.length > 0) {
      if (selectedAction === 'VNZ' || selectedAction === 'VNL') {
        this.service.prepareZadost(['VNZ','VNL']).subscribe((res: Zadost) => {
          this.state.currentZadost['VNX']= res;
          this.saveZadost(selectedAction, identiefiers);
        });
      } else {
        this.service.prepareZadost([selectedAction]).subscribe((res: Zadost) => {
          this.state.currentZadost[selectedAction]= res;
          this.saveZadost(selectedAction, identiefiers);
        });
      }
    }
  }

  viewFullCatalog() {
    const q: any = {};
    q['fullCatalog'] = this.state.fullCatalog;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  setNotis() {
    const q: any = {};
    q.withNotification = this.state.withNotification;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }


  openBulkProposal() {
    const dialogRef = this.dialog.open(DialogBulkProposalComponent, {
      width: '450px',
      panelClass: 'app-dialog-bulk-proposal'
    });
  }

}
