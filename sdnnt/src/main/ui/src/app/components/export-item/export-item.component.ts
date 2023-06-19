import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-export-item',
  templateUrl: './export-item.component.html',
  styleUrls: ['./export-item.component.scss']
})
export class ExportItemComponent implements OnInit {
  public imgSrc: boolean = false;

  constructor() { }

  ngOnInit(): void {
  }

}
