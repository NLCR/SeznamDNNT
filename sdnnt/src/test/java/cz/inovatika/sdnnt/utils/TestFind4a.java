package cz.inovatika.sdnnt.utils;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;

public class TestFind4a {
    
    public static void main(String[] args) throws SolrServerException, IOException {
        SolrClient solrClient = Indexer.getClient();
     

        SolrQuery idQuery = new SolrQuery(String.format("identifier:\"%s\"", "oai:aleph-nkp.cz:SKC01-000732820"));
        //idQuery.addFilterQuery("NOT dntstav:D").addFilterQuery("NOT kuratorstav:DX").setRows(100);
        idQuery = idQuery.addFilterQuery("(NOT dntstav:D OR NOT kuratorstav:DX)").setRows(100);

        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
        if (results.getNumFound() == 1) {
            System.out.println(results.getNumFound());
        } else {
            System.out.println("Nenasel nic ");
        }
        

    }
}
