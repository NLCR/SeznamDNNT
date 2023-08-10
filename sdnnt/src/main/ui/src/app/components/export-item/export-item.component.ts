import { Component, OnInit, Input } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-export-item',
  templateUrl: './export-item.component.html',
  styleUrls: ['./export-item.component.scss']
})
export class ExportItemComponent implements OnInit {

  @Input() doc: SolrDocument;

  alephLink: string;
  alternativeAlephLink:string;
  showAlephLink : boolean = true;
  public imgSrc: boolean = false;

  constructor(
    public state: AppState,
    public config: AppConfiguration,
    private service: AppService
  ) { }

  ngOnInit(): void {


  }

  curatorAndPublicStateAreDifferent(): boolean {
    // neni nastaveny public stav ale ma kuratorsky stav NPA 
    if (this.doc.kuratorstav && !this.doc.dntstav) {
      return true;
      // verejny a kuratorsky stav je rozdilny
    } else if (this.doc.kuratorstav && this.doc.dntstav && this.doc.kuratorstav[this.doc.kuratorstav.length - 1] != this.doc.dntstav[this.doc.dntstav.length - 1]) {
      return true;
    }
    return false;
  }

}
