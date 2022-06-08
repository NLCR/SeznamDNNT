package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service responsible for creating reuests or updating titles which should be public according to the state in digital library
 */
public interface PXKrameriusService extends RequestService, LoggerAware {

	/**
     * Check the titles and returns all possible candidates
     * @return List of candidates
     */
    public List<String> check();


    /**
     * Update titles
     * @param identifiers
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

}
