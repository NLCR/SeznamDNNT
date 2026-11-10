import { CdkScrollable } from '@angular/cdk/scrolling';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogClose, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { MatRadioButton, MatRadioGroup } from '@angular/material/radio';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { Zadost } from 'src/app/shared/zadost';

import { MatButton } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'app-dialog-bulk-proposal',
    templateUrl: './dialog-bulk-proposal.component.html',
    styleUrls: ['./dialog-bulk-proposal.component.scss'],
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatRadioGroup, FormsModule, MatRadioButton, MatDialogActions, MatButton, MatDialogClose, TranslateModule]
})
export class DialogBulkProposalComponent {
  dialogRef = inject<MatDialogRef<DialogBulkProposalComponent>>(MatDialogRef);
  private service = inject(AppService);
  private config = inject(AppConfiguration);
  state = inject(AppState);
  data = inject(MAT_DIALOG_DATA);


  navrhNaZarazeni = false;
  navrhNaVyrazeni = false;
  navrhNaOmezeni = false;
  // all actions
  actions: string[] = [];
  // ident x actions mapping
  identifiersAndActionsMapping = new Map<string, string[]>();
  navrh: 'VN' | 'VNZ' | 'NZN' | null = null;

  constructor() {
    const data = this.data;

    if (data.VN) {
      this.navrhNaVyrazeni = true;
      this.navrh = 'VN';
    }
    if (data.VNL || data.VNZ) {
      this.navrhNaOmezeni = true;
      this.navrh = 'VNZ';
    }
    if (data.NZN) {
      this.navrhNaZarazeni = true;
      this.navrh = 'NZN';
    }


    if (data.actions) {
      this.actions = data.actions;
    }
    if (data.identifiersAndActionsMapping) {
      this.identifiersAndActionsMapping = data.identifiersAndActionsMapping;
    }
  }



  /** Pripravi a ulozi zadost po te reload */
  saveZadost(navrh: 'VN' | 'VNZ' | 'NZN', identifiers: string[], customSuccess: string | null, customError: string | null, forceError: boolean=false) {
    const key = navrh === 'VNZ' ? 'VNX' : navrh;
    const request = this.state.currentZadost[key];
    if (!request) {
      return;
    }
    if (!request.identifiers) {
      request.identifiers = [];
    }
    identifiers.forEach(ident=> {
      request.identifiers.push(ident);
    });

    this.service.saveZadost(request).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', customError ? customError : res.error , true);
      } else {
        if (forceError) {
          // ulozeno ale stejne error
          this.service.showSnackBar('alert.ulozeni_zadosti_error', customError ? customError : res.error , true);
        } else {
          this.service.showSnackBar('alert.ulozeni_zadosti_success', customSuccess ? customSuccess : '', false);
        }
      }
    });
  }

    /**
     * Hromadna zadost
     */
     setBulkProposal() {
      const identiefiers: string[] = [];
      const selectedAction = this.navrh;
      if (!selectedAction) {
        return;
      }
      this.identifiersAndActionsMapping.forEach((actions: string[], ident: string) => {
          if(actions.indexOf(selectedAction) > -1 ) {
            identiefiers.push(ident);
          }
      });
      if (identiefiers && identiefiers.length > 0) {

        if (selectedAction === 'VNZ') {
          this.service.prepareZadost(['VNZ','VNL']).subscribe((res: Zadost) => {
            const resNumberOfItems: number = res.identifiers?.length ?? 0;
            if ((identiefiers.length+ resNumberOfItems) <= this.config.maximumItemInRequest) {
              this.state.currentZadost.VNX= res;
              this.saveZadost(selectedAction, identiefiers, null, null);
            }  else {
              // remove identifiers from zadost
              this.state.currentZadost.VNX= res;
              const diff: number = this.config.maximumItemInRequest - resNumberOfItems;
              this.saveZadost(selectedAction, identiefiers.slice(0, diff), null, 'alert.maximalni_pocet_polozek_v_zadosti_prekrocen', true);
            }
            this.dialogRef.close();
          });
        } else {
          this.service.prepareZadost([selectedAction]).subscribe((res: Zadost) => {
            const resNumberOfItems: number = res.identifiers?.length ?? 0;
            if ((identiefiers.length+ resNumberOfItems) <= this.config.maximumItemInRequest) {
              this.state.currentZadost[selectedAction]= res;
              this.saveZadost(selectedAction, identiefiers,null, null);
            } else {
              const diff: number = this.config.maximumItemInRequest - resNumberOfItems;
              this.state.currentZadost[selectedAction]= res;
              this.saveZadost(selectedAction, identiefiers.slice(0, diff),null, 'alert.maximalni_pocet_polozek_v_zadosti_prekrocen', true);
              //this.service.showSnackBar('alert.ulozeni_zadosti_error', 'alert.maximalni_pocet_polozek_v_zadosti_prekrocen',  true);
            }
            this.dialogRef.close();
          });
        }
      }

    }

}
