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
  docs: SolrDocument[];
  hideProcessed: boolean;
  na_vyrazeni: number;

  importId: string;

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
    this.getDocs(false);
  }

  onlyA(e) {
    console.log(e);
    this.getDocs(e.checked);
  }

  getDocs(onlyA: boolean) {
    this.service.getImport(this.importId, onlyA).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
      this.na_vyrazeni = resp.stats.stats_fields.na_vyrazeni.count;
    });

  }

}
