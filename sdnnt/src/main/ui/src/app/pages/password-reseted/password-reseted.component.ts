import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Subscription } from 'rxjs';

enum Tokenvalidation {
  INVALID, VALID
}


@Component({
  selector: 'app-password-reseted',
  templateUrl: './password-reseted.component.html',
  styleUrls: ['./password-reseted.component.scss']
})
export class PasswordResetedComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    public state: AppState

  ) { }

  tokenValidity: boolean;  
  token: string;
  userName: string;
  userFirstname: string;
  userSurname: string;

  ngOnInit(): void {
    this.route.paramMap.subscribe(val => {
      if (val.has("token")) {
        this.token = val.get("token");
        this.service.activateToken(this.token) .subscribe((res:any) => {
          if (res.error) {
            this.tokenValidity = false;
          } else {
            this.tokenValidity = true;
            this.userName = val.get("username");
            this.userFirstname = val.get("jmeno");
            this.userSurname = val.get("prijmeni");
          }
        });
      } 
    });
  }

}
