import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
    selector: 'app-shibboleth-landing-page',
    templateUrl: './shibboleth-landing-page.component.html',
    styleUrls: ['./shibboleth-landing-page.component.scss']
})
export class ShibbolethLandingPageComponent implements OnInit {
  private router = inject(Router);
  private service = inject(AppService);
  private state = inject(AppState);


  ngOnInit(): void {

    this.service.thirdPartyUser().subscribe((res: any) => {
      this.state.setLogged(res);
      this.router.navigate(['/home'], {});
    });

  }

}
