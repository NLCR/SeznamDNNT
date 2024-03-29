import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-dialog-send-request',
  templateUrl: './dialog-send-request.component.html',
  styleUrls: ['./dialog-send-request.component.scss']
})
export class DialogSendRequestComponent implements OnInit {

  newState: string;
  poznamka: string;
  pozadavek: string;

  constructor(
    public dialogRef: MatDialogRef<DialogSendRequestComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Zadost,
    private service: AppService) { }

  ngOnInit(): void {
  }

  send() {
    
    this.data.pozadavek = this.pozadavek;
    this.data.poznamka = this.poznamka;

    this.service.sendZadost(this.data).subscribe((res: any) => {
      if (res.error) {
        if (res.key) {
          this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
        } else {
          this.service.showSnackBar(res.key, '', true);

        }
      } else {
        this.data = <Zadost>res;
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.dialogRef.close();
      }
    });

    
    /*
    this.data.datum_zadani = new Date();
    this.data.pozadavek = this.pozadavek;
    this.data.poznamka = this.poznamka;
    // sluzba 
    this.data.state = 'waiting';
    this.service.saveZadost(this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
        this.dialogRef.close();
      }
    });
    */

  }

}
