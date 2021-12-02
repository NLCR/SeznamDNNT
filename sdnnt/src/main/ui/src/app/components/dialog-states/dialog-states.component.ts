import { Component, Inject, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-dialog-states',
  templateUrl: './dialog-states.component.html',
  styleUrls: ['./dialog-states.component.scss']
})
export class DialogStatesComponent implements OnInit {

  newState: string;
  newLicense: string;
  poznamka: string;
  public dntStates: string[] = [ 'A', 'PA', 'NPA', 'N',  'NL', 'X', 'PX'];
  granularity: any[] = [];

  rocnik: string;
  cislo: string;
  link: string;
  stav: string[];
  fmt: string;
  
  constructor(
    public dialogRef: MatDialogRef<DialogStatesComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public config: AppConfiguration,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    this.newState = this.data.kuratorstav ?  this.data.kuratorstav[0]  : 'A';
    this.newLicense = this.data.license ? this.data.license[0] : null  ;
    this.granularity = this.data.granularity ? this.data.granularity : [];
    this.fmt = this.data.fmt ? this.data.fmt : null;
    // this.dntStates = this.config.dntStates[this.state.user ? this.state.user.role : 'user'];
  }

  change() {
    if (!this.poznamka || this.poznamka === '') {
      this.service.showSnackBar('poznamka_povinna', 'poznamka_povinna', true);
      return;
    } else {
      this.dialogRef.close({newState: this.newState, newLicense: this.newLicense, poznamka: this.poznamka, granularity: this.granularity, change: true});
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

  changeState(evt: any) {
    if ((this.newState === 'PA' || this.newState === 'A') && (!this.newLicense)) {
      this.newLicense = 'dnnto';
    }
  }
}
