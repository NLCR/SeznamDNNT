import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-result-actions-search',
  templateUrl: './result-actions-search.component.html',
  styleUrls: ['./result-actions-search.component.scss']
})
export class ResultActionsSearchComponent implements OnInit {

  @Input() doc:SolrDocument;

  hasNavhr:boolean;
  view:string;


  constructor() { }

  ngOnInit(): void {
  }

}
