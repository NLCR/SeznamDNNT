package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import cz.inovatika.sdnnt.services.impl.HistoryImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
  @JsonIgnoreProperties(ignoreUnknown = true)
public class Zadost {
  
  public static final Logger LOGGER = Logger.getLogger(Zadost.class.getName());
  
  @Field
  public String id;
  
  @Field
  public List<String> identifiers;
  
  @Field
  public String typ;

  @Field
  public String state;
  
  @Field
  public String kurator;
  
  @Field
  public String navrh;
  
  @Field
  public String poznamka;
  
  @Field
  public String pozadavek;
  
  @Field
  public Date datum_zadani;
  
  @Field
  public Date datum_vyrizeni;
  
  @Field
  public String formular;
  
  @Field
  public Map<String, ZadostProcess> process;

  @Field
  public String user;

  @Field
  public String institution;

  @Field
  public String delegated;

  @Field
  public String priority;


  public static Zadost fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Zadost o = objectMapper.readValue(json, Zadost.class); 
      if (o.process == null) {
        o.process = new HashMap<>();
      }
    return o;
  }
  
  public JSONObject toJSON() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JSONObject ret = new JSONObject(mapper.writeValueAsString(this));
      // ret.put("process", new JSONObject(this.process));
      return ret;
    } catch (JsonProcessingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject save(String js, String username) {
    try {

      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject saveWithFRBR(String js, String username, String frbr) {

    try {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      SolrClient solr = Indexer.getClient();
      SolrQuery query = new SolrQuery("frbr:\"" + frbr + "\"")
              .setFields("identifier")
              .setRows(10000);
      SolrDocumentList docs = solr.query("catalog", query).getResults();
      for (SolrDocument doc : docs) {
        zadost.identifiers.add((String) doc.getFirstValue("identifier"));
      }
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject markAsProcessed(String js, String username) {

    try {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.kurator = username;
      zadost.datum_vyrizeni = new Date(); // ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
      zadost.state = "processed";
      return save(zadost);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject approve(String identifier, String js, String komentar, String username, String approvestate) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      String oldProcess = new JSONObject().put("process", zadost.process).toString();
      ZadostProcess zprocess = new ZadostProcess();
      zprocess.state = approvestate != null ? approvestate : "approved";
      zprocess.user = username;
      zprocess.reason = komentar;
      zprocess.date = new Date();
      zadost.process.put(identifier, zprocess);
      String newProcess = new JSONObject().put("process", zadost.process).toString();
      new HistoryImpl(Indexer.getClient()).log(zadost.id, oldProcess, newProcess, username, "zadost");
      return save(zadost);
    } catch (JsonProcessingException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }


  public static JSONObject reject(String identifier, String js, String reason, String username) {
    try {
      
      Zadost zadost = Zadost.fromJSON(js);
      if (zadost.process == null) {
        zadost.process = new HashMap<>();
      }
      String oldProcess = new JSONObject().put("process", zadost.process).toString();
      ZadostProcess zprocess = new ZadostProcess();
      zprocess.state = "rejected";
      zprocess.user = username;
      zprocess.reason = reason;
      zprocess.date = new Date();
      zadost.process.put(identifier, zprocess);
      String newProcess = new JSONObject().put("process", zadost.process).toString();
      new HistoryImpl(Indexer.getClient()).log(zadost.id, oldProcess, newProcess, username, "zadost");
      return save(zadost);
    } catch (JsonProcessingException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject save(Zadost zadost) {

    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      DocumentObjectBinder dob = new DocumentObjectBinder();
      SolrInputDocument idoc = dob.toSolrInputDocument(zadost);
      ObjectMapper mapper = new ObjectMapper();
      JSONObject p = new JSONObject(mapper.writeValueAsString(zadost.process));
      idoc.setField("process", p.toString());
      //solr.addBean("zadost", zadost);
      solr.add("zadost", idoc);
      solr.commit("zadost");
      solr.close();
      return zadost.toJSON();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
}
