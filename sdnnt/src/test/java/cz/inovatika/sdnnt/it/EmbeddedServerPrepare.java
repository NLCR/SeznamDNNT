package cz.inovatika.sdnnt.it;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.embedded.*;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EmbeddedServerPrepare {

    CoreContainer container;
    EmbeddedSolrServer  solrServer;
    JUnitClient client;


    public void prepareCore() {
        container =  new CoreContainer(Path.of("src/test/resources/cz/inovatika/sdnnt/solr"), new Properties());
        container.load();
        solrServer = new EmbeddedSolrServer( container, "users" );
        client = new JUnitClient(solrServer);
    }


    public  void tearDownAfterClass() throws Exception {
        client.down();
    }

    public void setupBeforeClass() {
        prepareCore();
    }

    public SolrClient getClient() {
        return client;
    }

    public void deleteCores(String ... cores) throws Exception {
        for (String core :  cores) {
            UpdateResponse response = client.deleteByQuery(core, "*:*");
            client.commit(core);

        }
    }

}
