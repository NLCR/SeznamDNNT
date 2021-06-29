import { HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-import',
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.scss']
})
export class ImportComponent implements OnInit, OnDestroy {

  subs: Subscription[] = [];
  numFound: number;
  docs: SolrDocument[] = [];
  onlyA: boolean;
  onlyNoEAN: boolean;
  onlyNoHits: boolean;
  na_vyrazeni: number;
  noean: number;
  noHits: number;

  importId: string;
  date: Date;
  origin: string;
  uri: string;
  initialized = false;

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
    this.subs.push(this.route.queryParams.subscribe(val => {
      this.importId = this.route.snapshot.paramMap.get('id');
      this.onlyNoEAN = !!this.route.snapshot.queryParamMap.get('onlyNoEAN');
      this.onlyNoHits = !!this.route.snapshot.queryParamMap.get('onlyNoHits');
      this.onlyA = !!this.route.snapshot.queryParamMap.get('onlyA');
      this.getDocs(val);
    }));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  onlyAChange(e) {
    this.onlyA = e.checked;
    const params: any = {};
    params.onlyA = e.checked;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });

    // this.getDocs();
  }

  onlyNoEANChange(e) {
    this.onlyNoEAN = e.checked;
    //this.getDocs();
  }

  onlyNoHitsChange(e) {
    this.onlyNoHits = e.checked;
    //this.getDocs();
  }

  getDocs(params: Params) {
    this.docs = [];
    const p = Object.assign({}, params);
    p.id = this.importId;
    this.service.getImport(p as HttpParams).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
      this.na_vyrazeni = resp.stats.stats_fields.na_vyrazeni.count;
      this.noean = resp.facet_counts.facet_fields.hit_type.noean;
      this.noHits = resp.facet_counts.facet_fields.num_hits['0'];
      if (!this.initialized) {
        this.date = this.docs[0].import_date;
        this.uri = this.docs[0].import_uri;
        this.origin = this.docs[0].import_origin;
      }
      this.initialized = true;
    });

  }

  approve(doc, identifier) {
    this.service.approveNavrhInImport(identifier, doc).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
        doc = res;
      }
    });
  }

  link(id: string) {
    return 'http://aleph.nkp.cz/F/?func=direct&local_base=SKC&doc_number=' + id.substr(id.lastIndexOf('-')+1);
  }

}
