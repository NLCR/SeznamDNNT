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
import { SearchResultsUtils } from 'src/app/shared/searchresultsutils';
import { DialogBulkNotificationsComponent } from 'src/app/components/dialog-bulk-notifications/dialog-bulk-notifications.component';
import { AppConfiguration } from 'src/app/app-configuration';

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
  

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog,
    public configuration:AppConfiguration
  ) { }

  ngOnInit(): void {

    this.state.activePage = 'Search';
    this.subs.push(this.route.queryParams.subscribe(val => {
      this.search(val);
    }));
  }



  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  search(params: Params) {
    let utils:SearchResultsUtils = new SearchResultsUtils();

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
        utils.enhanceByRequest(this.docs, resp.zadosti);
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
        utils.enhanceByNotifications(this.docs, resp.notifications);
      }

      if (resp.rnotifications) {
        utils.enhanceByRulebasedNotifications(this.docs,resp.rnotifications);
      }
 
      this.numFound = resp.response.numFound;
      this.facets = resp.facet_counts.facet_fields;

      Object.keys(this.facets).forEach(key => {
        if (this.configuration.sortedFacets.includes(key)) {
            let values = this.facets[key];
            values.sort(function(a, b){
              if(a.name < b.name) { return -1; }
              if(a.name > b.name) { return 1; }
              return 0;
            });
        }
      });
      this.loading = false;
    });
  }

  hromadnaZadostEnabled() {
    let flag =  this.identifiersAndActionsMapping.size  > 0 && this.actions.length > 0;
    return flag;
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
    const data = {};
    this.actions.forEach(action => {
      data[action] = true;
    });
    
    data["docs"] = this.docs;
    data["actions"] = this.actions;
    data["identifiersAndActionsMapping"] = this.identifiersAndActionsMapping; 
    
    const dialogRef = this.dialog.open(DialogBulkProposalComponent, {
      width: '450px',
      data,
      panelClass: 'app-dialog-bulk-proposal'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.route.queryParams.subscribe(val => {
        this.search(val);
      });
    });

  }

  

  selectNofiticationFitler(f) {
    const q: any = {};
    if (f) {
      this.state.notificationSettings.selected = f;
      //this.notificationFilter = f;
      q.notificationFilter =  f.id;
    } else {
      this.state.notificationSettings.selected = null;
      //this.notificationFilter = null;
      q.notificationFilter = null;
    }
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });

  }

  openBulkNotifications() {
    const dialogRef = this.dialog.open(DialogBulkNotificationsComponent, {
      width: '600px',
      panelClass: 'app-dialog-bulk-notifications'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.route.queryParams.subscribe(val => {
        this.search(val);
      });
    });
  }

}
