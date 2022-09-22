package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * Mapping SKC DNT records
 * @author happy
 */
public interface DNTSKCPairService extends LoggerAware {
    
    /**
     * Main mapping procedure
     * @throws IOException
     * @throws SolrServerException
     */
    public void update() throws IOException, SolrServerException;
}
