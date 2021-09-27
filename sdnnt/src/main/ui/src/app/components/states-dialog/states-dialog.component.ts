import { Component, Inject, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-states-dialog',
  templateUrl: './states-dialog.component.html',
  styleUrls: ['./states-dialog.component.scss']
})
export class StatesDialogComponent implements OnInit {

  newState: string;
  poznamka: string;
  public dntStates: string[] = ['PA', 'A', 'VS', 'VN', 'N', 'NZ'];
  granularity: any[] = [];

  rocnik: string;
  cislo: string;
  link: string;
  stav: string[];
  fmt: string;
  
  constructor(
    public dialogRef: MatDialogRef<StatesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public config: AppConfiguration,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    this.newState = this.data.dntstav ? (this.data.dntstav.includes('NZ') ?  'NZ' :this.data.dntstav[0])  : 'A';
    this.granularity = this.data.granularity ? this.data.granularity : [];
    this.fmt = this.data.fmt ? this.data.fmt : null;
    // this.dntStates = this.config.dntStates[this.state.user ? this.state.user.role : 'user'];
  }

  change() {
    if (!this.poznamka || this.poznamka === '') {
      this.service.showSnackBar('poznamka_povinna', 'poznamka_povinna', true);
      return;
    } else {
      this.dialogRef.close({newState: this.newState, poznamka: this.poznamka, granularity: this.granularity, change: true});
    }
  }

  addGranularitu() {
    this.granularity.push({rocnik: this.rocnik, cislo: this.cislo, link: this.link, stav: this.stav});
    this.rocnik = '';
    this.cislo = '';
    this.link = '';
    this.stav = [];
  
  }

  removeGranularitu(idx: number) {
    this.granularity.splice(idx, 1);
  }

  shouldShowGranularity(): boolean {
    return (this.fmt != null && this.fmt === "SE");
  }
}
