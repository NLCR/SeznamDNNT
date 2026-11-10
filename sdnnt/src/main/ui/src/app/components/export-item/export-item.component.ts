import { Component, Input, inject } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { MatCard, MatCardHeader, MatCardTitle, MatCardSubtitle, MatCardActions } from '@angular/material/card';
import { FlexModule } from '@angular/flex-layout/flex';

import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-export-item',
    templateUrl: './export-item.component.html',
    styleUrls: ['./export-item.component.scss'],
    imports: [MatCard, MatCardHeader, FlexModule, MatCardTitle, MatCardSubtitle, MatCardActions, MatTooltip, MatIcon, TranslateModule]
})
export class ExportItemComponent {
  state = inject(AppState);
  config = inject(AppConfiguration);
  private service = inject(AppService);


  @Input() doc: SolrDocument;

  alephLink: string;
  alternativeAlephLink: string;
  showAlephLink = true;
  public imgSrc = false;


  curatorAndPublicStateAreDifferent(): boolean {
    // neni nastaveny public stav ale ma kuratorsky stav NPA
    if (this.doc.kuratorstav && !this.doc.dntstav) {
      return true;
      // verejny a kuratorsky stav je rozdilny
    } else if (this.doc.kuratorstav && this.doc.dntstav && this.doc.kuratorstav[this.doc.kuratorstav.length - 1] !== this.doc.dntstav[this.doc.dntstav.length - 1]) {
      return true;
    }
    return false;
  }

}
