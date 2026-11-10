import { Component, inject } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatIcon } from '@angular/material/icon';
import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-consent',
    templateUrl: './consent.component.html',
    styleUrls: ['./consent.component.scss'],
    imports: [FlexModule, MatIcon, MatButton, TranslateModule]
})
export class ConsentComponent {
  state = inject(AppState);


  close(): void {
    localStorage.setItem('consent', 'true');
  }

}
