import { Component, OnInit } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  
  activeTab: string;
  tabContent: string;

  constructor(
    public config: AppConfiguration,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.selectTab(this.config.homeTabs[0]);
  }

  login() {

  }

  register() {

  }

  selectTab(tab: string) {
    this.activeTab = tab;
    this.service.getText(tab).subscribe(text => this.tabContent = text);
  }

}
