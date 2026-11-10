import { Component, OnInit, inject } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { FlexModule } from '@angular/flex-layout/flex';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-footer',
    templateUrl: './footer.component.html',
    styleUrls: ['./footer.component.scss'],
    imports: [FlexModule, TranslateModule]
})
export class FooterComponent implements OnInit {
  private service = inject(AppService);


  public currentYear = new Date().getFullYear();

  public gitHash: string;

  ngOnInit(): void {
    this.service.getGitInfo().subscribe(res => {
      this.gitHash = res['git.commit.id.abbrev'];
    });

  }

}
