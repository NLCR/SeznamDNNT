package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;

/**
 * Implementation is able to create request
 * @author happy
 */
public interface RequestService {

    /**
     * Creates request
     * @param identifiers Lists of identifier
     * @throws AccountException Cannot create request
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

}
