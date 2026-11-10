import { Component, EventEmitter, Input } from '@angular/core';
import { Export } from 'src/app/shared/exports';
import { SolrDocument } from 'src/app/shared/solr-document';

import { MatButtonToggle } from '@angular/material/button-toggle';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-result-actions-export',
    templateUrl: './result-actions-export.component.html',
    styleUrls: ['./result-actions-export.component.scss'],
    imports: [MatButtonToggle, MatTooltip, MatIcon, TranslateModule]
})
export class ResultActionsExportComponent {

  @Input() export: Export;
  @Input() doc: SolrDocument;
  @Input() processExportEvent: EventEmitter<{ type: string; identifier: string }>;

  constructor() { }



  exported() {
    this.processExportEvent.emit({ type: 'exported', identifier: this.doc.identifier});
  }
}
