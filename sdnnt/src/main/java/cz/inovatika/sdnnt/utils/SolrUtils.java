package cz.inovatika.sdnnt.utils;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SolrUtils {

    public static final Logger LOGGER = Logger.getLogger(SolrUtils.class.getName());

    private SolrUtils() {}

    public static void quietCommit(SolrClient client, String collection) {
        try {
            client.commit(collection);
        } catch (SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }
}
