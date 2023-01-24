package cz.inovatika.sdnnt.utils;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Solr utils; utility methods for working with solr */
public class SolrJUtilities {

    public static final Logger LOGGER = Logger.getLogger(SolrJUtilities.class.getName());

    private SolrJUtilities() {}

    public static void quietCommit(SolrClient client, String collection) {
        try {
            client.commit(collection);
        } catch (SolrServerException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

    public static Date solrDate(String date) {
        Instant parse = Instant.parse(date);
        return Date.from(parse);
    }

    public static String solrDateString(Date date) {
        return date.toInstant().toString();
    }

    public static JSONArray jsonDocsFromResult(SolrClient client, SolrQuery query, String collection) throws IOException, SolrServerException {
        QueryRequest qreq = new QueryRequest(query);
        NoOpResponseParser rParser = new NoOpResponseParser();
        rParser.setWriterType("json");
        qreq.setResponseParser(rParser);

        NamedList<Object> qresp = client.request(qreq, collection);
        JSONObject ret = new JSONObject((String) qresp.get("response"));
        JSONArray jsonArray = ret.getJSONObject("response").getJSONArray("docs");

        return jsonArray;
    }
    
    
    public static void atomicAdd(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("add", fValue);
        idoc.addField(fName, modifier);
    }

    public static void atomicSet(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }

}
