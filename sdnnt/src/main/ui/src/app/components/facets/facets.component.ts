import { Component, Input, OnInit, HostListener } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { Filter } from 'src/app/shared/filter';
import { DialogAllPublishersComponent } from '../dialog-all-publishers/dialog-all-publishers.component';
import { HttpParams } from '@angular/common/http';
import { AppService } from 'src/app/app.service';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-facets',
  templateUrl: './facets.component.html',
  styleUrls: ['./facets.component.scss']
})
export class FacetsComponent implements OnInit {


  public getScreenWidth: any;
  public facetBreakpoint: number = 1040; 

  @Input() facet_fields: {[field: string]: {name: string, type: string, value: number}[]};
  @Input() stats:{ [field: string]: {min: any, max: any, count: number, from: any, until: any}};

  @Input() view:string="search";


  queryParams:any;


  facets: string[];

  rokoddate = new FormControl(new Date());
  rokod: number;
  rokdodate = new FormControl(new Date());
  rokdo: number;

  showRoky: boolean;

  selectedRadioButton:string = 'in_list';

  facetSearchOffset=0;
  facetFieldsInDialog:{};

  rokvydaniParam:string;


  constructor(
    private router: Router,
    private route : ActivatedRoute,
    public config: AppConfiguration,
    public state: AppState,
    public dialog: MatDialog,
    private service: AppService
  ) { 


    this.route.queryParams.subscribe((params) => {
      const catalogParam = params['catalog'];
      this.selectedRadioButton = catalogParam || 'in_list';
      this.queryParams = params;
      this.rokvydaniParam = params['rokvydani'];
    });

    

  }

  ngOnInit(): void {
    // get breakpoint
    this.getScreenWidth = window.innerWidth;

    this.facets = [];
    // filter kurator stav for users 
    this.config.filterFields.forEach(f => {
      let flag:boolean = true;
      if (f && f ==="kuratorstav") {
        flag = this.state.user && (this.state.user.role === 'kurator' || this.state.user.role === 'mainKurator' || this.state.user.role === 'admin');        
      } else  if (f && f ==="c_actions") {
        flag = this.state.user && (this.state.user.role === 'kurator' || this.state.user.role === 'mainKurator' || this.state.user.role === 'admin');        
      }
      if (flag) {
        if (this.facet_fields[f]) {
          this.facets.push(f);
        }
      }      
    });

    if (this.stats) {
      // TODO: zmenit      
      const year = (new Date()).getFullYear();
      // this.rokod = (this.stats['rokvydani'].min ? this.stats['rokvydani'].min : 1915);
      // this.rokdo = Math.min(year, (this.stats['rokvydani'].max ? this.stats['rokvydani'].max : 2008));

      if (this.rokvydaniParam) {

        let [start, end]: number[] = this.rokvydaniParam.split(',').map(Number);
        this.rokod = start;
        this.rokdo = end;

        this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
        this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
        this.showRoky = true;

      } else {
        this.rokod = (this.stats['date1_int'].min ? this.stats['date1_int'].min : 1915);
        this.rokdo = Math.min(year, (this.stats['date1_int'].max ? this.stats['date1_int'].max : 2008));
  
  
        this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
        this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
        this.showRoky = true;
  
      }
    }
  }

  isYearsButtonDisabled(): boolean {
    return this.rokod > this.rokdo;
  }



  onRadioButtonChange() {
      const q: any = {};
      q['catalog'] =  this.selectedRadioButton;
      q.page = null;
      this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }
  

  addFilter(field: string, f:{name: string, type: string, value: number}) {
    const q: any = {};
    q[field] = f.name;
    q.page = null;
    this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
  }

  chosenYearHandler(normalizedYear: Date, datepicker: any, field: string) {
    console.log(normalizedYear)
    if (field === 'from') {
      this.rokod = normalizedYear.getFullYear();
    } else {
      this.rokdo = normalizedYear.getFullYear();
    }

    this.rokoddate.setValue(new Date(this.rokoddate.value.setFullYear(this.rokod)));
    this.rokdodate.setValue(new Date(this.rokdodate.value.setFullYear(this.rokdo)));
    datepicker.close();
  }

  clickRokFacet() {
    const params: any = {};
    params['rokvydani'] = this.rokod + ',' + this.rokdo;
    params.page = 0;
    this.router.navigate([], { queryParams: params, queryParamsHandling: 'merge' });
  }

  // get breakpoint
  @HostListener('window:resize', ['$event'])
  onWindowResize() {
    this.getScreenWidth = window.innerWidth;
  }

  getPanelExpansion() {
    if (this.getScreenWidth <= this.facetBreakpoint) {
      return false;
    } else {
      return true;
    }
  }


  usedFacet(facet) {
    let retval = this.state.usedFilters.filter(filter => filter.field === facet);
    return retval;
  }


  showAllPublishers() {
    const data = {};
    data['queryParams'] =   this.queryParams;


   const dialogRef = this.dialog.open(DialogAllPublishersComponent, {
     width: '750px',
     data:data,
     panelClass: 'app-dialog-all-publishers' 
   });

   dialogRef.afterClosed().subscribe(result => {

    if (result.selectedFacets) {

      const q: any = {};
  
      result.selectedFacets.forEach(facet => {
        if (!q['nakladatel']) {
          q['nakladatel'] = [];
        }
        q['nakladatel'].push(facet.name);
      });
      q.page = null;

      this.router.navigate([], { queryParams: q, queryParamsHandling: 'merge' });
    }

  });
 

  }

}
