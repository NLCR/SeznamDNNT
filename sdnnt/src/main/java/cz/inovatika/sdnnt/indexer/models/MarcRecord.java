/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.indexer.ISBN;
import cz.inovatika.sdnnt.indexer.MD5;
import cz.inovatika.sdnnt.indexer.RomanNumber;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class MarcRecord {
  
  public static final Logger LOGGER = Logger.getLogger(MarcRecord.class.getName());

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

  final public static List<String> tagsToIndex = Arrays.asList("015", "020", "022", "035", "040", "100", "245", "250", "260", "856", "990", "992", "998", "956");

  
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
      sdoc.setField("item_type", leader.substring(7, 8));
    }
    addDedup();
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
    addDedup();
  }
  
  public void addDedup() {
    sdoc.setField("dedup_fields", generateMD5());
  }

  private String generateMD5() {
    try {

      //ISBN
      String pole = (String) sdoc.getFieldValue("marc_020a");
      ISBN val = new ISBN();

      if (pole != null && !pole.equals("")) {
        //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
        if (!"".equals(pole) && val.isValid(pole)) {
          return MD5.generate(new String[]{pole});
        }
      }

      //ISSN
      pole = (String) sdoc.getFieldValue("marc_022a");
      if (pole != null && !pole.equals("")) {
        //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
        if (!"".equals(pole) && val.isValid(pole)) {
          return MD5.generate(new String[]{pole});
        }
      }

      //ccnb
      pole = (String) sdoc.getFieldValue("marc_015a");
      //logger.log(Level.INFO, "ccnb: {0}", pole);
      if (pole != null && !"".equals(pole)) {
        return MD5.generate(new String[]{pole});
      }

      //Check 245n číslo části 
      String f245n = "";
      String f245nraw = (String) sdoc.getFieldValue("marc_245n");
      if (f245nraw != null) {
        RomanNumber rn = new RomanNumber(f245nraw);
        if (rn.isValid()) {
          f245n = Integer.toString(rn.toInt());
        }
      }
 
      //Pole 250 údaj o vydání (nechat pouze numerické znaky) (jen prvni cislice)
      String f250a = (String) sdoc.getFieldValue("marc_250a");
      if (f250a != null) {
        f250a = onlyLeadNumbers(f250a);
      } 

      //Pole 100 autor – osobní jméno (ind1=1 →  prijmeni, jmeno; ind1=0 → jmeno, prijmeni.  
      //Obratit v pripade ind1=1, jinak nechat)
      String f100a = (String) sdoc.getFieldValue("marc_100a");
      if (dataFields.containsKey("100") && f100a != null) {
        String ind1 = dataFields.get("100").get(0).ind1;
        if ("1".equals(ind1) && !"".equals(f100a)) {
          String[] split = f100a.split(",", 2);
          if (split.length == 2) {
            f100a = split[1] + split[0];
          }
        }
      }

      if ("".equals(f100a)) {
        f100a = (String) sdoc.getFieldValue("marc_245c");
      }

      //vyber poli
      String uniqueCode = MD5.generate(new String[]{
        (String) sdoc.getFieldValue("marc_245a"),
        (String) sdoc.getFieldValue("marc_245b"),
        //map.get("245c"),
        f245n,
        (String) sdoc.getFieldValue("marc_245p"),
        f250a,
        f100a,
        (String) sdoc.getFieldValue("marc_110a"),
        (String) sdoc.getFieldValue("marc_111a"),
        (String) sdoc.getFieldValue("marc_260a"),
        (String) sdoc.getFieldValue("marc_260b"),
        onlyLeadNumbers((String) sdoc.getFieldValue("marc_260c"))
      });
      return uniqueCode;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }

  }

  private static String onlyLeadNumbers(String s) {
    if (s == null || "".equals(s)) {
      return s;
    }
    String retVal = "";
    int n = 0;
    while (n < s.length() && Character.isDigit(s.charAt(n))) {
      retVal += s.charAt(n);
      n++;
    }
    return retVal;
  }

}
