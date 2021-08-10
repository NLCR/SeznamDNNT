import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent implements OnInit {
  
  public currentYear = new Date().getFullYear();

  public gitHash:string;


  constructor(private service: AppService) { }

  ngOnInit(): void {
    this.service.getGitInfo().subscribe(res => {
      console.log(res)
      this.gitHash = res["git.commit.id.abbrev"];
    });

  }

}
