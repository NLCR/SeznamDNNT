import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Export } from 'src/app/shared/exports';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-result-actions-export',
  templateUrl: './result-actions-export.component.html',
  styleUrls: ['./result-actions-export.component.scss']
})
export class ResultActionsExportComponent implements OnInit {
 
  @Input() export: Export;
  @Input() doc:SolrDocument;
  @Input() processExportEvent: EventEmitter<{ type:string, identifier: string }>;
 
  constructor() { }

  ngOnInit(): void {
  }

  
  exported() {
    this.processExportEvent.emit({ type: 'exported', identifier: this.doc.identifier});
  }
}
