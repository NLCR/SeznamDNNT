import { Component, Inject, Input, OnInit,OnChanges, SimpleChanges, ViewChild, ElementRef } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { User } from 'src/app/shared/user';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatInput } from '@angular/material/input';
import { MatCheckbox } from '@angular/material/checkbox';

@Component({
  selector: 'app-dialog-registration-form',
  templateUrl: './dialog-registration-form.component.html',
  styleUrls: ['./dialog-registration-form.component.scss']
})
export class DialogRegistrationFormComponent implements OnInit, OnChanges {

  @Input() user: User;
  @Input() isRegister: boolean;
  @Input() focus: string;
  @Input() scroll: string;




  @ViewChild('username') username: MatInput;
  @ViewChild('email') email: MatInput;
  @ViewChild('jmeno') jmeno: MatInput;
  @ViewChild('phonenumber') phonenumber: MatInput;
  @ViewChild('ico') ico: MatInput;


  @ViewChild('conditionOne') conditionOneCheck: MatCheckbox;
  @ViewChild('conditionTwo') conditionTwoCheck: MatCheckbox;
  @ViewChild('conditionThree') conditionThreeCheck: MatCheckbox;

  @ViewChild('condition') conditionsElement: ElementRef; 


  inputFocusables: {[key: string] : MatInput};
  checkboxFocusables: {[key: string] : MatCheckbox};

  formTypeSelected: number = 1;
  isApiEnabled: boolean;

  autor: boolean;
  dedic: boolean;
  jiny: boolean;
  nakladatel: boolean;
  institutions: string[];


  //notifikace_interval: string = 'mesic';

  constructor(
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService) { }
  
  
  ngOnChanges(changes: SimpleChanges): void {
    this.isApiEnabled = this.user && this.user.apikey && this.user.apikey != null && this.user.apikey !== ""; 
    if (this.user) {
      this.autor = this.user.nositel?.includes('autor');
      this.dedic = this.user.nositel?.includes('dedic');
      this.jiny = this.user.nositel?.includes('jiny');
      this.nakladatel = this.user.nositel?.includes('nakladatel');
    }

    // @ViewChild('phonenumber') phonenumber: MatInput;
    // @ViewChild('ico') ico: MatInput;
  
    this.inputFocusables = {
      username: this.username,
      email: this.email,
      jmeno: this.jmeno,
      phonenumber: this.phonenumber,
      ico: this.ico
    }

    this.checkboxFocusables = {
      condition1: this.conditionOneCheck,
      condition2: this.conditionTwoCheck,
      condition3: this.conditionThreeCheck
    }


    

    if (changes.focus && changes.focus.currentValue) {

      if (this.inputFocusables[changes.focus.currentValue]) {
        this.inputFocusables[changes.focus.currentValue].focus();
      } 

      if (this.checkboxFocusables[changes.focus.currentValue]) {
        this.checkboxFocusables[changes.focus.currentValue].focus();
        this.conditionsElement.nativeElement.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }
  }

  ngOnInit(): void {

      this.service.getInstitutions().subscribe(res => {
        this.institutions = res.institutions.map(function(val,index) {
          return val.acronym;
        });
      });
  
  }


  intitutionChangeEnabled(): boolean {
    const retval:boolean = this.user.role == 'admin' || this.isRegister;
    console.log("Returning value "+retval);
    return retval;
  }

  
  setNositel() {
    this.user.nositel = [];
    if (this.autor) {
      this.user.nositel.push('autor');
    }
    if (this.dedic) this.user.nositel.push('dedic');
    if (this.jiny) this.user.nositel.push('jiny');
    if (this.nakladatel) this.user.nositel.push('nakladatel');
  }

  switchEnableApi() {
    if (this.isApiEnabled) {
      this.user.apikey = Guid.newGuid().toString();
      console.log("Generated api key "+this.user.apikey);

    } else {

      this.user.apikey = null;
      console.log("Deleted api key "+this.user.apikey);
    }
  }

  
}

class Guid {
  static newGuid() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
          const r = Math.random() * 16 | 0, v = c === 'x' ? r : ( r & 0x3 | 0x8 );
          return v.toString(16);
      });
  }
}

