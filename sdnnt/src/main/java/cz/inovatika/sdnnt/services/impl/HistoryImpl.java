package cz.inovatika.sdnnt.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.services.History;
import cz.inovatika.sdnnt.utils.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


// moved from cz.inovatika.sdnnt.index.History
public class HistoryImpl implements History {

    private SolrClient solr;

    public HistoryImpl(SolrClient solr) {
        this.solr = solr;
    }

    @Override
    public void log(String identifier, String oldRaw, String newRaw, String user, String type) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode source = mapper.readTree(oldRaw);
            JsonNode target = mapper.readTree(newRaw);
            JSONObject changes = new JSONObject();

            JsonNode fwPatch = JsonDiff.asJson(source, target);
            JsonNode bwPatch = JsonDiff.asJson(target, source);
            changes.put("forward_patch", new JSONArray(fwPatch.toString()));
            changes.put("backward_patch", new JSONArray(bwPatch.toString()));
            // Insert in history
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField("identifier", identifier);
            idoc.setField("user", user);
            idoc.setField("type", type);
            idoc.setField("changes", changes.toString());
            solr.add("history", idoc);
        } catch (IOException | SolrServerException ex) {
            Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            SolrUtils.quietCommit(solr, "history");
        }

    }
}
