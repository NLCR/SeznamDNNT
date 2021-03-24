import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {

  constructor(
    private service: AppService,
    public state: AppState
    ) { }

  ngOnInit(): void {
  }

  changeLang(lang: string) {
    this.service.changeLang(lang);
  }
  

}
