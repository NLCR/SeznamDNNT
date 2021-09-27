import { Component, OnInit } from '@angular/core';
import { CookieService } from 'ngx-cookie'
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-consent',
  templateUrl: './consent.component.html',
  styleUrls: ['./consent.component.scss']
})
export class ConsentComponent implements OnInit {


  constructor(
    private cookieService: CookieService,
    public state: AppState
  ) { }

  ngOnInit(): void {
  }

  close(): void {
    this.cookieService.put("consent", "true");
  }

}
