import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-help',
  templateUrl: './help.component.html',
  styleUrls: ['./help.component.scss']
})
export class HelpComponent implements OnInit {

  content: string;


  constructor(public state: AppState,
    private service: AppService,
    ) { }

  ngOnInit(): void {
    this.state.activePage = 'Help';
    this.helpContent();

    this.service.langChanged.subscribe(() => {
        this.helpContent();
    })
  }

  helpContent() {
    if (this.state.user != null) {
      this.service.getText('help_'+this.state.user.role).subscribe(text => this.content = text);
    } else {
      this.service.getText('help').subscribe(text => this.content = text);
    }
  }
}
