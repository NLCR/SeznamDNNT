import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateParser, TranslateService } from '@ngx-translate/core';

export class PaginatorI18n extends MatPaginatorIntl {

  private rangeLabelIntl: string;

  getRangeLabel = (page, pageSize, length) => {
      if (length === 0 || pageSize === 0) {
        return this.translate.instant('paginator.desc.rangeLabel1', { length });
      }
      length = Math.max(length, 0);
      const startIndex = page * pageSize;
      // If the start index exceeds the list length, do not try and fix the end index to the end.
      const endIndex = startIndex < length ? Math.min(startIndex + pageSize, length) : startIndex + pageSize;
      return this.translate.instant('paginator.desc.rangeLabel2', { startIndex: startIndex + 1, endIndex, length });
  };

  constructor(private readonly translate: TranslateService) {
    super();
    this.getTranslations();
    this.translate.onLangChange.subscribe(() => this.getTranslations());
   }

  getTranslations() {
    this.translate.get([
      'paginator.desc.itemsPerPageLabel',
      'paginator.desc.nextPageLabel',
      'paginator.desc.previousPageLabel',
      'paginator.desc.firstPageLabel',
      'paginator.desc.lastPageLabel'
    ])
      .subscribe(translation => {
        this.itemsPerPageLabel = translation['paginator.desc.itemsPerPageLabel'];
        this.nextPageLabel = translation['paginator.desc.nextPageLabel'];
        this.previousPageLabel = translation['paginator.desc.previousPageLabel'];
        this.firstPageLabel = translation['paginator.desc.firstPageLabel'];
        this.lastPageLabel = translation['paginator.desc.lastPageLabel'];
        this.getRangeLabel = this.getRangeLabel.bind(this);
        this.changes.next();
      });
  }

  // getPaginatorIntl() {
  //   const paginatorIntl = new MatPaginatorIntl();
  //   paginatorIntl.itemsPerPageLabel = this.translate.instant('itemsPerPageLabel');
  //   paginatorIntl.nextPageLabel = this.translate.instant('nextPageLabel');
  //   paginatorIntl.previousPageLabel = this.translate.instant('previousPageLabel');
  //   paginatorIntl.firstPageLabel = this.translate.instant('firstPageLabel');
  //   paginatorIntl.lastPageLabel = this.translate.instant('lastPageLabel');
  //   paginatorIntl.getRangeLabel = this.getRangeLabel.bind(this);
  //   return paginatorIntl;
  // }

}