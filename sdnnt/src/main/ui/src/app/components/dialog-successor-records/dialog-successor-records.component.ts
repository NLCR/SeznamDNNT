import { ArrayDataSource } from '@angular/cdk/collections';
import { toBase64String } from '@angular/compiler/src/output/source_map';
import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DocsUtils } from 'src/app/shared/docutils';
import { SolrDocument } from 'src/app/shared/solr-document';

@Component({
  selector: 'app-dialog-successor-records',
  templateUrl: './dialog-successor-records.component.html',
  styleUrls: ['./dialog-successor-records.component.scss']
})
export class DialogSuccessorRecordsComponent implements OnInit {

  documents = [];

  edit: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<DialogSuccessorRecordsComponent>,
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    private config: AppConfiguration,
    public state: AppState,
    @Inject(MAT_DIALOG_DATA) public dat:  any) {

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

  ngOnInit(): void {
  }

  title(doc:SolrDocument) {
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

  fondy(doc:SolrDocument) {
    let fondy = [];
    if (doc.marc_910a) {
      doc.marc_910a.forEach(i=> {
        let t = `${i} (${this.service.getTranslation('sigla.'+i)})`; 
        fondy.push(t);
      });
    }
    return fondy.join(', ');
  }

  digitallibraries(doc:SolrDocument) {
    let fondy = [];
    if (doc.digital_libraries) {
      doc.digital_libraries.forEach(i=> {
        let t = `${i} (${this.service.getTranslation('sigla.'+i)})`; 
        fondy.push(t);
      });
      return fondy.join(', ');
    }
  }

  filterView(strDoc) {
    let retval = {};
    let parsed = JSON.parse(strDoc);
    /*
    Object.keys(parsed).forEach(key => {
      if (forbiddenKeys.indexOf(key) < 0) {
        retval[key] = parsed[key];
      }
    });
    */
    retval["FMT"] = parsed.fmt;
    retval["identifier"] = parsed.identifier;
    retval["datestamp"] = parsed.datestamp;
    retval["leader"] = parsed.leader;
    retval["controlFields"] = parsed.controlFields;
    
    let transformedDataFields = [];

    for (let key in parsed.dataFields) {
      let marcFieldAr = parsed.dataFields[key];
      
      marcFieldAr.forEach(i=> {
        let itemObj = {};
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


        for(let j=0;j<subFieldsArr.length;j++) {
          let sfi = subFieldsArr[j];
          str  = str + '|'+subFieldsArr[j][0].code+"  "+subFieldsArr[j][0].value;
        }

        itemObj['key'] = i.tag; 
        itemObj['value'] = str; 

        transformedDataFields.push(itemObj);
      });
      
    }

    transformedDataFields.sort(function(a, b){
      return a.key - b.key; 
    });
  

    retval["dataFields"] = transformedDataFields;

    return retval;
  }
  /*
  raw(doc) {
    return JSON.parse(doc.raw);
  }*/

  accept() {
    let selected = [];
    this.documents.forEach(d=> {
      if (d.selected) {
        selected.push(d.doc.identifier);
      }
    });

    this.dialogRef.close({options: selected.length > 0 ? selected.join(",")  : ""});
  }

  goto(url, event) {
    window.open(url, "_blank", 'noreferrer');
    if (event) {event.stopPropagation(); }
    return;
  }

}
