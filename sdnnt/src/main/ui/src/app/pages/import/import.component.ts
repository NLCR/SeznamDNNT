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

    this.service.getImport(id).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
    });

  }

}
