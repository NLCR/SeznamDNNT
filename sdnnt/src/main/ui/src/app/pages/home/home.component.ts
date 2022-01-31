import { HttpParams } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {

  
  activeTab: string;
  tabContent: string;
  showStats = false;

  facets: any;

  cardsFacets: object = {
    'A':0,
    'PA':0,
    'N':0,
    'dntt':0,
    'dnto':0,
  };

  constructor(
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.state.activePage = 'Home';
    this.getText(this.config.homeTabs[0]);
    this.service.langChanged.subscribe(() => {
      this.service.getText(this.activeTab).subscribe(text => this.tabContent = text);
    });

    const p = Object.assign({}, {});
    this.service.search(p as HttpParams).subscribe((res)=>{

      this.facets = res.facet_counts.facet_fields;

      for (let dntstav of res.facet_counts.facet_fields.dntstav) {
        this.cardsFacets[dntstav.name] = dntstav.value;          
      }
      for (let license of res.facet_counts.facet_fields.license) {
        this.cardsFacets[license.name] = license.value;          
      }
    });
  }


  getText(tab: string) {
    this.activeTab = tab;
    this.service.getText(this.activeTab).subscribe(text => this.tabContent = text);
  }

  selectTab(selected) {
    const tab = this.config.homeTabs[selected.index];
    if (tab) {
      this.getText(tab);
    } else {
      this.showStats = true;
    }
    
  }
  
}
