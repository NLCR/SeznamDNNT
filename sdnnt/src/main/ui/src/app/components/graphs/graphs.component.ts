import { DatePipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { Component, Input, OnInit } from '@angular/core';
import { ECharts, EChartsOption } from 'echarts';
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
  from: string;
  to: string;
  historyStats: any;
  userEchart: ECharts;
  selectedUsers: {[user: string]: boolean} = {};
  selectedStates: {[state: string]: boolean} = {};

  constructor(
    private datePipe: DatePipe,
    private service: AppService) {
      const today = new Date();
      const yearAgo = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());
      this.from = this.datePipe.transform(yearAgo, 'yyyy-MM-dd');
      this.to = this.datePipe.transform(today, 'yyyy-MM-dd');
    }

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
          show: true,
          formatter: '{b}: {c}'
        },
        emphasis: {
          label: {
            show: true,
            fontWeight: 'bold'
          }
        },
        data: this.getSelectedStateData(),
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
            } else if (paramsExt?.data?.name1 === 'D') {
              return '#4e342e'
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

    this.licenseOpts = {
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
          show: true,
          formatter: '{b}: {c}'
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
      this.facets.dntstav.forEach(state => {
        if (this.selectedStates[state.name] === undefined) {
          this.selectedStates[state.name] = true;
        }
      });
      this.setSeries();
    });
  }

  onStateSelectionChange() {
    const options: any = this.stavOpts;
    this.stavOpts = {
      ...options,
      series: {
        ...options.series,
        data: this.getSelectedStateData()
      }
    };
  }

  formatCount(value: number): string {
    return value.toLocaleString('cs');
  }

  private getSelectedStateData(): any[] {
    if (!this.facets || !this.facets.dntstav) {
      return [];
    }
    return this.facets.dntstav
      .filter(state => this.selectedStates[state.name] !== false)
      .map(state => ({
        value: state.value,
        name: state.name + ' ' + this.service.getTranslation('state.' + state.name),
        name1: state.name
      }));
  }

  getStatsHistory() {
    if (!this.from || !this.to || this.from > this.to) {
      return;
    }
    this.service.getStatsHistory(this.interval, this.from, this.to).subscribe(res => {
      this.historyStats = res;

      const stats = res.facet_counts.facet_pivot.type;
      this.historyStavOpts = this.setHistoryStats(stats, 'History by type', false);
      this.historyUserOpts = this.setHistoryStats(res.facet_counts.facet_pivot.user, 'History by user', true);
      this.historyUserActivityOpts = this.setUserActivity(res.facet_counts.facet_fields.user.filter(e => e.name !== 'harvester'), "User activity");
    });
  }

  setHistoryStats(stats: any, title: string, user: boolean): any {
    const series = [];
    const legend = [];

    (stats || []).forEach(t => {
      if (t.value !== 'app' && t.value !== 'harvester') {
        // const s = t.ranges.indextime.counts.map(e => { return { x: new Date(e.name).getTime(), y: e.value } });
        const name = user ? t.value : this.service.getTranslation('graph.field.' + t.value);
        const values = t.ranges.indextime.counts.map(e => e.value);
        const total = values.reduce((sum, value) => sum + value, 0);
        const seriesName = name + ' (' + total.toLocaleString('cs') + ')';
        series.push({
          data: values,
          type: 'line',
          name: seriesName,
          label: { show: true, position: 'top' }
        });
        legend.push(seriesName);
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
        data: stats && stats.length ? stats[0].ranges.indextime.counts.map(e => {
          if (this.interval === '1YEAR') {
            return this.datePipe.transform(new Date(e.name), 'yyyy')
          } else if (this.interval === '7DAYS') {
            return this.datePipe.transform(new Date(e.name), 'ww (MM.yyyy)')
          } else if (this.interval === '1DAY') {
            return this.datePipe.transform(new Date(e.name), 'dd.MM.yyyy')
          } else {
            return this.datePipe.transform(new Date(e.name), 'MM.yyyy')
          }

        }) : [],
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
      series: [{
        name: this.service.getTranslation('graph.title.' + title),
        data,
        type: 'bar',
        label: { show: true, position: 'top' }
      }],
      legend: {
        data: stats.map(e => e.name),
        bottom: 0,
      }
    };
    return opts;
  }

  onChartInit(ec: ECharts) {
    this.userEchart = ec;
  }

  selectUserLegent(e) {
    if (this.selectedUsers[e.name]) {
      // select all
      for (const legend in e.selected) {
        this.userEchart.dispatchAction({
          type: 'legendSelect',
          name: legend,
        })
        this.selectedUsers[legend] = true;
      }
    } else {
      for (const legend in e.selected) {
        if (legend !== e.name) {
          this.userEchart.dispatchAction({
            type: 'legendUnSelect',
            name: legend,
          });
          this.selectedUsers[legend] = false;
        } else {
          this.userEchart.dispatchAction({
            type: 'legendSelect',
            name: e.name,
          })
          this.selectedUsers[legend] = true;
        }
      }
    }

  }

  exportCsv() {
    if (!this.facets || !this.historyStats) {
      return;
    }

    const rows: any[][] = [];
    rows.push([this.service.getTranslation('graph.title.States')]);
    rows.push([this.service.getTranslation('graph.export.name'), this.service.getTranslation('graph.export.count')]);
    this.facets.dntstav.forEach(e => rows.push([
      e.name + ' ' + this.service.getTranslation('state.' + e.name), e.value
    ]));
    rows.push([]);
    rows.push([this.service.getTranslation('graph.title.Licenses')]);
    rows.push([this.service.getTranslation('graph.export.name'), this.service.getTranslation('graph.export.count')]);
    this.facets.license.forEach(e => rows.push([
      e.name + ' ' + this.service.getTranslation('license.' + e.name), e.value
    ]));

    rows.push([]);
    rows.push([this.service.getTranslation('graph.title.User activity')]);
    rows.push([this.service.getTranslation('graph.export.name'), this.service.getTranslation('graph.export.count')]);
    this.historyStats.facet_counts.facet_fields.user
      .filter(e => e.name !== 'harvester')
      .forEach(e => rows.push([e.name, e.value]));

    this.appendHistoryCsv(rows, this.historyStats.facet_counts.facet_pivot.type,
      this.service.getTranslation('graph.title.History by type'), false);
    this.appendHistoryCsv(rows, this.historyStats.facet_counts.facet_pivot.user,
      this.service.getTranslation('graph.title.History by user'), true);

    const csv = '\ufeff' + rows.map(row => row.map(value => this.csvValue(value)).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'sdnnt-statistiky-' + this.from + '-' + this.to + '.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  private appendHistoryCsv(rows: any[][], stats: any[], title: string, user: boolean) {
    rows.push([]);
    rows.push([title]);
    rows.push([
      this.service.getTranslation('graph.export.period'),
      this.service.getTranslation('graph.export.name'),
      this.service.getTranslation('graph.export.count')
    ]);
    (stats || []).forEach(item => {
      if (item.value !== 'app' && item.value !== 'harvester') {
        const name = user ? item.value : this.service.getTranslation('graph.field.' + item.value);
        item.ranges.indextime.counts.forEach(point => rows.push([
          this.datePipe.transform(new Date(point.name), this.interval === '1YEAR' ? 'yyyy' : 'MM.yyyy'),
          name,
          point.value
        ]));
      }
    });
  }

  private csvValue(value: any): string {
    const text = value == null ? '' : String(value);
    return /[;"\r\n]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text;
  }

}
