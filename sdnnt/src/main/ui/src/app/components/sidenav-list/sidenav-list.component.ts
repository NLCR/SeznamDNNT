import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { NavbarComponent } from '../navbar/navbar.component';
import { MatDialog } from '@angular/material/dialog';
import { SUPER_EXPR } from '@angular/compiler/src/output/output_ast';
import { Router } from '@angular/router';

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
    router: Router,
    service: AppService,
    public state: AppState
    ) { 
      super(dialog,router,service,state);
    }

  ngOnInit(): void {
  }

  // sidenav fuction
  public onToggleSidenav = () => {
    this.sidenavToggle.emit();
  }

}
