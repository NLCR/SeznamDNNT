import { ArrayDataSource } from '@angular/cdk/collections';
import { Component, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DocsUtils } from 'src/app/shared/docutils';
import { SolrDocument } from 'src/app/shared/solr-document';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { NgIf, NgFor, NgClass, KeyValuePipe } from '@angular/common';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTooltip } from '@angular/material/tooltip';
import { ExtendedModule } from '@angular/flex-layout/extended';
import { MatCheckbox } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-successor-records',
    templateUrl: './dialog-successor-records.component.html',
    styleUrls: ['./dialog-successor-records.component.scss'],
    imports: [
      MatDialogTitle,
      CdkScrollable,
      MatDialogContent,
      NgIf,
      MatCard,
      MatCardContent,
      MatIcon,
      NgFor,
      MatTooltip,
      NgClass,
      ExtendedModule,
      MatCheckbox,
      FormsModule,
      MatDialogActions,
      MatButton,
      MatDialogClose,
      KeyValuePipe,
      TranslateModule
    ]
})
export class DialogSuccessorRecordsComponent {
  dialogRef = inject<MatDialogRef<DialogSuccessorRecordsComponent>>(MatDialogRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(AppService);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  dat = inject(MAT_DIALOG_DATA);


  documents = [];

  edit = false;

  constructor() {
      const dat = this.dat;


      this.edit = dat.edit;

      dat.docs.forEach((dd)=> {
        this.documents.push({
          expanded:false,
          doc:dd,
          selected: true,
          marcview: this.filterView(dd.raw)
        });
      });
    }


  title(doc: SolrDocument) {
    return DocsUtils.title(doc);
  }

  hlavnizahlavi(doc: SolrDocument) {
    return DocsUtils.hlavnizahlavi(doc);
  }


  vydani(doc: SolrDocument) {
    return DocsUtils.vydani(doc);
  }

  nakladatelskeUdaje(doc: SolrDocument) {
    return DocsUtils.nakladatelskeUdaje(doc);
  }
  nakladatel(doc: SolrDocument) {
    return DocsUtils.nakladatel(doc);
  }

  ccnb(doc: SolrDocument) {
    if (doc.id_ccnb && doc.id_ccnb.length > 0) {
      return doc.id_ccnb[0];
    }
  }

  fondy(doc: SolrDocument) {
    const fondy = [];
    if (doc.marc_910a) {
      doc.marc_910a.forEach(i=> {
        const t = `${i} (${this.service.getTranslation('sigla.'+i)})`;
        fondy.push(t);
      });
    }
    return fondy.join(', ');
  }

  digitallibraries(doc: SolrDocument) {
    const fondy = [];
    if (doc.digital_libraries) {
      doc.digital_libraries.forEach(i=> {
        const t = `${i} (${this.service.getTranslation('sigla.'+i)})`;
        fondy.push(t);
      });
      return fondy.join(', ');
    }
  }

  filterView(strDoc) {
    const parsed = JSON.parse(strDoc);
    /*
    Object.keys(parsed).forEach(key => {
      if (forbiddenKeys.indexOf(key) < 0) {
        retval[key] = parsed[key];
      }
    });
    */
    const retval = {
      FMT: parsed.fmt,
      identifier: parsed.identifier,
      datestamp: parsed.datestamp,
      leader: parsed.leader,
      controlFields: parsed.controlFields,
      dataFields: [] as { key: number; value: string }[]
    };

    const transformedDataFields = [];

    for (const key of Object.keys(parsed.dataFields)) {
      const marcFieldAr = parsed.dataFields[key];

      marcFieldAr.forEach(i=> {
        //let tag = i.tag;
        let str = '';
        //let numbers = [];
        //let chars = [];

        let subFieldsArr = [];
        Object.keys(i.subFields).forEach(k => {
          subFieldsArr.push(i.subFields[k]);
        });
        subFieldsArr = subFieldsArr.sort(function(a, b){
          if (a && a[0] && b && b[0]) {
            if (a[0].index > b[0].index) { return 1; }
            if (a[0].index < b[0].index) {return -1; }
          }
          return 0;
        });


        for (const subField of subFieldsArr) {
          str = str + '|' + subField[0].code + '  ' + subField[0].value;
        }

        transformedDataFields.push({ key: i.tag, value: str });
      });

    }

    transformedDataFields.sort(function(a, b){
      return a.key - b.key;
    });


    retval.dataFields = transformedDataFields;

    return retval;
  }
  /*
  raw(doc) {
    return JSON.parse(doc.raw);
  }*/

  accept() {
    const selected = [];
    this.documents.forEach(d=> {
      if (d.selected) {
        selected.push(d.doc.identifier);
      }
    });

    this.dialogRef.close({options: selected.length > 0 ? selected.join(',')  : ''});
  }

  goto(url, event) {
    window.open(url, '_blank', 'noreferrer');
    if (event) {event.stopPropagation(); }
    return;
  }

}
