import { SolrDocument } from 'src/app/shared/solr-document';

export class SolrResponse {
  response: {
    docs: SolrDocument[];
    numFound: number,
    start: number
  };
  responseHeader: {
    QTime: number;
    params: any;
    status: number
  };
  facet_counts: {
    facet_intervals: any;
    facet_ranges: any;
    facet_fields: {[field: string]: {name: string, type: string, value: number}[]};
    facet_heatmaps: any;
  };
  stats: {
    stats_fields: { [field: string]: {min: any, max: any, count: number, from: any, until: any}}
  };
  error?: any;
}

