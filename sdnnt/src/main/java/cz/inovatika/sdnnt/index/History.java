/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import static cz.inovatika.sdnnt.index.Indexer.getClient;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class History {
  
  public static final Logger LOGGER = Logger.getLogger(History.class.getName());
  
  public static void log(String identifier, String oldRaw, String newRaw, String user, String type) {
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
      SolrClient solr = getClient();
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("identifier", identifier);
      idoc.setField("user", user);
      idoc.setField("type", type);
      idoc.setField("changes", changes.toString());
      solr.add("history", idoc);
      solr.commit("history");
    } catch (IOException | SolrServerException ex) {
      Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
  
}
