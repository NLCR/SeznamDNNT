import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';
import { Zadost } from 'src/app/shared/zadost';
import { DialogIdentifierComponent } from '../dialog-identifier/dialog-identifier.component';
import { ExpressionDialogComponent } from '../expression-dialog/expression-dialog.component';
import { GranularityComponent } from '../granularity/granularity.component';
import { DialogHistoryComponent } from '../dialog-history/dialog-history.component';
import { DialogPromptComponent } from '../dialog-prompt/dialog-prompt.component';
import { DialogStatesComponent } from '../dialog-states/dialog-states.component';
import { HttpErrorResponse } from '@angular/common/http';
import { zip } from 'rxjs';

@Component({
  selector: 'app-result-item',
  templateUrl: './result-item.component.html',
  styleUrls: ['./result-item.component.scss']
})
export class ResultItemComponent implements OnInit {

  @Input() doc: SolrDocument;
  @Input() inZadost: boolean;
  @Input() zadost: Zadost;
  @Output() removeFromZadostEvent = new EventEmitter<string>();
  @Output() processZadostEvent = new EventEmitter<{ type: string, identifier: string, komentar: string }>();

  newState = new FormControl();
  isZarazeno: boolean;
  hasNavhr: boolean;
  hasGranularity: boolean;
  imgSrc: string;
  processed: { date: Date, state: string, user: string, reason?: string };
  processedTooltip: string;
  alephLink: string;

  dkLinks: string[] = [];

  constructor(
    private datePipe: DatePipe,
    public dialog: MatDialog,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {

    if (this.doc.marc_998a) {
      this.alephLink = this.doc.marc_998a[0];
      if (!this.alephLink.startsWith('http')) {
        this.alephLink = 'https://aleph.nkp.cz/F/?func=direct&local_base=DNT&doc_number=' + this.doc.marc_998a[0].split('-')[1];
      }
    }
    this.newState.setValue(this.doc.dntstav);
    this.isZarazeno = this.doc.dntstav?.includes('A') || this.doc.dntstav?.includes('PA') || this.doc.dntstav?.includes('NL');

    const z = this.inZadost ? this.zadost : this.doc.zadost;
    if (z?.process) {
      //let tablekey = this.doc.identifier;
      let stateKey = (z.desired_item_state ? z.desired_item_state : "_");
      let licenseKey = (z.desired_license ? z.desired_license : "_");

      let fullTableKey = this.doc.identifier + "_(" + stateKey + "," + licenseKey + ")";
      this.processed = z.process[fullTableKey] ? z.process[fullTableKey] : z.process[this.doc.identifier];
      if (this.processed) {
        this.processedTooltip = `${this.service.getTranslation('desc.uzivatel')} : ${this.processed.user}
          ${this.service.getTranslation('desc.datum')}: ${this.datePipe.transform(this.processed.date, 'dd.MM.yyyy')}`;

        if (this.processed.reason) {
          if (this.processed.state === "rejected") {
            this.processedTooltip += `
            ${this.service.getTranslation('desc.duvod')}: ${this.processed.reason}`

          } else {
            this.processedTooltip += `
            ${this.service.getTranslation('desc.komentar')}: ${this.processed.reason}`
          }
        }
      }
    }
    this.hasNavhr = !!this.doc.zadost && !this.processed;
    this.hasGranularity = this.doc.granularity && this.doc.granularity.length > 1;
    this.dkLinks = [];
    const tags = ['marc_956u', 'marc_911u', 'marc_856u'];
    tags.forEach(t => {
      if (this.doc[t]) {
        this.doc[t].forEach(l => {
          if (!this.dkLinks.includes(l)) {
            this.dkLinks.push(l);
          }
        });
      }
    });

    // this.dkLinks = this.dkLinks.concat(this.doc.marc_911u ? this.doc.marc_911u : []);
    // this.dkLinks = this.dkLinks.concat(this.doc.marc_856u ? this.doc.marc_856u : []);
    if (this.doc.marc_956u) {
      // Je to kramerius
      const link: string = this.doc.marc_956u[0];

      // http://krameriusndk.nkp.cz/search/handle/uuid:960bc370-c6c0-11e2-b6da-005056827e52 
      if (link.indexOf('handle') > -1 && link.indexOf('uuid') > -1) {
        this.imgSrc = link.replace('/handle/', '/api/v5.0/item/') + '/thumb';
      }

    } else if (this.doc.marc_911u) {
      // Je to kramerius
      const link: string = this.doc.marc_911u[0];

      // http://krameriusndk.nkp.cz/search/handle/uuid:960bc370-c6c0-11e2-b6da-005056827e52 
      if (link.indexOf('handle') > -1 && link.indexOf('uuid') > -1) {
        this.imgSrc = link.replace('/handle/', '/api/v5.0/item/') + '/thumb';
      }

    } else if (this.doc.marc_856u) {

      if (this.doc.marc_856u[0].indexOf('books.google') > 0) {
        // google books
        const link: string = this.doc.marc_856u[0];
        const id = link.substring(link.indexOf('vid=') + 4, link.indexOf('&'));
        this.service.findGoogleBook(id).subscribe((res: any) => {
          if (res[id]) {
            this.imgSrc = res[id].thumbnail_url;
          }
          // this.imgSrc = res.items[0].volumeInfo.imageLinks.smallThumbnail;
        });
      } else {
        // this.imgSrc = this.doc.marc_856u;
      }

    }
  }

  curatorAndPublicStateAreDifferent(): boolean {
    // neni nastaveny public stav ale ma kuratorsky stav NPA 
    if (this.doc.kuratorstav && !this.doc.dntstav) {
      return true;
      // verejny a kuratorsky stav je rozdilny
    } else if (this.doc.kuratorstav && this.doc.dntstav && this.doc.kuratorstav[this.doc.kuratorstav.length - 1] != this.doc.dntstav[this.doc.dntstav.length - 1]) {
      return true;
    }
    return false;
  }

  showIdentifiers() {
    const data = {
      title: this.doc.title,
      items: [],
    }
    data.items.push({ label: 'Aleph identifier', value: this.doc['identifier'] })

    this.config.identifiers.forEach(f => {
      if (this.doc['marc_' + f]) {
        data.items.push({ label: 'field.' + f, value: this.doc['marc_' + f] })
      }
    });


    const dialogRef = this.dialog.open(DialogIdentifierComponent, {
      width: '750px',
      data,
      panelClass: 'app-dialog-identifier'
    });
  }

  showHistory() {
    const dialogRef = this.dialog.open(DialogHistoryComponent, {
      width: '750px',
      data: this.doc,
      panelClass: 'app-history-identifier'
    });


    // dialogRef.afterClosed().subscribe(result => {
    //   console.log('The dialog was closed', result);
    // });
  }

  changeStav() { }

  public showGranularity() {

    const data = { title: this.doc.nazev, items: this.doc.granularity };

    const dialogRef = this.dialog.open(GranularityComponent, {
      width: '1150px',
      data: data,
      panelClass: 'app-dialog-states'
    });
  }

  public alreadyRejected() {
    //let tablekey = this.doc.identifier;
    // posledni zmena je v identifikatoru
    if (this.zadost.process && this.zadost.process[this.doc.identifier]) {
      return this.zadost.process[this.doc.identifier].state === 'rejected';
    } else return false;
  }

  public showStates() {
    const dialogRef = this.dialog.open(DialogStatesComponent, {
      width: '1150px',
      data: this.doc,
      panelClass: 'app-dialog-states'
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.change) {
        this.service.changeStavDirect(this.doc.identifier, result.newState, result.newLicense, result.poznamka, result.granularity).subscribe(res => {

          if (res.response.docs.length > 0) {
            this.doc = res.response.docs[0];
          }
        });
      }

    });
  }

  goto(url) {
    window.open(url, "_blank", 'noreferrer');
    return;
  }

  addToZadostForReduction() {
    // prepare VNX
    //const navrh = this.isZarazeno ? 'VN' : 'NZN';
    this.service.prepareZadost(['VNZ', 'VNL']).subscribe((res: Zadost) => {
      this.state.currentZadost['VNX'] = res;
      this.addToZadostInternal(res.navrh);
    });
  }

  addToZadost() {

    // if (this.hasGranularity) {
    //   const data = { title: this.doc.nazev, items: this.doc.granularity, isNavrh: true };

    //   const dialogRef = this.dialog.open(GranularityComponent, {
    //     width: '1150px',
    //     data: data,
    //     panelClass: 'app-dialog-states'
    //   });
    //   dialogRef.afterClosed().subscribe(result => {
    //     if (result) {
          
    //       console.log(result);
    //       // const navrh = this.isZarazeno ? ['VN'] : ['NZN'];
    //       // this.service.prepareZadost(navrh).subscribe((res: Zadost) => {
    //       //   this.state.currentZadost[res.navrh] = res;
    //       //   this.addToZadostInternal(res.navrh);
    //       // });
    //     }
    //   });
    // } else {
    //   const navrh = this.isZarazeno ? ['VN'] : ['NZN'];
    //   this.service.prepareZadost(navrh).subscribe((res: Zadost) => {
    //     this.state.currentZadost[res.navrh] = res;
    //     this.addToZadostInternal(res.navrh);
    //   });
    // }

    const navrh = this.isZarazeno ? ['VN'] : ['NZN'];
    this.service.prepareZadost(navrh).subscribe((res: Zadost) => {
      this.state.currentZadost[res.navrh] = res;
      this.addToZadostInternal(res.navrh);
    });
  }


  addToZadostInternal(navrh: string) {
    let onlyRecord = true;
    this.service.getExpression(this.doc.frbr).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('', res.error, true);
      } else {
        if (res.response.numFound > 1) {
          // docasne vypnuto 
          // Je vice zaznamu pro toto vyjadreni. Zeptame se
          /*
          const dialogRef = this.dialog.open(ExpressionDialogComponent, {
            width: '750px',
            data: res.response,
            panelClass: 'app-dialog-identifier'
          });

          dialogRef.afterClosed().subscribe(result => {
            console.log(result);
            if (result && result !== '') {
              onlyRecord = result === 'onlyRecord';
              if (onlyRecord) {
                this.saveZadost(navrh);
              } else {
                this.addFRBRToZadost(navrh);
              }
            }
          });*/
          this.saveZadost(navrh);

        } else {
          this.saveZadost(navrh);
        }
      }
    });
  }


  saveZadost(navrh: string) {
    const key = navrh === 'VNL' || navrh === 'VNZ' ? 'VNX' : navrh;
    if (!this.state.currentZadost[key].identifiers) {
      this.state.currentZadost[key].identifiers = [];
    }
    // check maxium
    if (this.state.currentZadost[key].identifiers && this.state.currentZadost[key].identifiers.length >= this.config.maximumItemInRequest) {
      this.service.showSnackBar('alert.maximalni_pocet_polozek_v_zadosti_prekrocen', '', false);
    } else {
      if (!this.state.currentZadost[key].identifiers.includes(this.doc.identifier)) {
        this.state.currentZadost[key].identifiers.push(this.doc.identifier);
      }
      this.service.saveZadost(this.state.currentZadost[key]).subscribe((res: any) => {
        if (res.error) {
          this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
        } else {
          this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
          if (!this.doc.zadost) {
            this.doc.zadost = this.state.currentZadost[key];
            // TODO: ??  
            this.hasNavhr = !!this.doc.zadost && !this.processed;
          } else if (this.doc.zadost.identifiers) {
            this.doc.zadost.identifiers.push(this.doc.identifier);;
            // TODO: ??  
            this.hasNavhr = !!this.doc.zadost && !this.processed;
          }
        }
      });

    }
  }

  addFRBRToZadost(navrh: string) {
    this.service.addFRBRToZadost(this.state.currentZadost[navrh], this.doc.frbr).subscribe((res: any) => {
      if (res.error) {
        this.service.showSnackBar('alert.ulozeni_zadosti_error', res.error, true);
      } else {
        this.service.showSnackBar('alert.ulozeni_zadosti_success', '', false);
      }
    });
  }

  removeFromZadost() {
    this.removeFromZadostEvent.emit(this.doc.identifier);
  }

  approve() {

    const approveDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: { caption: 'komentar', label: 'komentar' },
      panelClass: 'app-register-dialog'
    });

    approveDialogRef.afterClosed().subscribe(result => {
      if (result !== null) {
        this.processZadostEvent.emit({ type: 'approve', identifier: this.doc.identifier, komentar: result });
      }
    });

    // this.processZadostEvent.emit({type: 'change', identifier: this.doc.identifier});
  }

  approveLib() {


    const approvedLibDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: { caption: 'komentar', label: 'komentar' },
      panelClass: 'app-register-dialog'
    });

    approvedLibDialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.processZadostEvent.emit({ type: 'approveLib', identifier: this.doc.identifier, komentar: result });
      }
    });
  }


  releasedProved() {

    const releasedDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: { caption: 'komentar', label: 'komentar' },
      panelClass: 'app-register-dialog'
    });

    releasedDialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.processZadostEvent.emit({ type: 'releasedProved', identifier: this.doc.identifier, komentar: result });
      }
    });

  }

  reject() {
    const rejectDialogRef = this.dialog.open(DialogPromptComponent, {
      width: '700px',
      data: { caption: 'duvod_pro_odmitnuti', label: 'duvod' },
      panelClass: 'app-register-dialog'
    });

    rejectDialogRef.afterClosed().subscribe(result => {
      //if (result) {
      this.processZadostEvent.emit({ type: 'reject', identifier: this.doc.identifier, komentar: result });
      //}
    });

  }

  setFollow(follow: boolean) {
    this.service.followRecord(this.doc.identifier, follow).subscribe((res: any) => {
      if (res.error) {
        if (follow === true) {
          this.service.showSnackBar('alert.sledovat_zaznam_error', res.error, true);
        } else {
          this.service.showSnackBar('alert.odebrani_sledovaneho_zaznamu_error', res.error, true);
        }
      } else {
        if (follow === true) {
          this.service.showSnackBar('alert.sledovat_zaznam_success', '', false);
        } else {
          this.service.showSnackBar('alert.odebrani_sledovaneho_zaznamu_success', '', false);
        }
        this.doc.hasNotifications = follow;
      }
    });
  }


  //(zadost.navrh === 'VNL' && doc.kuratorstav && doc.kuratorstav[doc.kuratorstav.length-1] ==='NLX')

  curratorInteractionNeedAfterProcessed(zadost: Zadost, doc: SolrDocument): boolean {
    return (zadost && zadost.navrh && zadost.navrh === 'VNL' && doc.kuratorstav && doc.kuratorstav[doc.kuratorstav.length - 1] === 'NLX');
  }

  notPublic(doc: SolrDocument) {
    return doc.dntstav == null || doc.dntstav[doc.dntstav.length - 1] !== 'X';
  }
}

