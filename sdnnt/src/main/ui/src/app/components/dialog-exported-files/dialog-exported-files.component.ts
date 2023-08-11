import { Component, OnInit, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface ExportedFilesData {
  export: string;
  files : {
    name: string;
    path: string;
  }[];
}

@Component({
  selector: 'app-dialog-exported-files',
  templateUrl: './dialog-exported-files.component.html',
  styleUrls: ['./dialog-exported-files.component.scss']
})
export class DialogExportedFilesComponent implements OnInit {

  public files: any = [
  ];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ExportedFilesData
  ) { 
  }

  ngOnInit(): void {}

  encodePath(path) {
    return encodeURIComponent(path);
  }

}
