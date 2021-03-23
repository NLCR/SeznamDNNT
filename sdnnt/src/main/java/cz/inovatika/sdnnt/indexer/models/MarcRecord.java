/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class MarcRecord {

  // Header fields
  public String identifier;
  public String datestamp;
  public String setSpec;
  public String leader;
  
  public boolean isDeleted = false;

  // <marc:controlfield tag="001">000000075</marc:controlfield>
  // <marc:controlfield tag="003">CZ PrDNT</marc:controlfield>
  public Map<String, String> controlFields = new HashMap();

//  <marc:datafield tag="650" ind1="0" ind2="7">
//    <marc:subfield code="a">dějiny</marc:subfield>
//    <marc:subfield code="7">ph114390</marc:subfield>
//    <marc:subfield code="2">czenas</marc:subfield>
//  </marc:datafield>
//  <marc:datafield tag="651" ind1=" " ind2="7">
//    <marc:subfield code="a">Praha (Česko)</marc:subfield>
//    <marc:subfield code="7">ge118011</marc:subfield>
//    <marc:subfield code="2">czenas</marc:subfield>
//  </marc:datafield> 
  public Map<String, List<DataField>> dataFields = new HashMap();
  public SolrInputDocument sdoc = new SolrInputDocument();
  
  final public static List<String> tagsToIndex = Arrays.asList("015", "020", "022","035", "040", "100", "245", "250", "260", "856", "990", "992", "998", "956");
  
  public static MarcRecord fromJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    MarcRecord mr = objectMapper.readValue(json, MarcRecord.class);
    return mr;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    json.put("identifier", identifier);
    json.put("datestamp", datestamp);
    json.put("setSpec", setSpec);
    json.put("leader", leader);
    json.put("controlFields", controlFields);

    json.put("dataFields", dataFields);
    return json;
  }
  
  public SolrInputDocument toSolrDoc() {
    sdoc.setField("identifier", identifier);
    sdoc.setField("datestamp", datestamp);
    sdoc.setField("setSpec", setSpec);
    sdoc.setField("leader", leader);
    sdoc.setField("raw", toJSON().toString());

    // Control fields
    for (String cf : controlFields.keySet()) {
      sdoc.addField("controlfield_" + cf, controlFields.get(cf));
    }
    if (leader != null) {
      sdoc.setField("item_type", leader.substring(7,8) );
    }
    return sdoc;
  }

  public void fillSolrDoc() {
    
    for (String tag : tagsToIndex) {
      if (dataFields.containsKey(tag)) {
        for (DataField df : dataFields.get(tag)) {
          for (String code : df.getSubFields().keySet()) {
            sdoc.addField("marc_" + tag + code, df.getSubFields().get(code).get(0).getValue());
          }
        }
      }
    }

  }

}
