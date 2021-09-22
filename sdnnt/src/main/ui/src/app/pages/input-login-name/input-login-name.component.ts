import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { InputLoginNameDialogComponent } from 'src/app/components/input-login-name-dialog/input-login-name-dialog.component';

@Component({
  selector: 'app-input-login-name',
  templateUrl: './input-login-name.component.html',
  styleUrls: ['./input-login-name.component.scss']
})
export class InputLoginNameComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog 
  ) {

    const data = {};
    const dialogRef = this.dialog.open(InputLoginNameDialogComponent, {
      width: '800px',
      data,
      panelClass: 'app-data-dialog'
    });

    dialogRef.afterClosed().subscribe(result => {
      this.router.navigate(['/home'], {});
    });

   }


  ngOnInit(): void {
  }

}
