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
  public dntStates: string[] = ['PA', 'A', 'VS', 'VN', 'N', 'NZN', 'VVN', 'VVS'];


  constructor(
    public dialogRef: MatDialogRef<StatesDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SolrDocument,
    public config: AppConfiguration,
    private service: AppService,
    public state: AppState) { }

  ngOnInit(): void {
    console.log(this.data);
    this.newState = this.data.dntstav ? this.data.dntstav[0] : 'A';
    // this.dntStates = this.config.dntStates[this.state.user ? this.state.user.role : 'user'];
  }

}
