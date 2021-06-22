import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-import',
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.scss']
})
export class ImportComponent implements OnInit {

  numFound: number;
  docs: SolrDocument[] = [];
  onlyA: boolean;
  onlyNoEAN: boolean;
  na_vyrazeni: number;
  noean: number;

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
    this.importId = this.route.snapshot.paramMap.get('id');
    this.getDocs();
  }

  onlyAChange(e) {
    this.onlyA = e.checked;
    this.getDocs();
  }

  onlyNoEANChange(e) {
    this.onlyNoEAN = e.checked;
    this.getDocs();
  }

  getDocs() {
    this.docs = [];
    this.service.getImport(this.importId, this.onlyA, this.onlyNoEAN).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
      this.na_vyrazeni = resp.stats.stats_fields.na_vyrazeni.count;
      this.noean = resp.facet_counts.facet_fields.hit_type.noean;
      if (!this.initialized) {
        this.date = this.docs[0].import_date;
        this.uri = this.docs[0].import_uri;
        this.origin = this.docs[0].import_origin;
      }
      this.initialized = true;
    });

  }

  checkHits(doc) {

  }

  approve(doc) {

  }

}
