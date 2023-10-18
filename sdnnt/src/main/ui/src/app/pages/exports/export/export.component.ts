import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { DialogExportedFilesComponent } from 'src/app/components/dialog-exported-files/dialog-exported-files.component';
import { SolrDocument } from 'src/app/shared/solr-document';
import { AppState } from 'src/app/app.state';
import { map, startWith, debounce, debounceTime } from 'rxjs/operators'; // autocomplete


@Component({
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  private subject: Subject<string> = new Subject();


  docs: SolrDocument[] = [];

  public numFound: number = 30;
  public imgSrc: boolean = false;
  public exportname:string;
  public query:string;

  public exportObj:any;
  public exportFiles:any[];
  facets: any;

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
      
      this.route.queryParams.subscribe(val => {
        this.exportname = id;
        this.query = this.route.snapshot.queryParamMap.get('exportq') ;

        this.getDocs(val);
        this.service.getExport(this.exportname).subscribe((exp)=> {
          if (exp.response.docs && exp.response.docs.length > 0) {
            this.exportObj = exp.response.docs[0];
          }
        });

        this.service.getExportFiles(this.exportname).subscribe((exp)=> {
          this.exportFiles = exp.files;
        });
        
        this.state.prefixsearch['export'] = val.exportq;
      });

      this.subject.pipe(
        debounceTime(400)
      ).subscribe(searchTextValue => {
  
        const req: any = {};
  
        req.exportq = searchTextValue;
  
        this.router.navigate([], { queryParams: req, queryParamsHandling: 'merge' });
      });
  }

  getDocs(params: Params) {
    const p = Object.assign({}, params);
    p.exportname = this.exportname;
    if (this.query) {
      p.q = this.query;
    }
    this.service.searchInExports(p as HttpParams).subscribe((resp: any) => {
      this.docs = resp.response.docs;
      this.numFound = resp.response.numFound;
      this.facets = resp.facet_counts.facet_fields;

    });
  }


  setProcessed() {
    this.service.processExport(this.exportname).subscribe(res => {
      this.getDocs({});

      this.service.getExport(this.exportname).subscribe((exp)=> {
        if (exp.response.docs && exp.response.docs.length > 0) {
          this.exportObj = exp.response.docs[0];
        }
      });
    });
  }
  

  approveAll() {
    this.service.approveExport(this.exportname).subscribe((exp)=> {
      this.getDocs({});
      
      this.service.getExport(this.exportname).subscribe((exp)=> {
        if (exp.response.docs && exp.response.docs.length > 0) {
          this.exportObj = exp.response.docs[0];
        }
      });
    });
  }

  encodePath(path) {
    return encodeURIComponent(path);
  }
  openExportedFilesDialog() {

    const data = {
      exportname: this.exportname,
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



  onFilterExportKeyUp(target) {
    this.subject.next(target.value);
  }

  cleanFilterExport() {
    this.subject.next('');
  }
}
