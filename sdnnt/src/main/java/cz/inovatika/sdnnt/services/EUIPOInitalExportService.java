package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrServerException;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;

/** 
 * 
 * @author happy
 * TODO: Pozdeni rozsirena o standardni export 
 */
public interface EUIPOInitalExportService extends LoggerAware{

    /**
     * Check titles and return all possible candidates
     * @return List of candidates
     */
    public List<String> check(String format);



    /**
     * Update titles
     * @param identifiers
     * @throws AccountException
     * @throws IOException
     * @throws ConflictException
     * @throws SolrServerException
     */
    public void update(String format, String exortIdentifier, List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

    
    public String getLastExportedDate();

}