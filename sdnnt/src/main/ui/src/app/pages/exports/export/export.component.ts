import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { DialogExportedFilesComponent } from 'src/app/components/dialog-exported-files/dialog-exported-files.component';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  public numFound: number = 30;
  public imgSrc: boolean = false;

  constructor(
    public state: AppState,
    public dialog: MatDialog
  ) { }

  ngOnInit(): void {
  }

  openExportedFilesDialog() {
    const dialogRef = this.dialog.open(DialogExportedFilesComponent, {
      width: '600px',
      panelClass: 'app-dialog-exported-files'
    });
  }

  cleanFilterExport() {
    // todo
  }

}
