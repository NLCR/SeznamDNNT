import { Component, Input, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
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

  newState = new FormControl();

  constructor(
    public dialog: MatDialog,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.newState.setValue(this.doc.marc_990a);
  }

  showIdentifiers() {
    const data = {
      title: 'Identifikatory zaznamu ' + this.doc.title,
      items: []
    }

    this.config.identifiers.forEach(f => {
      if (this.doc['marc_' + f]) {
        data.items.push({label: 'field.'+f, value: this.doc['marc_' + f]})
      }
    });
    

    const dialogRef = this.dialog.open(DataDialogComponent, {
        width: '750px',
        data
      });
  }

  showHistory() {
    const dialogRef = this.dialog.open(HistoryDialogComponent, {
        width: '750px',
        data: this.doc
      });
    

    // dialogRef.afterClosed().subscribe(result => {
    //   console.log('The dialog was closed', result);
    // });
  }

  showStates() {
    const dialogRef = this.dialog.open(StatesDialogComponent, {
      width: '1150px',
      data: this.doc
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
      console.log(this.doc)
       
     });
  }

}

