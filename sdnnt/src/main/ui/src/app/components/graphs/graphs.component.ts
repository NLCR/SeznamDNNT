import { HttpParams } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { ApexChart } from 'ng-apexcharts';
import { timeInterval } from 'rxjs/operators';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-graphs',
  templateUrl: './graphs.component.html',
  styleUrls: ['./graphs.component.scss']
})
export class GraphsComponent implements OnInit {

  facets: any;
  stavOpts: {
    series: number[],
    chart: ApexChart,
    title: {},
    labels: string[]
  }

  licenseOpts: {
    series: number[],
    chart: ApexChart,
    title: {},
    labels: string[]
  }

  historyStavOpts: {
    series: any[],
    chart: ApexChart,
    title: {},
    labels: string[],
    xaxis: {}
  }

  historyUserOpts: {
    series: any[],
    chart: ApexChart,
    title: {},
    labels: string[],
    xaxis: {}
  }

  historyUserActivityOpts: {
    series: any[],
    chart: ApexChart,
    title: {},
    labels: string[],
    xaxis: {}
  }

  interval: string = '1MONTH';


  constructor(private service: AppService) { }

  ngOnInit(): void {
    this.getFacets();
    this.service.langChanged.subscribe(res => {
      this.getFacets();
    })
  }

  setSeries() {
    this.stavOpts =
    {
      series: this.facets.dntstav.map(e => e.value),
      chart: {
        toolbar: {
          show: false
        },
        height: 350,
        type: "donut"
      },
      title: {
        text: this.service.getTranslation("graphs.States")
      },
      labels: this.facets.dntstav.map(e => e.name + ' ' + this.service.getTranslation('state.' + e.name))
    }

    this.licenseOpts =
    {
      series: this.facets.license.map(e => e.value),
      chart: {
        toolbar: {
          show: false
        },
        height: 350,
        type: "donut"
      },
      title: {
        text: this.service.getTranslation("graphs.Licenses")
      },
      labels: this.facets.license.map(e => this.service.getTranslation('license.' + e.name))
    }
    this.getStatsHistory();

  }

  getFacets() {
    const p = Object.assign({}, {});
    this.service.search(p as HttpParams).subscribe((res)=>{
      this.facets = res.facet_counts.facet_fields;
      this.setSeries();
    });
  }

  getStatsHistory() {
    this.service.getStatsHistory(this.interval).subscribe(res => {

      const stats = res.facet_counts.facet_pivot.type;
      this.historyStavOpts = this.setHistoryStats(stats, 'History by type');
      this.historyUserOpts = this.setHistoryStats(res.facet_counts.facet_pivot.user, 'History by user');
      this.historyUserActivityOpts = this.setUserActivity(res.facet_counts.facet_fields.user.filter(e => e.name !== 'harvester'), "User activity");
      console.log(this.historyUserActivityOpts)
    });
  }

  setHistoryStats(stats, title): any {
    const series = [];

    stats.forEach(t => {
      if (t.value !== 'app' && t.value !== 'harvester') {
        const s = t.ranges.indextime.counts.map(e => { return { x: new Date(e.name).getTime(), y: e.value } });
        series.push({ data: s, name: t.value })
      }
    });
    const opts: any = {};
    opts.series = series;
    opts.chart = {
      toolbar: {
        show: false
      },
      height: 350,
      type: "line"
    };
    opts.title = {
      text: this.service.getTranslation('graphs.' + title)
    };
    opts.xaxis = {
      type: 'datetime'
    }
    return opts;
  }

  setUserActivity(stats, title): any {

    // const data= stats.map(e => { return { x: e.name, y: e.value } });
    const data = stats.map(e => e.value);
    const categories = stats.map(e => e.name);

    const opts: any = {};
    opts.series = [{ name: title, data }];
    opts.chart = {
      toolbar: {
        show: false
      },
      height: 350,
      type: "bar"
    };
    opts.title = {
      text: this.service.getTranslation('graphs.' + title)  
    };
    opts.xaxis = {
      categories: categories
    }
    return opts;
  }

}
