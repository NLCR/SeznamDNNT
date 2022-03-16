import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { NavbarComponent } from '../navbar/navbar.component';
import { MatDialog } from '@angular/material/dialog';
import { SUPER_EXPR } from '@angular/compiler/src/output/output_ast';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DialogNotificationsSettingsComponent } from '../dialog-notifications-settings/dialog-notifications-settings.component';

@Component({
  selector: 'app-sidenav-list',
  templateUrl: './sidenav-list.component.html',
  styleUrls: ['./sidenav-list.component.scss']
})
export class SidenavListComponent extends NavbarComponent implements OnInit {
  @Output() sidenavClose = new EventEmitter();

  public onSidenavClose = () => {
    this.sidenavClose.emit();
  }

  // sidenav
  @Output() public sidenavToggle = new EventEmitter();

  constructor(
    dialog: MatDialog,
    private dialogNotification: MatDialog,
    router: Router,
    service: AppService,
    public state: AppState,
    public config: AppConfiguration
    ) { 
      super(dialog,router,service,state,config);
    }

  ngOnInit(): void {
  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }

  openNotificationsSettings() {
    const dialogRef = this.dialogNotification.open(DialogNotificationsSettingsComponent, {
      width: '600px',
      panelClass: 'app-dialog-notifications-settings'
    });
  }

}
