import { Component, Input, OnInit } from '@angular/core';

@Component({
  standalone: false,
  selector: 'app-account-item',
  templateUrl: './account-item.component.html',
  styleUrls: ['./account-item.component.scss']
})
export class AccountItemComponent implements OnInit {

  @Input() item;
  constructor() { }

  ngOnInit(): void {
  }

}
