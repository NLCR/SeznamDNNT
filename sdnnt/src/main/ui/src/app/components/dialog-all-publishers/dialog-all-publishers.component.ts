import { HttpParams } from '@angular/common/http';
import { Component, Inject, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';
import { DataDialogData } from '../dialog-identifier/dialog-identifier.component';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppState } from 'src/app/app.state';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-dialog-all-publishers',
  templateUrl: './dialog-all-publishers.component.html',
  styleUrls: ['./dialog-all-publishers.component.scss']
})
export class DialogAllPublishersComponent implements OnInit {

  private subject: Subject<string> = new Subject();


  visibleShowMoreButton:boolean = true;

  facetSearchOffset=0;
  facetFieldsInDialog:any;
  selectedFacets: any[] = [];

  searchTerm:string;

  constructor(
    public dialogRef: MatDialogRef<DialogAllPublishersComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DataDialogData,
    private service: AppService
    ) { }


  ngOnInit(): void {

    let p = Object.assign({}, this.data['queryParams'], {
      facetSearchField: 'nakladatel',
      facetSearchOffset: this.facetSearchOffset,
      ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
    });

    this.service.facetSearch(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.facetFieldsInDialog = resp.facet_counts.facet_fields['nakladatel'];
    });
  
    this.subject.pipe(
      debounceTime(600)
    ).subscribe(searchTextValue => {

      this.searchTerm = searchTextValue;

      const p = Object.assign({}, this.data['queryParams'], {
        facetSearchField: 'nakladatel',
        facetSearchOffset: this.facetSearchOffset,
        ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
      });

      this.service.facetSearch(p as HttpParams).subscribe((resp: SolrResponse) => {
        this.facetFieldsInDialog = resp.facet_counts.facet_fields['nakladatel'];
      });
    });

  }

  onFilterByPrefixKeyUp(target) {
    this.subject.next(target.value);
  }

  onCheckboxChange(facet: any, isChecked: boolean) {
    if (isChecked) {
      this.selectedFacets.push(facet);
    } else {
      const index = this.selectedFacets.findIndex(f => f.field === facet.field);
      if (index > -1) {
        this.selectedFacets.splice(index, 1);
      }
    }
  }

  get selectedFacetsNames(): string {
    return this.selectedFacets.map(facet => facet.name).join(', ');
  }

  applyAndClose() {
    this.dialogRef.close({selectedFacets:this.selectedFacets});
  }

  showMore() {
    this.facetSearchOffset+=100;

    const p = Object.assign({}, this.data['queryParams'], {
      facetSearchField: 'nakladatel',
      facetSearchOffset: this.facetSearchOffset,
      ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
    });

    this.service.facetSearch(p as HttpParams).subscribe((resp: SolrResponse) => {
      if (resp.facet_counts.facet_fields['nakladatel'].length>0) {
        resp.facet_counts.facet_fields['nakladatel'].forEach((f)=>{

          this.facetFieldsInDialog.push(f);
        });
      } else {
        this.visibleShowMoreButton =false;
      }
    });
  }
}
