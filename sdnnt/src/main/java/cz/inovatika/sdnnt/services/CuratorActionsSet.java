package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;

/** 
 * Nastavi kuratorskou akci
 * @author happy
 */
public interface CuratorActionsSet {
    
    public List<String> check() throws AccountException, IOException, SolrServerException;

    public int update(List<String> identifiers) throws IOException, ConflictException, SolrServerException;

}
