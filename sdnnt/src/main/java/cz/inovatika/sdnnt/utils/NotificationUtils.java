package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/** Notification utils */
public class NotificationUtils {

    private NotificationUtils() {}

    //Indexer.getClient()
    public static JSONArray findNotifications(List<String> identifiers, String user, SolrClient solr) {
      try {
        if (!identifiers.isEmpty()) {
          String q = identifiers.toString().replace("[", "(").replace("]", ")").replaceAll(",", "");
          SolrQuery query = new SolrQuery("identifier:" + q)
                  .addFilterQuery("user:" + user)
                  .setFields("identifier").setRows(100); //maximum 100 notifications

          QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "notifications");
          return (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
        } else return new JSONArray();
      } catch (SolrServerException | IOException ex) {
        CatalogSearcher.LOGGER.log(Level.SEVERE, null, ex);
        return null;
      }
    }
}
