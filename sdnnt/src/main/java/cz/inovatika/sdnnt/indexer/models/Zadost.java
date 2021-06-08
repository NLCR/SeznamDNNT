package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.History;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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
  public String user;
  
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
  public String process;
  
  public static Zadost fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Zadost o = objectMapper.readValue(json, Zadost.class); 
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
  
  public static JSONObject approve(String identifier, String js, String username) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      JSONObject p = new JSONObject();
      if (zadost.process != null) {
        p = new JSONObject(zadost.process);
      } else {
        zadost.process = "{}";
      }
      JSONObject process = new JSONObject();
      process.put("state", "approved");
      process.put("user", username);
      process.put("date", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
      p.put(identifier, process);
      History.log(zadost.id, zadost.process, p.toString(), username, "zadost");
      zadost.process = p.toString();
      return save(zadost);
    } catch (JsonProcessingException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject reject(String identifier, String js, String reason, String username) {
    try {
      Zadost zadost = Zadost.fromJSON(js);
      JSONObject p = new JSONObject();
      if (zadost.process != null) {
        p = new JSONObject(zadost.process);
      } else {
        zadost.process = "{}";
      }
      JSONObject process = new JSONObject();
      process.put("state", "rejected");
      process.put("user", username);
      process.put("date", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
      process.put("reason", reason);
      p.put(identifier, process);
      History.log(zadost.id, zadost.process, p.toString(), username, "zadost");
      zadost.process = p.toString();
      return save(zadost);
    } catch (JsonProcessingException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject save(Zadost zadost) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      solr.addBean("zadost", zadost);
      solr.commit("zadost");
      solr.close();
      return zadost.toJSON();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
}
