import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-consent',
  templateUrl: './consent.component.html',
  styleUrls: ['./consent.component.scss']
})
export class ConsentComponent implements OnInit {

  //isConsent:boolean = true;

  constructor(
    public state: AppState
  ) { }

  ngOnInit(): void {
  }

  close(): void {
    localStorage.setItem("consent", "true");
  }

}
