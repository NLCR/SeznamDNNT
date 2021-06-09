/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import static cz.inovatika.sdnnt.indexer.models.Zadost.LOGGER;
import java.util.Date;
import java.util.logging.Level;
import org.apache.solr.client.solrj.beans.Field;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ZadostProcess {
  
  @Field
  public Date date;
  
  @Field
  public String user;
  
  @Field
  public String reason;
  
  @Field
  public String state;
  
  public static ZadostProcess fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    ZadostProcess o = objectMapper.readValue(json, ZadostProcess.class); 
    return o;
  }
  
  public JSONObject toJSON() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JSONObject ret = new JSONObject(mapper.writeValueAsString(this));
      return ret;
    } catch (JsonProcessingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
}
