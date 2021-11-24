import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-dialog-delete-request',
  templateUrl: './dialog-delete-request.component.html',
  styleUrls: ['./dialog-delete-request.component.scss']
})
export class DialogDeleteRequestComponent implements OnInit {

  constructor(
    private router: Router,
    private service: AppService,
    @Inject(MAT_DIALOG_DATA) public data: Zadost
  ) { }

  ngOnInit(): void {
  }

  deleteRequest() {
    this.service.deleteZadost(this.data).subscribe(res => {
      if (res.error) {
        this.service.showSnackBar('alert.smazani_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.smazani_zadosti_success', '', false);
      }
    });
  }

}
