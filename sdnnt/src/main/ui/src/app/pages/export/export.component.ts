import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-export',
  templateUrl: './export.component.html',
  styleUrls: ['./export.component.scss']
})
export class ExportComponent implements OnInit {

  public numFound: number = 30;
  public imgSrc: boolean = false;

  constructor() { }

  ngOnInit(): void {
  }

}
