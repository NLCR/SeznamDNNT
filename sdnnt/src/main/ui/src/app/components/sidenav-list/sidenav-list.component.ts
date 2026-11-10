import { Component, EventEmitter, inject, Output } from '@angular/core';
import { FlexModule } from '@angular/flex-layout/flex';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatDivider, MatListItem, MatNavList } from '@angular/material/list';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { DialogNotificationsSettingsComponent } from '../dialog-notifications-settings/dialog-notifications-settings.component';
import { NavbarComponent } from '../navbar/navbar.component';

import { MatAccordion, MatExpansionPanel, MatExpansionPanelHeader, MatExpansionPanelTitle } from '@angular/material/expansion';
import { MatMenuItem } from '@angular/material/menu';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-sidenav-list',
    templateUrl: './sidenav-list.component.html',
    styleUrls: ['./sidenav-list.component.scss'],
    imports: [
    MatNavList,
    MatListItem,
    RouterLink,
    FlexModule,
    RouterLinkActive,
    MatIcon,
    MatDivider,
    MatAccordion,
    MatExpansionPanel,
    MatExpansionPanelHeader,
    MatExpansionPanelTitle,
    MatMenuItem,
    TranslateModule
]
})
export class SidenavListComponent extends NavbarComponent {
  private dialogNotification = inject(MatDialog);

  @Output() sidenavClose = new EventEmitter();

  public onSidenavClose = () => {
    this.sidenavClose.emit();
  };

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();


  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  };

  openNotificationsSettings() {
    const dialogRef = this.dialogNotification.open(DialogNotificationsSettingsComponent, {
      width: '600px',
      panelClass: 'app-dialog-notifications-settings'
    });
  }

}
