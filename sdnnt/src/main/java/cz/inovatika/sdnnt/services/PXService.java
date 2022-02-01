package cz.inovatika.sdnnt.services;

import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.List;

public interface PXService {
    public List<String> check();
    public void request(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException;

}
