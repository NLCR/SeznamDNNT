package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.Options;
import static cz.inovatika.sdnnt.UserController.LOGGER;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
  @JsonIgnoreProperties(ignoreUnknown = true)
public class Zadost {
  
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
  public String new_stav;
  
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
  
  public static Zadost fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    Zadost o = objectMapper.readValue(json, Zadost.class); 
    return o;
  }
  
  public static JSONObject save(String js, String username) {

    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      Zadost zadost = Zadost.fromJSON(js);
      zadost.user = username;
      
      solr.addBean("zadost", zadost);
      solr.commit("zadost");
      solr.close();
      return new JSONObject(js);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
}
