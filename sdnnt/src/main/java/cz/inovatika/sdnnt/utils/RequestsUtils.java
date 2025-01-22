package cz.inovatika.sdnnt.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

public class RequestsUtils {
    
    private RequestsUtils() {
        
    }

    public static Set<String> usedInRequest(Logger logger, SolrClient solr, List<String> identifiers, String typeOfRequest, List<String> states) {
        try {
            Set<String> retval = new HashSet<>();
            SolrQuery query = new SolrQuery("*")
                    .setFields("id", "identifiers")
                    .addFilterQuery("navrh:" + typeOfRequest)
                    .setRows(3000);
    
            if (states != null && states.size() > 0) {
                 String fq = states.stream().map(st-> {return '"'+st+'"';}).collect(Collectors.joining(" OR "));
                 query.addFilterQuery("state:("+fq+")");
            }
            
            String collected = identifiers.stream().map(id -> '"' + id + '"').collect(Collectors.joining(" OR "));
            query.addFilterQuery("identifiers:(" + collected + ")");
    
            SolrDocumentList zadost = solr.query("zadost", query).getResults();
            zadost.stream().forEach(solrDoc -> {
                Collection<Object> identsFromZadost = solrDoc.getFieldValues("identifiers");
                identsFromZadost.stream().map(Object::toString).forEach(foundInZadost -> {
                    if (identifiers.contains(foundInZadost)) {
                        retval.add(foundInZadost);
                    }
                });
            });
            return retval;
        } catch (SolrServerException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return new HashSet<>();
    }
    
    public static Set<String> usedInRequest(Logger logger, SolrClient solr, List<String> identifiers, String typeOfRequest) {
        return usedInRequest(logger, solr, identifiers, typeOfRequest, new ArrayList<>());
    }
    
}
