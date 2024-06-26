package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.joda.time.LocalDate;
import org.json.JSONArray;
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
  public String import_url;
  
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
  public List<String> na_vyrazeni;
  
  @Field
  public int hits_na_vyrazeni;
  
  @Field
  public String hit_type;
  
  @Field
  public int num_hits;
  
  @Field
  public List<Map> identifiers;
  
//  @Field
//  public String catalog;
  
  @Field
  public Map<String, String> item;
  
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
      ObjectMapper mapper = new ObjectMapper();
      JSONObject p = new JSONObject(mapper.writeValueAsString(o.item));
      idoc.setField("item", p.toString());
      JSONObject i = new JSONObject(mapper.writeValueAsString(o.identifiers));
      idoc.setField("identifiers", i.toString());
      //solr.addBean("zadost", zadost);
      solr.add("imports_documents", idoc);
      solr.commit("imports_documents");
      solr.close();
      return o.toJSON();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject approve(Import o, String identifier, String user) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      DocumentObjectBinder dob = new DocumentObjectBinder();
      ObjectMapper mapper = new ObjectMapper();
      JSONObject p = new JSONObject(mapper.writeValueAsString(o.item));
      JSONArray ids = new JSONArray(mapper.writeValueAsString(o.identifiers));
      
      SolrInputDocument idoc = dob.toSolrInputDocument(o);
      idoc.setField("item", p.toString());
      idoc.setField("identifiers", null);
      o.identifiers = new ArrayList<>();
      for(int i =0; i< ids.length(); i++) {
        if (identifier.equals(ids.getJSONObject(i).getString("identifier"))) {
          ids.getJSONObject(i).put("approved", true).put("approved_user", user);
        } 
        idoc.addField("identifiers", ids.getJSONObject(i).toString());
        o.identifiers.add(ids.getJSONObject(i).toMap());
      }
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
  
  public static JSONObject setControlled(String id, String note, String user) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("id", id);
      
      Map<String,Object> fieldModifier = new HashMap<>(1);
      fieldModifier.put("set",true);

      idoc.addField("controlled", fieldModifier); 
      Map<String,Object> fieldModifier1 = new HashMap<>(1);
      fieldModifier1.put("set", note);
      idoc.addField("controlled_note", fieldModifier1); 

      idoc.addField("controlled_date", LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME)); 
      
      Map<String,Object> fieldModifier2 = new HashMap<>(1);
      fieldModifier2.put("set",user);
      idoc.addField("controlled_user", fieldModifier2); 

      solr.add("imports_documents", idoc);
      solr.commit("imports_documents");
      solr.close();
      return new JSONObject().put("saved", true);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static JSONObject setProcessed(String id, String user) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("id", id);
      
      Map<String,Object> fieldModifier = new HashMap<>(1);
      fieldModifier.put("set",true);
      idoc.addField("processed", fieldModifier); 
      
      Map<String,Object> fieldModifier2 = new HashMap<>(1);
      fieldModifier2.put("set",user);
      idoc.addField("processed_user", fieldModifier2); 

      solr.add("imports", idoc);
      solr.commit("imports");
      solr.close();
      return new JSONObject().put("saved", true);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  
  public static JSONObject changeStav(JSONObject input, String user) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("id", input.getString("id"));
      
      Map<String,Object> fieldModifier = new HashMap<>(1);
      List<String> l = new ArrayList();
      JSONArray ids = input.getJSONArray("identifiers");
      for (int i = 0; i < ids.length(); i++) {
        l.add(ids.getJSONObject(i).toString());
      }
      fieldModifier.put("set", l);
      
      idoc.addField("identifiers", fieldModifier); 

      solr.add("imports_documents", idoc);
      solr.commit("imports_documents");
      solr.close();
      return new JSONObject().put("saved", true);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  
  public static SolrDocument isControlled(String id) {
    SolrDocument ret = null;
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      int days = Options.getInstance().getInt("importControlledExpireationDays", 30);
      SolrQuery q = new SolrQuery("*")
              .addFilterQuery("item_id:" + id)
              .addFilterQuery("controlled_date:[NOW/DAY-" + days + "DAYS TO NOW]")
              .addFilterQuery("controlled:true");
      SolrDocumentList docs = solr.query("imports_documents", q).getResults();
      if (docs.getNumFound() > 0) {
        ret = docs.get(0);
      }
      solr.close();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ret;
  }
 
  
}
