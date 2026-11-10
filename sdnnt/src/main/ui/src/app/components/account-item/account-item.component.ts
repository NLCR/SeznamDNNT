import { Component, Input } from '@angular/core';

@Component({
    selector: 'app-account-item',
    templateUrl: './account-item.component.html',
    styleUrls: ['./account-item.component.scss']
})
export class AccountItemComponent {

  @Input() item;
  constructor() { }


}
