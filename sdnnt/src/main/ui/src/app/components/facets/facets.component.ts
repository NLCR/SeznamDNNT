import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.scss']
})
export class FacetsComponent implements OnInit {

  @Input() facet_fields: {[field: string]: {name: string, type: string, value: number}[]};
  facets: string[];

  constructor() { }

  ngOnInit(): void {
    this.facets = Object.keys(this.facet_fields);
  }


}
