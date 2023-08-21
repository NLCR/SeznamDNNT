package cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.model.DataCollections;

public class SiglaControlField001 {
    
    public static final Logger LOGGER = Logger.getLogger(SiglaControlField001.class.getName());
    

//    public static List<String> findBySiglaControlfied001(SolrClient solrClient, String sigla, String controlField001) throws SolrServerException, IOException {
//        List<String> revals = new ArrayList<>();
//        //String combination = sigla +marc910x;
//        
//        Set<String> identifiers = new LinkedHashSet<>();
//        SolrQuery idQuery = new SolrQuery("*").setRows(100);
//        idQuery = idQuery.addFilterQuery(String.format("controlfield_001:%s AND sigla:%s",sigla, controlField001));
//        idQuery = idQuery.addFilterQuery("fmt:SE OR fmt:BK").setRows(1000);
//        idQuery = idQuery.setFields("identifier");
//        //LOGGER.info("Query: "+idQuery);
//        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
//        long numFound = results.getNumFound();
//        
//        
//        
//        for (SolrDocument sDocument : results) {
//            String fValue = sDocument.getFieldValue("identifier").toString();
//            retvals.add(fValue);
//            
//        }
//        
//        List<String> retvals = new ArrayList<>(identifiers);
//        return retvals;
//    }    

}
