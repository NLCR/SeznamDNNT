package cz.inovatika.sdnnt.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.services.History;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;



public class HistoryImpl implements History {

    private SolrClient solr;

    public HistoryImpl(SolrClient solr) {
        this.solr = solr;
    }

    @Override
    public void log(String identifier, String oldRaw, String newRaw, String user, String type, String workflowId) {
        this.log(identifier,oldRaw, newRaw, user, type, workflowId, true);
    }

    @Override
    public void log(String identifier, String oldRaw, String newRaw, String user, String type, String workflowId, boolean commit) {
        try {

            ObjectMapper mapper = new ObjectMapper();
            String oldChanged = changeObject(new JSONObject(oldRaw)).toString();
            String newChanged = changeObject( new JSONObject(newRaw)).toString();

            JsonNode source = mapper.readTree(changeObject( new JSONObject(oldRaw)).toString());
            JsonNode target = mapper.readTree(changeObject( new JSONObject(newRaw)).toString());
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
            if (workflowId != null) {
                idoc.setField("workflowid", type);
            }
            idoc.setField("changes", changes.toString());
            // all dnnt state rename to stav
            solr.add("history", idoc);


        } catch (IOException | SolrServerException ex) {
            Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (commit) SolrJUtilities.quietCommit(solr, "history");
        }

    }

    private JSONObject changeObject(JSONObject oldObject) {
        changeDNTStav(oldObject);
        if (oldObject.has(HISTORIE_STAVU_FIELD)) {
            JSONArray oldJsonArray = oldObject.getJSONArray(HISTORIE_STAVU_FIELD);
            oldJsonArray.forEach(obj-> {
                if (obj instanceof JSONObject) {
                    changeDNTStav((JSONObject) obj);
                }
            });
        }
        return oldObject;
    }

    private void changeDNTStav(JSONObject oldObject) {
        if (oldObject.has(DNTSTAV_FIELD)) {
            oldObject.put("stav", oldObject.get(DNTSTAV_FIELD));
            oldObject.remove(DNTSTAV_FIELD);
        }
    }

}
