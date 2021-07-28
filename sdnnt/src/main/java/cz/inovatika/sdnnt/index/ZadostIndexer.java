/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ZadostIndexer {
  public static final Logger LOGGER = Logger.getLogger(ZadostIndexer.class.getName());
  
  public JSONObject add(JSONObject json) {
    JSONObject ret = new JSONObject();
    Options opts = Options.getInstance();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
      

      // Insert in history
      SolrInputDocument idoc = new SolrInputDocument();
//      idoc.setField("identifier", id);
//      idoc.setField("user", user);
//      idoc.setField("type", "app");
//      idoc.setField("changes", ret.toString());
      solr.add("zadost", idoc);
      solr.commit("zadost");

      // Update record in catalog
//      MarcRecord mr = MarcRecord.fromJSON(newRaw.toString());
//      mr.fillSolrDoc();
//      solr.add("catalog", mr.toSolrDoc());
//      solr.commit("catalog");

      //ret.put("newRecord", new JSONObject(JsonPatch.apply(fwPatch, source).toString()));
      //ret.put("newRaw", mr.toJSON());
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }
}
