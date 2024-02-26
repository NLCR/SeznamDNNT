package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Service responsible for creating reuests or updating titles which should be public according to the state in digital library
 */
public interface PXKrameriusService extends RequestService, LoggerAware {
    
    public enum CheckResults {
        public_dl_results, disable_ctx_results;
    }
    
    
    public enum DataTypeCheck {
        live, // live instance
        granularity; // previous granularity fetch
    }
    
    public Map<CheckResults,Set<String>> check();
    
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;
    
    
    public void disableContext(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;
    
}
