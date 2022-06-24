import { HttpParams } from '@angular/common/http';
import { identifierModuleUrl } from '@angular/compiler';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DialogHistoryComponent } from 'src/app/components/dialog-history/dialog-history.component';
import { DialogIdentifierComponent } from 'src/app/components/dialog-identifier/dialog-identifier.component';
import { DialogPromptComponent } from 'src/app/components/dialog-prompt/dialog-prompt.component';
import { DialogStatesComponent } from 'src/app/components/dialog-states/dialog-states.component';
import { GranularityComponent } from 'src/app/components/granularity/granularity.component';
import { Import } from 'src/app/shared/import';
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

  eanAlephLink: string = '';

  import: Import;

  constructor(
    public dialog: MatDialog,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute,
    private router: Router,
    private config: AppConfiguration,
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
      this.getImport();
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

  getImport() {
    this.service.getImport(this.importId).subscribe((resp: any) => {
      this.import = resp.response.docs[0];
    });
  }

  getDocs(params: Params) {
    this.docs = [];
    this.filteredIds = {};
    const p = Object.assign({}, params);
    p.id = this.importId;
    this.service.getImportDocuments(p as HttpParams).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.facets = resp.facet_counts.facet_fields;
      this.numFound = resp.response.numFound;
      // this.nejen_na_vyrazeni = resp.stats.stats_fields.na_vyrazeni.count;
      // this.ean = resp.facet_counts.facet_fields.hit_type.ean;
      // this.noHits = resp.facet_counts.facet_fields.num_hits['0'];
      // if (!this.initialized) {
      //   this.date = this.docs[0].import_date;
      //   this.uri = this.docs[0].import_uri;
      //   this.origin = this.docs[0].import_origin;
      // }
      
      this.docs.forEach(doc => {
        doc.identifiers.forEach(id => {
          if (doc.ean && id.ean && id.ean.includes(doc.ean)) {
            doc.eanAlephLink = 'http://aleph.nkp.cz/F/?func=direct&local_base=SKC&doc_number=' + id.identifier.substr(id.identifier.lastIndexOf('-') + 1);
            return;
          }
        });
      });
      this.docs.forEach(doc => {

        const f = doc.identifiers.filter(id => {
          // if (this.fullCatalog) {
          //   return true;
          // }
          if (id.changedInImport) {
            return true;
          }
          if (!id.dntstav || id.dntstav.includes('N') || id.dntstav.includes('X') ||  id.dntstav.includes('D')) {
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

  showIdentifiers(id) {

    this.service.getCatalogDoc(id.identifier).subscribe(res => {
      if (res.response.docs.length > 0) {

        const data = {
          title: res.response.docs[0].title,
          items: [],
        }
        data.items.push({ label: 'Aleph identifier', value: res.response.docs[0]['identifier'] })

        this.config.identifiers.forEach(f => {
          if (res.response.docs[0]['marc_' + f]) {
            data.items.push({ label: 'field.' + f, value: res.response.docs[0]['marc_' + f] })
          }
        });


        const dialogRef = this.dialog.open(DialogIdentifierComponent, {
          width: '750px',
          data,
          panelClass: 'app-dialog-identifier'
        });
      }
    });

  }

  showHistory(doc) {
    this.service.getCatalogDoc(doc.identifier).subscribe(res => {
      if (res.response.docs.length > 0) {

        const dialogRef = this.dialog.open(DialogHistoryComponent, {
          width: '750px',
          data: res.response.docs[0],
          panelClass: 'app-history-identifier'
        });

      }
    });
  }

  showGranularity(id) {
    // this.service.getCatalogDoc(doc.identifier).subscribe(res => {
    //   if (res.response.docs.length > 0) {

        const data = { title: id.nazev, items: id.granularity };

        const dialogRef = this.dialog.open(GranularityComponent, {
          width: '1150px',
          data: data,
          panelClass: 'app-dialog-states'
        });
    //   }
    // });
  }

  showStates(doc, id) {

    const dialogRef = this.dialog.open(DialogStatesComponent, {
      width: '1150px',
      data: id,
      panelClass: 'app-dialog-states'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.change) {
        this.service.changeStavDirect(id.identifier, result.newState, result.newLicense, result.poznamka, result.granularity).subscribe(res => {

          if (res.response.docs.length > 0) {
            id.dntstav = res.response.docs[0].dntstav;
            id.kuratorstav = res.response.docs[0].kuratorstav;
            id.license = res.response.docs[0].license;
            id.changedInImport = true;
            this.service.changeStavImport(doc).subscribe(res => {
              // this.getDocs(this.route.snapshot.queryParams);
            });
          }
        });
      }

    });
  }

  curatorAndPublicStateAreDifferent(doc: any) {
    // neni nastaveny public stav ale ma kuratorsky stav NPA 
    if (doc.kuratorstav && !doc.dntstav) {
      return true;
      // verejny a kuratorsky stav je rozdilny
    } else if (doc.kuratorstav && doc.dntstav && doc.kuratorstav[doc.kuratorstav.length - 1] != doc.dntstav[doc.dntstav.length - 1]) {
      return true;
    }
    return false;
  }

  setControlled(doc) {

    const approveDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: {caption: 'komentar', label: 'komentar'},
      panelClass: 'app-register-dialog'
    });

    approveDialogRef.afterClosed().subscribe(result => {
      console.log(result)
      if (result !== undefined) {
        const note = result ? result : '';
        doc.controlled_note = note;
        this.service.setImportControlled(doc).subscribe(res => {
          doc.controlled = true;
          doc.controlled_user = this.state.user.username;
          // this.getDocs(this.route.snapshot.queryParams);
        });
      }


    });


    
  }

}
