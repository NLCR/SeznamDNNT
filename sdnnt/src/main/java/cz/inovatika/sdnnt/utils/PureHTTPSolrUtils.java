package cz.inovatika.sdnnt.utils;

import cz.inovatika.sdnnt.Options;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SolrJ nepodporuje atomic update; :-(
 */
public class PureHTTPSolrUtils {

    public static final Logger LOGGER  = Logger.getLogger(PureHTTPSolrUtils.class.getName());

    private PureHTTPSolrUtils() {}

    public static JSONObject bulkField(List<String> bulk, String identifierField, String index, String updateField) {
        try {
            StringBuilder builder = new StringBuilder("<add>\n");
            bulk.stream().forEach(identifier-> {
                String document = String.format("<doc><field name=\"%s\">%s</field> %s </doc>", identifierField, identifier, updateField);
                builder.append(document);
            });

            builder.append("\n</add>");
            String solrHosts = Options.getInstance().getString("solr.host", "http://localhost:8983/solr/");
            Pair<Integer, String> post = SimplePOST.post(solrHosts + (solrHosts.endsWith("/") ? "" : "/") + index+"/update", builder.toString());

            JSONObject returnFromBulk = new JSONObject();
            returnFromBulk.put("statusCode", post.getLeft());
            returnFromBulk.put("message", post.getRight());

            return returnFromBulk;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
            JSONObject returnFromBulk = new JSONObject();
            returnFromBulk.put("statusCode", -1);
            returnFromBulk.put("message", e.getMessage());
            return returnFromBulk;
        }
    }


    public static JSONObject touchBulk(List<String> bulk, String identifierField, String index) {
        try {
            StringBuilder builder = new StringBuilder("<add>\n");
            bulk.stream().forEach(identifier-> {
                String document = String.format("<doc><field name=\"%s\">%s</field><field name=\"touch\" update=\"set\">true</field></doc>", identifierField, identifier);
                builder.append(document);
            });

            builder.append("\n</add>");
            String solrHosts = Options.getInstance().getString("solr.host", "http://localhost:8983/solr/");
            Pair<Integer, String> post = SimplePOST.post(solrHosts + (solrHosts.endsWith("/") ? "" : "/") + index+"/update", builder.toString());

            JSONObject returnFromBulk = new JSONObject();
            returnFromBulk.put("statusCode", post.getLeft());
            returnFromBulk.put("message", post.getRight());

            return returnFromBulk;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
            JSONObject returnFromBulk = new JSONObject();
            returnFromBulk.put("statusCode", -1);
            returnFromBulk.put("message", e.getMessage());
            return returnFromBulk;
        }
    }

    public static void commit(String index) {
        try {
            String solrHosts = Options.getInstance().getString("solr.host", "http://localhost:8983/solr/");
            SimplePOST.post(solrHosts + (solrHosts.endsWith("/") ? "" : "/") + index+"/update", "<commit/>");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }


}
