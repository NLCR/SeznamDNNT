package cz.inovatika.sdnnt.services;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

/**
 * Mapovani zaznamu dnt a skc zaznamu
 * @author happy
 */
public interface DNTSKCPairService extends LoggerAware {

    public void update() throws IOException, SolrServerException;
}
