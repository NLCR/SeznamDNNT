package cz.inovatika.sdnnt.it;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.embedded.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

// must user real instance, embedded server doesnt work properly
public class SolrTestServer {

    public static final Logger LOGGER = Logger.getLogger(SolrTestServer.class.getName());

    public static final String TEST_URL = "http://localhost:28984/solr/";

    public static boolean TEST_SERVER_IS_RUNNING = pingTestServer();

    public static boolean pingTestServer()  {
        try {
            URL url = new URL(TEST_URL+"admin/info/system");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(300);
            connection.setReadTimeout(300);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            LOGGER.warning("Skipping solr test server ");
            return false;
        }
    }

    public void prepareCore(String core) {
    }


    public  void tearDownAfterClass() throws Exception {
    }

    public void setupBeforeClass(String core) {
        if (TEST_SERVER_IS_RUNNING) {
            prepareCore(core);
        }
    }

    public static SolrClient getClient(String collection) {
        String url = String.format("%s%s/", TEST_URL, collection);
        return new HttpSolrClient.Builder(url).build();
    }


    //
    public static SolrClient getClient() {
        return new HttpSolrClient.Builder(TEST_URL).build();
    }

    public void deleteCores(String ... cores) throws Exception {
        if(TEST_SERVER_IS_RUNNING) {
            try (SolrClient client = getClient()) {
                for (String core :  cores) {
                    UpdateResponse response = client.deleteByQuery(core, "*:*");
                    client.commit(core);
                }
            }
        }
    }

}
