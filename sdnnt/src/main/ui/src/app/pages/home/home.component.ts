import { HttpParams } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FlexModule } from '@angular/flex-layout/flex';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

import { MatButton } from '@angular/material/button';
import { MatCard, MatCardActions, MatCardAvatar, MatCardHeader, MatCardSubtitle, MatCardTitle } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { GraphsComponent } from '../../components/graphs/graphs.component';

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.scss'],
    imports: [
    FlexModule,
    MatCard,
    RouterLink,
    MatCardHeader,
    MatCardAvatar,
    MatIcon,
    MatCardTitle,
    MatCardSubtitle,
    MatCardActions,
    MatButton,
    MatTabGroup,
    MatTab,
    GraphsComponent,
    TranslateModule
]
})
export class HomeComponent implements OnInit {
  config = inject(AppConfiguration);
  state = inject(AppState);
  private service = inject(AppService);



  activeTab: string;
  tabContent: string;
  showStats = false;

  facets: any;

  cardsFacets: object = {
    A:0,
    PA:0,
    N:0,
    dntt:0,
    dnto:0,
  };

  ngOnInit(): void {
    this.state.activePage = 'Home';
    this.getText(this.config.homeTabs[0]);
    this.service.langChanged.subscribe(() => {
      this.service.getText(this.activeTab).subscribe(text => this.tabContent = text);
    });

    const p = Object.assign({}, {});
    this.service.search(p as HttpParams).subscribe((res)=>{

      this.facets = res.facet_counts.facet_fields;

      for (const dntstav of res.facet_counts.facet_fields.dntstav) {
        this.cardsFacets[dntstav.name] = dntstav.value;
      }
      for (const license of res.facet_counts.facet_fields.license) {
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
