package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service responsible for creating reuests or updating titles which should be public according to the date of publication
 */
public interface PXYearService {

    /**
     * Check titles and return all possible candidates
     * @return List of candidates
     */
    public List<String> check();

    /**
     * Creating requests
     * @param identifiers Lists of identifier
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

    /**
     * Update titles
     * @param identifiers
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

    /**
     * Get logger for logging customization
     * @return
     */
    public Logger getLogger();
}
