import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { Zadost } from 'src/app/shared/zadost';

@Component({
  selector: 'app-dialog-bulk-proposal',
  templateUrl: './dialog-bulk-proposal.component.html',
  styleUrls: ['./dialog-bulk-proposal.component.scss']
})
export class DialogBulkProposalComponent implements OnInit {

  navrhNaZarazeni: boolean = false;
  navrhNaVyrazeni: boolean = false;
  navrhNaOmezeni: boolean = false;
  // all actions
  actions: string[] = [];
  // ident x actions mapping
  identifiersAndActionsMapping = new Map<string, string[]>(); 
  navrh:string;

  docs: SolrDocument[];

  constructor(
    public dialogRef: MatDialogRef<DialogBulkProposalComponent>,
    private route: ActivatedRoute,
    private router: Router,
    private service: AppService,
    private config: AppConfiguration,
    public state: AppState,
    @Inject(MAT_DIALOG_DATA) public data:  any) {
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


    if (data.docs) {
      this.docs = data.docs;
    }
    if (data.actions) {
      this.actions = data.actions;
    }
    if (data.identifiersAndActionsMapping) {
      this.identifiersAndActionsMapping = data.identifiersAndActionsMapping;
    }
  }

  ngOnInit(): void {
  }


  /** Pripravi a ulozi zadost po te reload */
  saveZadost(navrh: string, identifiers: string[], customSuccess:string, customError:string, forceError: boolean=false) {
    const key =  navrh === 'VNL' || navrh === 'VNZ' ?  'VNX' : navrh;
    if  (!this.state.currentZadost[key].identifiers) {
      this.state.currentZadost[key].identifiers = [];
    }    
    identifiers.forEach(ident=> {
      this.state.currentZadost[key].identifiers.push(ident);
    });

    this.service.saveZadost(this.state.currentZadost[key]).subscribe((res: any) => {
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
      let identiefiers = [];
      let selectedAction = this.navrh;
      this.identifiersAndActionsMapping.forEach((actions: string[], ident: string) => {
          if(actions.indexOf(selectedAction) > -1 ) {
            identiefiers.push(ident);
          }
      });
      if (identiefiers && identiefiers.length > 0) {
        
        if (selectedAction === 'VNZ' || selectedAction === 'VNL') {
          this.service.prepareZadost(['VNZ','VNL']).subscribe((res: Zadost) => {
            let resNumberOfItems:number = res.identifiers?.length | 0;
            if ((identiefiers.length+ resNumberOfItems) < this.config.maximumItemInRequest) {
              this.state.currentZadost['VNX']= res;
              this.saveZadost(selectedAction, identiefiers, null, null);
            }  else {
              // remove identifiers from zadost 
              this.state.currentZadost['VNX']= res;
              let diff:number = this.config.maximumItemInRequest - resNumberOfItems;
              this.saveZadost(selectedAction, identiefiers.slice(0, diff), null, 'alert.maximalni_pocet_polozek_v_zadosti_prekrocen', true);
            }
            this.dialogRef.close();
          });
        } else {
          this.service.prepareZadost([selectedAction]).subscribe((res: Zadost) => {
            let resNumberOfItems:number = res.identifiers?.length | 0;
            if ((identiefiers.length+ resNumberOfItems) < this.config.maximumItemInRequest) {
              this.state.currentZadost[selectedAction]= res;
              this.saveZadost(selectedAction, identiefiers,null, null);
            } else {
              let diff:number = this.config.maximumItemInRequest - resNumberOfItems;
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
