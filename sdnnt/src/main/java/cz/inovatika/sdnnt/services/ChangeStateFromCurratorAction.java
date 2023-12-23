package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

public interface ChangeStateFromCurratorAction {
    
    public List<String> check();

    public void update(List<String> identifiers) throws IOException, SolrServerException;
}
