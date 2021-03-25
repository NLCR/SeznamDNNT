import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  loading: boolean;
  docs;

  constructor(
    private route: ActivatedRoute,
    private service: AppService
    ) { }


  ngOnInit(): void {
    this.route.queryParams.subscribe(val => {
      this.search(val);
    });
  }

  search(params: Params) {
    this.loading = true;
    const p = Object.assign({}, params);
    // p.mapa = !this.state.isMapaCollapsed;
    this.service.search(p as HttpParams).subscribe((resp: SolrResponse) => {
      // this.state.setSearchResponse(resp);
      this.docs = resp.response.docs;
      this.loading = false;
    });

  }

}
