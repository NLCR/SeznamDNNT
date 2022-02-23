package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;

/**
 * PX service, checking title against kramerius
 */
public interface PXKrameriusService {

    /**
     * Iterate over filter fields and check states against kramerius
     * @return
     */
    public List<String> check();

    /**
     * Sends requests
     * @param identifiers Found identifiers
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

    /**
     * Update
     * @param identifiers
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;
}
