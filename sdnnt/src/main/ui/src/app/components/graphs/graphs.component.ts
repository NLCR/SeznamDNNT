import { DatePipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { EChartsOption } from 'echarts';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-graphs',
  templateUrl: './graphs.component.html',
  styleUrls: ['./graphs.component.scss']
})
export class GraphsComponent implements OnInit {

  facets: any;
  stavOpts: EChartsOption = {};
  licenseOpts: EChartsOption = {};
  historyStavOpts: EChartsOption = {};
  historyUserOpts: EChartsOption = {};
  historyUserActivityOpts: EChartsOption = {};

  interval: string = '1MONTH';


  constructor(
    private datePipe: DatePipe,
    private service: AppService) { }

  ngOnInit(): void {
    this.getFacets();
    this.service.langChanged.subscribe(res => {
      this.getFacets();
    })
  }

  setSeries() {
    this.stavOpts = {
      title: {
        text: this.service.getTranslation("graph.title.States")
      },
      tooltip: {
        trigger: 'item',
        position: [10, 25],
        valueFormatter: (value) => '' + value.toLocaleString('cs')
      },
      series: {
        name: this.service.getTranslation("graph.title.States"),
        type: 'pie',
        radius: ['40%', '70%'],
        // center: ['40%', '50%'],
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold'
          }
        },
        data: this.facets.dntstav.map(e => {
          return {
            value: e.value,
            name: e.name + ' ' + this.service.getTranslation('state.' + e.name),
            name1: e.name
          }
        }),
        itemStyle: {
          color: (params) => {
            const paramsExt: any = params;
            if (paramsExt?.data?.name1 === 'A') {
              return '#3949ab';
            } else if (paramsExt?.data?.name1 === 'NZ') {
              return '#546e7a'
            } else if (paramsExt?.data?.name1 === 'N') {
              return '#43a047'
            } else if (paramsExt?.data?.name1 === 'PA') {
              return '#8e24aa'
            } else if (paramsExt?.data?.name1 === 'X') {
              return '#757575'
            } else if (paramsExt?.data?.name1 === 'NL') {
              return '#039BE5'
            } else {
              return params.color
            }

          }
        },
      },
      legend: {
        // type: 'scroll',
        // orient: 'vertical',
        // right: 10,
        // top: 20,
        bottom: 0,
      }
    };

    this.licenseOpts =  {
      title: {
        text: this.service.getTranslation("graph.title.Licenses")
      },
      tooltip: {
        trigger: 'item',
        valueFormatter: (value) => '' + value.toLocaleString('cs')
      },
      series: {
        name: this.service.getTranslation("graph.title.Licenses"),
        type: 'pie',
        radius: ['40%', '70%'],

        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold'
          }
        },
        data: this.facets.license.map(e => {
          return {
            value: e.value,
            name: e.name + ' ' + this.service.getTranslation('license.' + e.name),
            name1: e.name
          }
        }),
        itemStyle: {
          color: (params) => {
            const paramsExt: any = params;
            if (paramsExt?.data?.name1 === 'dnnto') {
              return '#5e35b1';
            } else if (paramsExt?.data?.name1 === 'dnntt') {
              return '#f4511e'
            } else {
              return params.color
            }

          }
        },
      },
      legend: {
        right: 0,
        orient: 'vertical'
      },
    };

    this.getStatsHistory();

  }

  getFacets() {
    const p = Object.assign({}, {});
    this.service.search(p as HttpParams).subscribe((res) => {
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
    });
  }

  setHistoryStats(stats, title): any {
    const series = [];
    const legend = [];

    stats.forEach(t => {
      if (t.value !== 'app' && t.value !== 'harvester') {
        // const s = t.ranges.indextime.counts.map(e => { return { x: new Date(e.name).getTime(), y: e.value } });
        series.push({ 
          data: t.ranges.indextime.counts.map(e => e.value ), 
          type: 'line',
          name: t.value 
        });
        legend.push(t.value);
      }
    });
    const opts: any = {
      title: {
        text: this.service.getTranslation('graph.title.' + title)
      },
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value) => '' + value.toLocaleString('cs')
      },
      xAxis: {
        type: 'category',
        data: stats[0].ranges.indextime.counts.map(e => this.datePipe.transform(new Date(e.name), 'MM.yyyy')),
      },
      yAxis: {
        type: 'value',
      },
      series: series,
      legend: {
        data: legend,
        bottom: 0,
      }
    };
    return opts;
  }

  setUserActivity(stats, title): any {

    const data = stats.map(e => e.value);

    const opts: any = {
      title: {
        text: this.service.getTranslation('graph.title.' + title)
      },
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value) => '' + value.toLocaleString('cs')
      },
      xAxis: {
        type: 'category',
        data: stats.map(e => e.name),
      },
      yAxis: {
        type: 'value',
      },
      series: [{ name: title, data, type: 'bar' }],
      legend: {
        data: stats.map(e => e.name),
        bottom: 0,
      }
    };
    return opts;
  }

}
