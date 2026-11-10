import { HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { AppService } from 'src/app/app.service';
import { DialogExportedFilesComponent } from 'src/app/components/dialog-exported-files/dialog-exported-files.component';
import { SolrDocument } from 'src/app/shared/solr-document';
import { AppState } from 'src/app/app.state';
import { map, startWith, debounce, debounceTime } from 'rxjs/operators';
import { FlexModule } from '@angular/flex-layout/flex';

import { FacetsComponent } from '../../../components/facets/facets.component';
import { PaginatorComponent } from '../../../components/paginator/paginator.component';
import { MatFormField, MatLabel, MatSuffix } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatIconButton } from '@angular/material/button';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIcon } from '@angular/material/icon';
import { MatCard } from '@angular/material/card';
import { MatButtonToggle } from '@angular/material/button-toggle';
import { MatMenuTrigger, MatMenu, MatMenuItem } from '@angular/material/menu';
import { ResultItemComponent } from '../../../components/result-item/result-item.component';
import { TranslateModule } from '@ngx-translate/core'; // autocomplete


@Component({
    selector: 'app-export',
    templateUrl: './export.component.html',
    styleUrls: ['./export.component.scss'],
    imports: [
    FlexModule,
    RouterLink,
    FacetsComponent,
    PaginatorComponent,
    MatFormField,
    MatLabel,
    MatInput,
    FormsModule,
    MatIconButton,
    MatSuffix,
    MatTooltip,
    MatIcon,
    MatCard,
    MatButtonToggle,
    MatMenuTrigger,
    MatMenu,
    MatMenuItem,
    ResultItemComponent,
    TranslateModule
]
})
export class ExportComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  state = inject(AppState);
  dialog = inject(MatDialog);


  private subject: Subject<string> = new Subject();


  docs: SolrDocument[] = [];

  public numFound = 30;
  public imgSrc = false;
  public exportname: string;
  public query: string;

  public exportObj: any;
  public exportFiles: any[];
  facets: any;

  loading: boolean;


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
        this.service.getExport(this.exportname).subscribe((exportResponse)=> {
          if (exportResponse.response.docs && exportResponse.response.docs.length > 0) {
            this.exportObj = exportResponse.response.docs[0];
          }
        });

        this.service.getExportFiles(this.exportname).subscribe((exp)=> {
          this.exportFiles = exp.files;
        });

        this.state.prefixsearch.export = val.exportq;
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

    if (this.exportObj.export_type === 'IOCP') {

      this.service.approveExportIOCP(this.exportname).subscribe((exp)=> {
        this.getDocs({});

        this.service.getExport(this.exportname).subscribe((exportResponse)=> {
          if (exportResponse.response.docs && exportResponse.response.docs.length > 0) {
            this.exportObj = exportResponse.response.docs[0];
          }
        });
      });

    } else {

      this.service.approveExportUOCP(this.exportname).subscribe((exp)=> {
        this.getDocs({});

        this.service.getExport(this.exportname).subscribe((exportResponse)=> {
          if (exportResponse.response.docs && exportResponse.response.docs.length > 0) {
            this.exportObj = exportResponse.response.docs[0];
          }
        });
      });

    }

  }

  encodePath(path) {
    return encodeURIComponent(path);
  }
  openExportedFilesDialog() {

    const data = {
      exportname: this.exportname,
      files: []
    };
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



  processExport(data: { type: string; identifier: string }) {
    switch (data.type) {
      case 'exported':

        if (this.exportObj.export_type === 'UOCP') {
          this.service.approveExportItemUOCP(this.exportObj.id, data.identifier).subscribe((res: any) => {
            if (res.error) {
              this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
            } else {
              this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);

              this.service.getExport(this.exportname).subscribe((exp)=> {
                if (exp.response.docs && exp.response.docs.length > 0) {
                  this.exportObj = exp.response.docs[0];

                  this.getDocs(this.route.snapshot.queryParams);
                }
              });
            }
          });

      } else {
        this.service.approveExportItemIOCP(this.exportObj.id, data.identifier).subscribe((res: any) => {
          if (res.error) {
            this.service.showSnackBar('alert.schvaleni_navrhu_error', res.error, true);
          } else {
            this.service.showSnackBar('alert.schvaleni_navrhu_success', '', false);

            this.service.getExport(this.exportname).subscribe((exp)=> {
              if (exp.response.docs && exp.response.docs.length > 0) {
                this.exportObj = exp.response.docs[0];

                this.getDocs(this.route.snapshot.queryParams);
              }
            });
          }
        });
      }
        break;
      }
    }
}
