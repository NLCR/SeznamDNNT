import { Component, Inject, Input, OnInit,OnChanges, SimpleChanges } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { User } from 'src/app/shared/user';
import { FormControl, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss']
})
export class UserFormComponent implements OnInit, OnChanges {

  @Input() user: User;
  @Input() isRegister: boolean;
  
  formTypeSelected: number = 1;
  isApiEnabled: boolean;


  // user: User;

  // @Input() set setUser(u: User) {
  //   this.user = u;
  //   this.isApiEnabled = this.user.apikey && this.user.apikey != null && this.user.apikey !== ""; 
  // }



  constructor(
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService) { }
  
  
  ngOnChanges(changes: SimpleChanges): void {
    this.isApiEnabled = this.user.apikey && this.user.apikey != null && this.user.apikey !== ""; 
    console.log("Changed user "+this.user.username);
  }

  ngOnInit(): void {
  }



  switchEnableApi() {
    /*
    if (this.isApiEnabled()) {
       this.user.apikey = null; 
    } else {
      this.user.apikey = Guid.newGuid().toString();
    }*/
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

