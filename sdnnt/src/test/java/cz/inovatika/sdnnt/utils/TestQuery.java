package cz.inovatika.sdnnt.utils;

import org.apache.solr.client.solrj.SolrQuery;

public class TestQuery {


    public static void main(String[] args) {
        String q = "test";
        String modifiedQuery = String.format("fullText:%s OR id_pid:%s OR id_all_identifiers:%s OR id_all_identifiers_cuts:%s", q,q,q,q);
        SolrQuery query = new SolrQuery(modifiedQuery)
                .setRows(100)
                .setStart(20)
                .setFacet(true).addFacetField("fmt", "language", "dntstav", "kuratorstav", "license", "sigla", "nakladatel")
                .setFacetMinCount(1)
                .setParam("json.nl", "arrntv")
                .setParam("stats", true)
                .setParam("stats.field", "rokvydani")
                .setParam("q.op", "AND")
                .setFields("*,raw:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json]");


        query.set("defType", "edismax");
        query.set("qf", "fullText id_pid id_all_identifiers id_all_identifiers_cuts");
        System.out.println(query);
    }
}
