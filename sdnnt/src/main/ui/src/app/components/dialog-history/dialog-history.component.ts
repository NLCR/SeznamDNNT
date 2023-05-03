import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { HistoryItem } from 'src/app/shared/history-item';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-dialog-history',
  templateUrl: './dialog-history.component.html',
  styleUrls: ['./dialog-history.component.scss']
})
export class DialogHistoryComponent implements OnInit {

  history: HistoryItem[] = [];
  stavy: HistoryItem[] = [];
  kuratorskestavy: HistoryItem[] = [];

  kategorieGranulovanychStavu=[];
  granulovaneStavyAggregated={};


  granularity: boolean = false;


  constructor(
    public dialogRef: MatDialogRef<DialogHistoryComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public state: AppState,
    private service: AppService) { }

  ngOnInit(): void {
    //console.log(this.kategorieGranulovanychStavu);
    //this.stavy = this.data.historie_stavu;


    this.stavy = this.data.historie_stavu;
    this.stavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4)),
          m = parseInt(d.substr(4, 2)) - 1,
          day = parseInt(d.substr(6, 2));
        h.date = new Date(y, m, day);

      }
    });

    this.kuratorskestavy = this.data.historie_kurator_stavu;
    this.kuratorskestavy.map(h => {
      if (!(h.date instanceof Date)) {
        const d: string = h.date;
        const y = parseInt(d.substr(0, 4)),
          m = parseInt(d.substr(4, 2)) - 1,
          day = parseInt(d.substr(6, 2));
        h.date = new Date(y, m, day);
      }
    });

    this.granularity = (this.data.fmt === 'SE' || this.data.fmt === 'BK') && this.data.granularity;
    

    //this.granulaovaneStavy = this.data.historie_granulovaneho_stavu;

    this.data.historie_granulovaneho_stavu.forEach(hiist=> {
      let stringRep = JSON.stringify(hiist);
      let copy = JSON.parse(stringRep)
      let date = copy['date'];
      let key = copy['group'] ?  date +"/"+copy['group'] : date;

      if (!this.granulovaneStavyAggregated[key]) {
        this.granulovaneStavyAggregated[key] = [];
      }
      this.granulovaneStavyAggregated[key].push(copy);
      

      if (!this.kategorieGranulovanychStavu.includes(key)) {
        this.kategorieGranulovanychStavu.push(key);
      }
    });

    this.kategorieGranulovanychStavu.sort((a,b) => {
      let leftkey:number = 0;
      let rightkey:number = 0;
      if (a.includes('/')) {
        leftkey = parseInt(a.substr(a.indexOf('/')+1));
      } else {
        leftkey = parseInt(a);
      }

      if (b.includes('/')) {
        rightkey = parseInt(b.substr(b.indexOf('/')+1));
      } else {
        rightkey = parseInt(b);
      }


      return leftkey > rightkey ? 1 : -1; 
    });

    for(let k in this.granulovaneStavyAggregated) {
      let inst = this.granulovaneStavyAggregated[k];
      inst.map(h => {
        if (!(h.date instanceof Date)) {
          const d: string = h.date;
          const y = parseInt(d.substr(0, 4)),
            m = parseInt(d.substr(4, 2)) - 1,
            day = parseInt(d.substr(6, 2));
          h.date = new Date(y, m, day);
        }
      });
      inst.sort((a,b) => {
        if (a.rocnik && b.rocnik) {
          if (a.rocnik == b.rocnik) {
            if (a.acronym && b.acronym) {
              return (a.acronym > b.acronym ? 1 : -1); 
            } else  if (a.acronym && !b.acronym) {
              return 1;
            } else return -1;

          } else return (a.rocnik > b.rocnik ? 1 : -1); 
        } else if (a.rocnik && !b.rocnik) {
          return 1;
        } else return -1;
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


  shortText(longText:string) {
    if (longText.length > 9) {
      return longText.substring(0,9) + " ...";
    } else return longText;
  }

}
