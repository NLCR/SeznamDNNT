import { HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';

import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatTooltip } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-all-publishers',
    templateUrl: './dialog-all-publishers.component.html',
    styleUrls: ['./dialog-all-publishers.component.scss'],
    imports: [MatDialogTitle, MatFormField, MatInput, CdkScrollable, MatDialogContent, MatCheckbox, MatTooltip, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogAllPublishersComponent implements OnInit {
  dialogRef = inject<MatDialogRef<DialogAllPublishersComponent>>(MatDialogRef);
  data = inject<{
    queryParams: HttpParams;
}>(MAT_DIALOG_DATA);
  private service = inject(AppService);


  private subject: Subject<string> = new Subject();


  visibleShowMoreButton = true;

  facetSearchOffset=0;
  facetFieldsInDialog: any;
  selectedFacets: any[] = [];

  searchTerm: string;


  ngOnInit(): void {

    const p = Object.assign({}, this.data.queryParams, {
      facetSearchField: 'nakladatel',
      facetSearchOffset: this.facetSearchOffset,
      ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
    });

    this.service.facetSearch(p as HttpParams).subscribe((resp: SolrResponse) => {
      this.facetFieldsInDialog = resp.facet_counts.facet_fields.nakladatel;
    });

    this.subject.pipe(
      debounceTime(600)
    ).subscribe(searchTextValue => {

      this.searchTerm = searchTextValue;

      const searchParams = Object.assign({}, this.data.queryParams, {
        facetSearchField: 'nakladatel',
        facetSearchOffset: this.facetSearchOffset,
        ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
      });

      this.service.facetSearch(searchParams as HttpParams).subscribe((resp: SolrResponse) => {
        this.facetFieldsInDialog = resp.facet_counts.facet_fields.nakladatel;
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

    const p = Object.assign({}, this.data.queryParams, {
      facetSearchField: 'nakladatel',
      facetSearchOffset: this.facetSearchOffset,
      ...(this.searchTerm && { facetSearchPrefix: this.searchTerm })
    });

    this.service.facetSearch(p as HttpParams).subscribe((resp: SolrResponse) => {
      if (resp.facet_counts.facet_fields.nakladatel.length>0) {
        resp.facet_counts.facet_fields.nakladatel.forEach((f)=>{

          this.facetFieldsInDialog.push(f);
        });
      } else {
        this.visibleShowMoreButton =false;
      }
    });
  }
}
