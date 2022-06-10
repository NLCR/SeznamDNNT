package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

public interface SKCDeleteService extends LoggerAware {

    public void updateDeleteInfo(List<String> deleteInfo);

    public void update() throws IOException, SolrServerException;

}
