import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-zadost-send-dialog',
  templateUrl: './zadost-send-dialog.component.html',
  styleUrls: ['./zadost-send-dialog.component.scss']
})
export class ZadostSendDialogComponent implements OnInit {

  newState: string;
  poznamka: string;
  pozadavek: string;
  // public dntStates: string[] = ['undefined', 'PA', 'A', 'VS', 'VN', 'N', 'NZN', 'VVN', 'VVS'];
  public dntStates: string[] = ['NZN', 'VVN'];

  constructor(
    public dialogRef: MatDialogRef<ZadostSendDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: Zadost,
    private service: AppService) { }

  ngOnInit(): void {
  }

  send() {
    this.data.datum_zadani = new Date();
    this.data.pozadavek = this.pozadavek;
    this.data.poznamka = this.poznamka;
    this.data.state = 'waiting';
    this.service.saveZadost(this.data).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('send_zadost_error', res.error, true);
      } else {
        this.service.showSnackBar('send_zadost_uspesna', '', false);
        this.dialogRef.close();
      }
    })
  }

}
