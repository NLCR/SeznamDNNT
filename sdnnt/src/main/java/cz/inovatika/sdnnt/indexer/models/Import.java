package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
  @JsonIgnoreProperties(ignoreUnknown = true)
public class Import {
  
  public static final Logger LOGGER = Logger.getLogger(Import.class.getName());
  
  @Field
  public String id;
  
  @Field
  public String import_id;
  
  @Field
  public String import_uri;
  
  @Field
  public Date import_date;
  
  @Field
  public String import_origin;
  
  @Field
  public String ean;
  
  @Field
  public String name;
  
  @Field
  public String author;
  
  @Field
  public String na_vyrazeni;
  
  @Field
  public int hits_na_vyrazeni;
  
  @Field
  public String hit_type;
  
  @Field
  public int num_hits;
  
  @Field
  public List<String> identifiers;
  
  @Field
  public String catalog;
  
  @Field
  public String item;
  
  public static Import fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Import o = objectMapper.readValue(json, Import.class); 
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
  
  public String toJSONString() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }
  
  public static JSONObject save(String js) {

    try {
      Import o = Import.fromJSON(js);
      return save(o);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject save(Import o) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      DocumentObjectBinder dob = new DocumentObjectBinder();
      SolrInputDocument idoc = dob.toSolrInputDocument(o);
      //solr.addBean("zadost", zadost);
      solr.add("imports", idoc);
      solr.commit("imports");
      solr.close();
      return o.toJSON();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
}
