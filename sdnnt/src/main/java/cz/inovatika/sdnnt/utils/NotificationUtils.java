package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/** Notification utils */
public class NotificationUtils {

    private NotificationUtils() {}

    //Indexer.getClient()
    public static JSONArray applySimpleNotifications(List<String> identifiers, String user, SolrClient solr) {
        try {
            if (!identifiers.isEmpty()) {
                String q = identifiers.toString().replace("[", "(").replace("]", ")").replaceAll(",", "");
                SolrQuery query = new SolrQuery("identifier:" + q).addFilterQuery("user:" + user)
                        .setFields("identifier").setRows(100); // maximum 100 notifications
                QueryRequest qreq = new QueryRequest(query);
                NoOpResponseParser rParser = new NoOpResponseParser();
                rParser.setWriterType("json");
                qreq.setResponseParser(rParser);
                NamedList<Object> qresp = solr.request(qreq, "notifications");
                return (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
            } else
                return new JSONArray();
        } catch (SolrServerException | IOException ex) {
            CatalogSearcher.LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
    public static JSONArray applyRuleNotifications(User user, List<String> identifiers) {
        JSONArray jsonArray = new JSONArray();
        if (!identifiers.isEmpty()) {
            
            try {
                NotificationsService service = new NotificationServiceImpl(null, null);
                
                List<AbstractNotification> ruleNotifications = service.findNotificationsByUser(user.getUsername(), TYPE.rule);
                List<String> notificationFilters = new ArrayList<>();
                ruleNotifications.stream().forEach(nru-> {
                    RuleNotification ru = (RuleNotification) nru;
                    String provideSearchQueryFilters = ru.provideSearchQueryFilters();
                    if (!provideSearchQueryFilters.trim().equals("")) {
                        notificationFilters.add(ru.provideSearchQueryFilters());
                    }
                });
                if (!notificationFilters.isEmpty()) {
                    SolrClient solr = Indexer.getClient();
                    String q = "("+identifiers.stream().collect(Collectors.joining(" "))+")";
                    String orQuery = notificationFilters.stream().map(it->"("+it+")").collect(Collectors.joining("  "));
                    
                    SolrQuery query = new SolrQuery("identifier:" + q)
                            //.addFilterQuery(q)
                            .addFilterQuery(orQuery)
                            .setFields("identifier").setRows(100);
                    
                    QueryRequest qreq = new QueryRequest(query);
                    NoOpResponseParser rParser = new NoOpResponseParser();
                    rParser.setWriterType("json");
                    qreq.setResponseParser(rParser);
                    NamedList<Object> qresp = solr.request(qreq, "catalog");
                    return (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
                    
                }
            } catch (NotificationsException | SolrServerException | IOException e) {
                CatalogSearcher.LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }        
        return jsonArray;
    }
}
