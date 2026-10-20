import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { HistoryItem } from 'src/app/shared/history-item';
import { SolrDocument } from 'src/app/shared/solr-document';

export enum KuratorStavColumns {
  DEFAULT_COLUMNS,
  ADDITOIONAL_A_PN_COLUMN,
  ADDITOIONAL_N_PN_COLUMN
};


@Component({
  selector: 'app-dialog-history',
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss']
})
export class DialogHistoryComponent implements OnInit {


  // plna historie
  history: HistoryItem[] = [];
  // verejne stavy
  stavy: HistoryItem[] = [];
  // kuratorske stavy
  kuratorskestavy: HistoryItem[] = [];

  // zpristupneni v sablone ??  Divne
  KuratorStavColumns = KuratorStavColumns;

  kuratorColumns: KuratorStavColumns = KuratorStavColumns.DEFAULT_COLUMNS;

  pnDeadlineReq: Date;
  pnDeadlineConfigReq: string;

  pnDeadlineImport: Date;
  pnDeadlineConfigImport: string;

  kategorieGranulovanychStavu=[];
  granulovaneStavyAggregated={};

  // pokud ma granulovane stavy
  granularity = false;


  constructor(
    public dialogRef: MatDialogRef<DialogHistoryComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {


    this.stavy = this.data.historie_stavu;
    this.stavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4), 10);
          const m = parseInt(d.substr(4, 2), 10) - 1;
          const day = parseInt(d.substr(6, 2), 10);
        h.date = new Date(y, m, day);

      }
    });

    this.kuratorskestavy = this.data.historie_kurator_stavu;
    this.kuratorskestavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4), 10);
          const m = parseInt(d.substr(4, 2), 10) - 1;
          const day = parseInt(d.substr(6, 2), 10);
        h.date = new Date(y, m, day);
      }
    });

    if (this.kuratorskestavy[this.kuratorskestavy.length-1] && this.kuratorskestavy[this.kuratorskestavy.length-1].stav === 'PN') {
      if (this.stavy[this.stavy.length-1] && this.stavy[this.stavy.length-1].stav === 'N') {

              this.kuratorColumns = KuratorStavColumns.ADDITOIONAL_N_PN_COLUMN;

              this.service.pnDeadlineInfo(this.data.identifier, 'PNREQ').subscribe((data)=> {
                if (data.deadline) {
                  this.pnDeadlineReq = new Date(data.deadline);
                  this.pnDeadlineConfigReq = '('+data.value +' '+data.unit+')';
                }
              });

      } else {

              this.kuratorColumns = KuratorStavColumns.ADDITOIONAL_A_PN_COLUMN;

              this.service.pnDeadlineInfo(this.data.identifier, 'import').subscribe((data)=> {
                if (data.deadline) {
                  this.pnDeadlineImport = new Date(data.deadline);
                  this.pnDeadlineConfigImport = '('+data.value +' '+data.unit+')';
                }
              });

      }

    }


    this.granularity = (this.data.fmt === 'SE' || this.data.fmt === 'BK') && this.data.granularity;

    this.data.historie_granulovaneho_stavu.forEach(hiist=> {
      const stringRep = JSON.stringify(hiist);
      const copy = JSON.parse(stringRep);
      const date = copy.date;
      const key = copy.group ?  date +'/'+copy.group : date;

      if (!this.granulovaneStavyAggregated[key]) {
        this.granulovaneStavyAggregated[key] = [];
      }
      this.granulovaneStavyAggregated[key].push(copy);


      if (!this.kategorieGranulovanychStavu.includes(key)) {
        this.kategorieGranulovanychStavu.push(key);
      }
    });

    this.kategorieGranulovanychStavu.sort((a,b) => {
      let leftkey = 0;
      let rightkey = 0;
      if (a.includes('/')) {
        leftkey = parseInt(a.substr(a.indexOf('/')+1), 10);
      } else {
        leftkey = parseInt(a, 10);
      }

      if (b.includes('/')) {
        rightkey = parseInt(b.substr(b.indexOf('/')+1), 10);
      } else {
        rightkey = parseInt(b, 10);
      }


      return leftkey > rightkey ? 1 : -1;
    });

    for (const k of Object.keys(this.granulovaneStavyAggregated)) {
      const inst = this.granulovaneStavyAggregated[k];
      inst.map(h => {
        if (!(h.date instanceof Date)) {
          const d: string = h.date;
          const y = parseInt(d.substr(0, 4), 10);
            const m = parseInt(d.substr(4, 2), 10) - 1;
            const day = parseInt(d.substr(6, 2), 10);
          h.date = new Date(y, m, day);
        }
      });
      inst.sort((a,b) => {
        if (a.rocnik && b.rocnik) {
          if (a.rocnik === b.rocnik) {
            if (a.acronym && b.acronym) {
              return (a.acronym > b.acronym ? 1 : -1);
            } else  if (a.acronym && !b.acronym) {
              return 1;
            } else {return -1;}

          } else {return (a.rocnik > b.rocnik ? 1 : -1);}
        } else if (a.rocnik && !b.rocnik) {
          return 1;
        } else {return -1;}
      });
    }

    /*
    let itms =  this.granulaovaneStavy.sort((a,b) => {
      if (a.rocnik && b.rocnik) {
        return (a.rocnik > b.rocnik ? 1 : -1);
      } else if (a.rocnik && !b.rocnik) {
        return 1;
      } else return -1;
    }
    );*/


    //console.log(this.granulaovaneStavy);
  }


  shortText(longText: string) {
    if (longText.length > 9) {
      return longText.substring(0,9) + ' ...';
    } else {return longText;}
  }

}
