import { HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
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
  facets: any;
  fullCatalog: boolean;
  onlyEAN: boolean;
  onlyNoHits: boolean;
  nejen_na_vyrazeni: number;
  ean: number;
  noHits: number;

  importId: string;
  date: Date;
  origin: string;
  uri: string;
  initialized = false;
  filteredIds: { [id: string]: any[] };

  constructor(
    private sanitizer: DomSanitizer,
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
      this.onlyEAN = this.route.snapshot.queryParamMap.get('onlyEAN') === 'true';
      this.onlyNoHits = this.route.snapshot.queryParamMap.get('onlyNoHits') === 'true';
      this.fullCatalog = this.route.snapshot.queryParamMap.get('fullCatalog') === 'true';

      this.getDocs(val);
    }));
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  fullCatalogChange(e) {
    this.fullCatalog = e.checked;
    const params: any = {};
    params.fullCatalog = e.checked;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });

    // this.getDocs();
  }

  onlyEANChange(e) {
    this.onlyEAN = e.checked;

    const params: any = {};
    params.onlyEAN = e.checked;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });

    //this.getDocs();
  }

  onlyNoHitsChange(e) {
    this.onlyNoHits = e.checked;
    const params: any = {};
    params.onlyNoHits = e.checked;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  getDocs(params: Params) {
    this.docs = [];
    this.filteredIds = {};
    const p = Object.assign({}, params);
    p.id = this.importId;
    this.service.getImport(p as HttpParams).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.facets = resp.facet_counts.facet_fields;
      this.numFound = resp.response.numFound;
      // this.nejen_na_vyrazeni = resp.stats.stats_fields.na_vyrazeni.count;
      // this.ean = resp.facet_counts.facet_fields.hit_type.ean;
      // this.noHits = resp.facet_counts.facet_fields.num_hits['0'];
      if (!this.initialized) {
        this.date = this.docs[0].import_date;
        this.uri = this.docs[0].import_uri;
        this.origin = this.docs[0].import_origin;
      }
      this.docs.forEach(doc => {
        const f = doc.identifiers.filter(id => {
          if (this.fullCatalog) {
            return true;
          }
          if (!id.dntstav) {
            return false;
          }
          if (!this.onlyEAN) {
            return true;
          }
          if (!id.ean) {
            return false;
          }
          return (id.ean && doc.ean && id.ean.includes(doc.ean.toString()));
        });
        this.filteredIds[doc.id] = f;
      });
      this.initialized = true;
    });

  }

  approve(doc, identifier) {
    this.service.approveNavrhInImport(identifier, doc).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);
        doc.identifiers = res.identifiers;
      }
    });
  }

  alephLink(id: string) {
    return 'https://aleph.nkp.cz/F/?func=direct&local_base=DNT&doc_number=' + id.substr(id.lastIndexOf('-') + 1);
    // return 'http://aleph.nkp.cz/F/?func=direct&local_base=SKC&doc_number=' + id.substr(id.lastIndexOf('-') + 1);
  }

  sanitize(url: string) {
    return this.sanitizer.bypassSecurityTrustUrl(url);
  }

  
  gotoAleph(id) {
    window.open(this.alephLink(id.identifier), "_blank", 'noreferrer');
    return;
  }

  showIdentifiers() {

  }

  showHistory() {

  }

  hasGranularity(id) {
    return false;
  }

  showGranularity() {

  }

  showStates() {

  }

  curatorAndPublicStateAreDifferent(doc: any) {
    // neni nastaveny public stav ale ma kuratorsky stav NPA 
    if (doc.kuratorstav && !doc.dntstav) {
      return true; 
    // verejny a kuratorsky stav je rozdilny
    } else if (doc.kuratorstav && doc.dntstav &&  doc.kuratorstav[doc.kuratorstav.length-1] != doc.dntstav[doc.dntstav.length-1])  {
      return true;
    }
    return false;
  }

  setControlled(doc) {
    this.service.setImportControlled(doc).subscribe(res => {
      
    });
  }

}
