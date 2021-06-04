import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { Zadost } from 'src/app/shared/zadost';
import { DataDialogComponent } from '../data-dialog/data-dialog.component';
import { HistoryDialogComponent } from '../history-dialog/history-dialog.component';
import { StatesDialogComponent } from '../states-dialog/states-dialog.component';

@Component({
  selector: 'app-result-item',
  templateUrl: './result-item.component.html',
  styleUrls: ['./result-item.component.scss']
})
export class ResultItemComponent implements OnInit {

  @Input() doc: SolrDocument;
  @Input() inZadost: boolean;
  @Input() zadost: Zadost;

  newState = new FormControl();
  isZarazeno: boolean;
  hasNavhr: boolean;
  imgSrc: string;
  process: {[key: string]: string};

  constructor(
    public dialog: MatDialog,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.newState.setValue(this.doc.marc_990a);
    this.isZarazeno = this.doc.marc_990a?.includes('A') || this.doc.marc_990a?.includes('PA');
    this.hasNavhr = !!this.doc.zadost;
    if (this.doc.marc_956u) {
      // Je to kramerius
      const link: string = this.doc.marc_956u[0];
        console.log(link);
      // http://krameriusndk.nkp.cz/search/handle/uuid:960bc370-c6c0-11e2-b6da-005056827e52 
      if (link.indexOf('handle') > -1) {
        this.imgSrc = link.replace('/handle/', '/api/v5.0/item/') + '/thumb';
      }
      
    } else if (this.doc.marc_856u) {
      if (this.doc.marc_856u[0].indexOf('books.google') > 0) {
        // google books
        const link: string = this.doc.marc_856u[0];
        const id = link.substring( link.indexOf('vid=')+4 , link.indexOf('&'));
        this.service.findGoogleBook(id).subscribe((res: any) => {
          if (res[id]) {
            this.imgSrc = res[id].thumbnail_url;
          }
          // this.imgSrc = res.items[0].volumeInfo.imageLinks.smallThumbnail;
        });
      } else {
        // this.imgSrc = this.doc.marc_856u;
      }
      
    }
  }

  showIdentifiers() {
    const data = {
      title: this.doc.title,
      items: [],
    }

    this.config.identifiers.forEach(f => {
      if (this.doc['marc_' + f]) {
        data.items.push({label: 'field.'+f, value: this.doc['marc_' + f]})
      }
    });
    

    const dialogRef = this.dialog.open(DataDialogComponent, {
        width: '750px',
        data,
        panelClass: 'app-data-dialog'
      });
  }

  showHistory() {
    const dialogRef = this.dialog.open(HistoryDialogComponent, {
        width: '750px',
        data: this.doc,
        panelClass: 'app-history-dialog'
      });
    

    // dialogRef.afterClosed().subscribe(result => {
    //   console.log('The dialog was closed', result);
    // });
  }

  public showStates() {
    const dialogRef = this.dialog.open(StatesDialogComponent, {
      width: '1150px',
      data: this.doc,
      panelClass: 'app-states-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.doc.marc_990a = result;
        const dataField = 
        {
          "ind2": " ",
          "ind1": " ",
          "tag": "990",
          "subFields": {
            "a": [
              {
                "code": "a",
                "value": result
              }
            ]
          }
        }

        this.doc.raw.dataFields['990'] = [dataField];
        this.service.saveRecord(this.doc.identifier, this.doc.raw).subscribe(res => {
          console.log(res);
        });
      }
       
     });
  }

  addToZadost() {
    const new_stav = this.isZarazeno ? 'VVS' : 'NZN'
    if (!this.state.currentZadost[new_stav]) {
      const z = new Zadost(new Date().getTime() + '', this.state.user.username);
      z.new_stav = new_stav;
      this.state.currentZadost[new_stav] = z;
    }
    this.state.currentZadost[new_stav].identifiers.push(this.doc.identifier);
    this.service.saveZadost(this.state.currentZadost[new_stav]).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('add_to_zadost_error', res.error, true);
      } else {
        this.service.showSnackBar('add_to_zadost_uspesna', '', false);
      }
    });
  }

  approve(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.zadost = res; 
        this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

  approveLib(doc: SolrDocument) {
    this.service.approveNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('approve_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('approve_navrh_success', '', false);
        this.zadost = res; 
        this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

  reject(doc: SolrDocument) {
    this.service.rejectNavrh(doc.identifier, this.zadost).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('reject_navrh_error', res.error, true);
      } else {
        this.service.showSnackBar('reject_navrh_success', '', false);
        this.zadost = res;
        this.process = this.zadost.process ? JSON.parse(this.zadost.process) : {};
      }
    });
  }

}

