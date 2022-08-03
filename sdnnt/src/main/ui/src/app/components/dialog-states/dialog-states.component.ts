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
  // stavy parent polozky
  public dntStates: string[] = [ 'A', 'PA', 'NPA', 'N',  'NL', 'X', 'PX'];

  // stavy rocniku
  public dntStatesItem: string[] = [ 'A', 'PA', 'NPA', 'N',  'X', 'PX'];

  granularity: any[] = [];
  
  //ngranularity: any[] = [];
  

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
    this.granularity = this.granularity.map(function(itm) {
      return DialogStatesComponent.flat(itm);
    });
  }

  /** TODO: Do it on the server side */
  static flat(obj:any) {
    let retval:any = new Object();
    for (let p in obj) {
        retval[p] =  Array.isArray(obj[p]) ? obj[p][0] : obj[p];
    }
    return retval;
  }

  static array(obj:any) {

    if (obj.kuratorstav && obj.kuratorstav !== 'PA' && obj.kuratorstav !== 'A') {
      delete obj.license;
    }

    switch(obj.kuratorstav) {
      case "A":
        obj.stav = 'A';
      break;
      case "PA":
        obj.stav = 'PA';
      break;
      case "NPA":
        obj.stav = 'N';
      break;
      case "N":
        obj.stav = 'N';
      break;
      case "NL":
        obj.stav = 'NL';
      break;
      case "X":
        obj.stav = 'X';
      break;
      case "PX":
        obj.stav = 'N';
      break;
    }


    if (obj.stav && !Array.isArray(obj.stav)) {
      obj.stav = [obj.stav];
    }
    if (obj.kuratorstav &&  !Array.isArray(obj.kuratorstav)) {
      obj.kuratorstav   = [obj.kuratorstav];    
    }
    return obj;
  }
  /**  */
  
  change() {
    if (!this.poznamka || this.poznamka === '') {
      this.service.showSnackBar('poznamka_povinna', 'poznamka_povinna', true);
      return;
    } else {
       this.dialogRef.close({newState: this.newState, newLicense: this.newLicense, poznamka: this.poznamka, granularity: this.granularity.map(DialogStatesComponent.array), change: true});
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
    return (this.fmt != null);
  }

  changeState(evt: any) {
    if ((this.newState === 'PA' || this.newState === 'A') && (!this.newLicense)) {
      this.newLicense = 'dnnto';
    } else if (this.newState === 'NL')  {
      this.newLicense = 'dnntt'
    }
  }

  changeLicensePossible() {
    return this.newState !== 'PA' && this.newState !=='A';
  }

  setProperty(evt: any) {
    console.log("Test "+evt);
  }

  kuratStavChange() {
    console.log(this.granularity);
  }
}
