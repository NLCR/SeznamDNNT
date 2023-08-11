import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { DialogExportedFilesComponent } from 'src/app/components/dialog-exported-files/dialog-exported-files.component';
import { SolrDocument } from 'src/app/shared/solr-document';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  subs: Subscription[] = [];
  docs: SolrDocument[] = [];

  public numFound: number = 30;
  public imgSrc: boolean = false;
  public export:string;
  public query:string;

  public exportObj:any;
  public exportFiles:any[];

  loading: boolean;


  constructor(

    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState,
    public dialog: MatDialog
    ) { }


    ngOnInit(): void {

      if (!this.state.user) {
        this.router.navigate(['/']);
        return;
      }
   
      const id = this.route.snapshot.paramMap.get('id');

      this.subs.push(this.route.queryParams.subscribe(val => {
        this.export = id;
        this.query = this.route.snapshot.queryParamMap.get('q') ;

        this.getDocs(val);
        this.service.getExport(this.export).subscribe((exp)=> {
          this.exportObj = exp;
        });

        this.service.getExportFiles(this.export).subscribe((exp)=> {
          this.exportFiles = exp.files;
        });
      }));
  


    //this.service.searchInExports


    // this.route.queryParams.subscribe(val => {
    //   this.search(val);
    //   this.newStavFilter = val.navrh;
    //   this.stateFilter = val.state;
    // });


    // this.subs.push(this.route.queryParams.subscribe(val => {
    //   //
    //   console.log("ID "+id);
    // }));
  }

  getDocs(params: Params) {
    const p = Object.assign({}, params);
    p.export = this.export;
    this.service.searchInExports(p as HttpParams).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
    });
  }

  encodePath(path) {
    return encodeURIComponent(path);
  }
  openExportedFilesDialog() {

    const data = {
      export: this.export,
      files: []
    }
    this.exportFiles.forEach((e)=> {
      data.files.push(e);
    });
 
    const dialogRef = this.dialog.open(DialogExportedFilesComponent, {
      data,
      width: '600px',
      panelClass: 'app-dialog-exported-files'
    });
  }

  cleanFilterExport() {
    // todo
  }

}
