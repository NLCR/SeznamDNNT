import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dialog-delete-request',
  templateUrl: './dialog-delete-request.component.html',
  styleUrls: ['./dialog-delete-request.component.scss']
})
export class DialogDeleteRequestComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

  deleteRequest() {
    // metoda pro smazani zadosti
  }

}
