package cz.inovatika.sdnnt.services;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;

public interface SKCTypeService {

    public void update() throws IOException, SolrServerException;

}
