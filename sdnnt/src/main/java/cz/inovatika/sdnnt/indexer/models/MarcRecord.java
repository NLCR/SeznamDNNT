/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.index.ISBN;
import cz.inovatika.sdnnt.index.MD5;
import cz.inovatika.sdnnt.index.RomanNumber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.validator.routines.ISBNValidator;
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
  public Date datum_stavu;

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

  final public static List<String> tagsToIndex
          = Arrays.asList("015", "020", "022", "035", "040", "044", "100", "130", "240", "243",
                  "245", "246", "250", "260", "264", "338",
                  "700", "710", "711", "730",
                  "856", "990", "991", "992", "998", "956", "911", "910");

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

  public String toXml(boolean onlyIdentifiers) {
    StringBuilder xml = new StringBuilder();

    if (!onlyIdentifiers) {

      xml.append("<metadata>");
      xml.append("<marc:record xmlns:marc=\"http://www.loc.gov/MARC21/slim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">");

      xml.append("<marc:leader>").append(leader).append("</marc:leader>");
      xml.append("<marc:controlfield tag=\"001\">").append(controlFields.get("001")).append("</marc:controlfield>");
      xml.append("<marc:controlfield tag=\"003\">").append(controlFields.get("003")).append("</marc:controlfield>");
      xml.append("<marc:controlfield tag=\"008\">").append(controlFields.get("008")).append("</marc:controlfield>");

      ArrayList<String> keys = new ArrayList<String>(dataFields.keySet());
      Collections.sort(keys);
              
      for (Object key : keys) {
        for (DataField df : dataFields.get(key)) {
          //DataField df = dataFields.get(key);
          xml.append("<marc:datafield tag=\"" + key + "\" ind1=\"" + df.ind1 + "\" ind2=\"" + df.ind2 + "\">");
          ArrayList<String> keys2 = new ArrayList<String>(df.subFields.keySet());
          Collections.sort(keys2);
          for (Object sk : keys2) {
            for (SubField sf : df.subFields.get(sk)) {
              xml.append("<marc:subfield code=\"" + sk + "\" >")
                      .append(StringEscapeUtils.escapeXml(sf.value)).append("</marc:subfield>");
            }
          }
          xml.append("</marc:datafield>");
        }
      }

      xml.append("</marc:record>");
      xml.append("</metadata>");
    }

    return xml.toString();
  }
  
  public SolrInputDocument toSolrDoc(boolean force) {
    sdoc.clear();
    return toSolrDoc();
  }

  public SolrInputDocument toSolrDoc() {
    if (sdoc.isEmpty()) {
      fillSolrDoc();
    }
    sdoc.setField("identifier", identifier);
    sdoc.setField("datestamp", datestamp);
    sdoc.setField("setSpec", setSpec);
    sdoc.setField("leader", leader);
    sdoc.setField("raw", toJSON().toString());
    
    sdoc.setField("datum_stavu", datum_stavu);

    // Control fields
    for (String cf : controlFields.keySet()) {
      sdoc.addField("controlfield_" + cf, controlFields.get(cf));
    }
    
    sdoc.setField("record_status", leader.substring(5, 6));
    sdoc.setField("type_of_resource", leader.substring(6, 7));
    sdoc.setField("item_type", leader.substring(7, 8));
    
    setFMT(leader.substring(6, 7), leader.substring(7, 8));

    // https://www.loc.gov/marc/bibliographic/bd008a.html
    if (controlFields.containsKey("008") && controlFields.get("008").length() > 37) {
      sdoc.setField("language", controlFields.get("008").substring(35, 38));
      sdoc.setField("place_of_pub", controlFields.get("008").substring(15, 18));
      sdoc.setField("type_of_date", controlFields.get("008").substring(6, 7));
      String date1 = controlFields.get("008").substring(7, 11);
      String date2 = controlFields.get("008").substring(11, 15);
      sdoc.setField("date1", date1);
      sdoc.setField("date2", date2);
      try {
        sdoc.setField("date1_int", Integer.parseInt(date1));
      } catch (NumberFormatException ex) {
        
      }
      try {
        sdoc.setField("date2_int", Integer.parseInt(date2));
      } catch (NumberFormatException ex) {
        
      }
      
    }
    
    setIsProposable();

    sdoc.setField("title_sort", sdoc.getFieldValue("marc_245a"));
    String nazev = "";
    if (sdoc.containsKey("marc_245a")) {
      nazev += sdoc.getFieldValue("marc_245a") + " ";
    }
    if (sdoc.containsKey("marc_245b")) {
      nazev += sdoc.getFieldValue("marc_245b") + " ";
    }
    if (sdoc.containsKey("marc_245c")) {
      nazev += sdoc.getFieldValue("marc_245c") + " ";
    }
    sdoc.setField("nazev", nazev.trim());
    addRokVydani();

    return sdoc;
  }
  
  private void setFMT(String type_of_resource, String item_type) {
    // https://knowledge.exlibrisgroup.com/Primo/Product_Documentation/Primo/Technical_Guide/020Working_with_Normalization_Rules/100Validate_UNIMARC_FMT
    // Zmena POZOR. Podle url ai by mel byt BK, ale v alephu vidim SE
    String fmt = "BK";
    switch(type_of_resource) {
      case "a":
        if ("s".equals(item_type) || "i".equals(item_type)) {
          fmt = "SE";
        }
        break;
      case "c":
      case "d":
        fmt = "MU";
        break;
      case "e":
      case "f":
        fmt = "MP";
        break;
      case "g":
      case "k":
      case "r":
        fmt = "VM";
        break;
      case "i":
      case "j":
        fmt = "AM";
        break;
      case "l":
        fmt = "CF";
        break;
      case "m":
        fmt = "MX";
        break;
    }
    sdoc.setField("fmt", fmt);
  }
  
  private void setIsProposable() {
    
    // Pole podle misto vydani (xr ) a 338 a 245h
    boolean is_proposable = false;
    
    String place_of_pub = (String) sdoc.getFieldValue("place_of_pub");
    if ("xr ".equals(place_of_pub)) {
      if (sdoc.containsKey("marc_338a")) {
        String marc_338a = (String) sdoc.getFieldValue("marc_338a");
        String marc_338b = (String) sdoc.getFieldValue("marc_338b");
        String marc_3382 = (String) sdoc.getFieldValue("marc_3382");
        is_proposable = "svazek".equals(marc_338a) && "nc".equals(marc_338b) && "rdacarrier".equals(marc_3382);
      }  else {
        is_proposable = !sdoc.containsKey("marc_245h");
      }
    }
    sdoc.setField("is_proposable", is_proposable);
  }

  public void setStav(String new_stav) {
//    if (!dataFields.containsKey("990")) {
    List<DataField> ldf = new ArrayList<>();
    DataField df = new DataField("990", " ", " ");
    SubField sf = new SubField("a", new_stav);
    List<SubField> lsf = new ArrayList<>();
    lsf.add(sf);
    df.subFields.put("a", lsf);
    ldf.add(df);
    dataFields.put("990", ldf);
    datum_stavu = Calendar.getInstance().getTime();
//    } else {
//      dataFields.get("990").get(0).subFields.get("a").get(0).value = new_stav;
//    }
  }

  private void fillSolrDoc() {
    for (String tag : tagsToIndex) {
      if (dataFields.containsKey(tag)) {
        for (DataField df : dataFields.get(tag)) {
          for (String code : df.getSubFields().keySet()) {
            sdoc.addField("marc_" + tag + code, df.getSubFields().get(code).get(0).getValue());
          }
        }
      }
    }
//    if (!sdoc.containsKey("marc_990a")) {
//      // Nastavime stav na 'NNN' jako Nikdy NeZarazeno
//      sdoc.setField("marc_990a", "NNN");
//    }
    addDedup();
    addFRBR();
    addEAN();
  }

  private void addRokVydani() {
    if (sdoc.containsKey("marc_260c")) {
      for (Object s : sdoc.getFieldValues("marc_260c")) {
        String val = (String) s;
        val = val.replaceAll("\\[", "").replaceAll("\\]", "").trim();
        try {
          // je to integer. Pridame
          int r = Math.abs(Integer.parseInt(val));
          //Nechame jen 4
          if ((r + "").length() > 3) {
            String v = (r + "").substring(0, 4);
            sdoc.addField("rokvydani", v);
          }

          return;
        } catch (NumberFormatException ex) {

        }
        // casto maji 'c' nebo 'p' na zacatku c2001 
        if (val.startsWith("c") || val.startsWith("p")) {
          val = val.substring(1);
          try {
            // je to integer. Pridame
            int r = Integer.parseInt(val);
            sdoc.addField("rokvydani", r);
            return;
          } catch (NumberFormatException ex) {

          }
        }
        // [před r. 1937]
        if (val.startsWith("před r.")) {
          val = val.substring(7).trim();
          try {
            // je to integer. Pridame
            int r = Integer.parseInt(val);
            sdoc.addField("rokvydani", r);
            return;
          } catch (NumberFormatException ex) {

          }
        }

      }
    }
  }

  private void addEAN() {
    ISBNValidator isbn = ISBNValidator.getInstance();
    if (sdoc.containsKey("marc_020a")) {
      for (Object s : sdoc.getFieldValues("marc_020a")) {
        String ean = ((String) s);
        ean = isbn.validate(ean);
        if (ean != null) {
          // ean.replaceAll("-", "")
          // ean = ISBN.convertTo13(ean);
          sdoc.addField("ean", ean);
        }
      }
    }
  }

  public void addDedup() {

    try {

      //ISBN
      String pole = (String) sdoc.getFieldValue("marc_020a");
      ISBN val = new ISBN();

      if (pole != null && !pole.equals("")) {
        //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
        if (!"".equals(pole) && val.isValid(pole)) {
          sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
        }
      }

      //ISSN
      pole = (String) sdoc.getFieldValue("marc_022a");
      if (pole != null && !pole.equals("")) {
        //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
        if (!"".equals(pole) && val.isValid(pole)) {
          sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
        }
      }

      //ccnb
      pole = (String) sdoc.getFieldValue("marc_015a");
      //logger.log(Level.INFO, "ccnb: {0}", pole);
      if (pole != null && !"".equals(pole)) {
        sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
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
        (String) sdoc.getFieldValue("marc_245a"), // main title
        (String) sdoc.getFieldValue("marc_245b"), // subtitle
        //map.get("245c"),
        f245n,
        (String) sdoc.getFieldValue("marc_245p"),
        f250a,
        f100a,
        (String) sdoc.getFieldValue("marc_110a"),
        (String) sdoc.getFieldValue("marc_111a"),
        (String) sdoc.getFieldValue("marc_260a"),
        (String) sdoc.getFieldValue("marc_260b"),
        onlyLeadNumbers((String) sdoc.getFieldValue("marc_260c")),
        (String) sdoc.getFieldValue("marc_264a"),
        (String) sdoc.getFieldValue("marc_264b"),
        onlyLeadNumbers((String) sdoc.getFieldValue("marc_264c"))
      });
      sdoc.setField("dedup_fields", uniqueCode);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

  }

  public void addFRBR() {
    // Podle 	Thomas Butler Hickey
    // https://www.oclc.org/content/dam/research/activities/frbralgorithm/2009-08.pdf
    // https://text.nkp.cz/o-knihovne/odborne-cinnosti/zpracovani-fondu/informativni-materialy/bibliograficky-popis-elektronickych-publikaci-v-siti-knihoven-cr
    // strana 42
    // https://www.nkp.cz/soubory/ostatni/vyda_cm26.pdf

    /*
    Pole 130, 240 a 730 pro unifikovaný název a podpole názvových údajů v polích 700, 710 a
711 umožní po doplnění formátu MARC 21 nebo zavedení nového formátu vygenerovat
alespoň částečné údaje o díle a vyjádření.
Nové pole 337 (společně s poli 336 a 338, která jsou součástí Minimálního záznamu)
nahrazuje dosavadní podpole 245 $h.
    
    https://is.muni.cz/th/xvt2x/Studie_FRBR.pdf
    strana 100
    
    FRBR Tool – příklad mapování entit a polí MARC21
Author: Dreiser, Theodore, 1871 (Field 100) [Work]
Work: Sister Carrie (Field 240 )
Form: text - English LDR/06 + 008/35-37) [Expression]
Edition: 2nd ed. (Field 250) [Manifestation]
    
     */
    String frbr = "";

    //Ziskame title part
    String title = getFieldPart("240", "adgknmpr");
    if (title.isBlank()) {
      title = getFieldPart("245", "adgknmpr");
    }
    if (title.isBlank()) {
      title = getFieldPart("246", "adgknmpr");
    }
    String authorPart = getAuthorPart("100", "bcd") + getAuthorPart("110", "bcd") + getAuthorPart("111", "bcdnq");

    if (!authorPart.isBlank()) {
      frbr = authorPart + "/" + title;
    } else if (sdoc.containsKey("marc_130a")) {
      //Else if a 130 exists then the title alone is a sufficient key
      frbr = (String) sdoc.getFieldValue("marc_130a");
    } else if (sdoc.containsKey("marc_700a") && !(sdoc.containsKey("marc_700t") || sdoc.containsKey("marc_700k"))) {
      // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
      // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name 
      frbr = "/" + title + "/" + getFieldPart("700", "abcdq");
    } else if (sdoc.containsKey("marc_710a") && !(sdoc.containsKey("marc_710t") || sdoc.containsKey("marc_710k"))) {
      // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
      // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name 
      frbr = "/" + title + "/" + getFieldPart("710", "abcdq");
    } else if (sdoc.containsKey("marc_711a") && !(sdoc.containsKey("marc_711t") || sdoc.containsKey("marc_711k"))) {
      // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
      // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name 
      frbr = "/" + title + "/" + getFieldPart("711", "abcdq");
    } else {
      // Else add the oclc number to the title to make the key unique
      frbr = "/" + title + "/" + (String) sdoc.getFieldValue("controlfield_001");
    }
    sdoc.setField("frbr", MD5.normalize(frbr));
  }

  private String getAuthorPart(String tag, String codes) {
    String author = "";
    if (sdoc.containsKey("marc_" + tag + "a")) {
      // If an author exists, combine it with a title 
      String ind1 = dataFields.get(tag).get(0).ind1;
      String f = dataFields.get(tag).get(0).subFields.get("a").get(0).value;
      if ("1".equals(ind1)) {
        String[] split = f.split(",", 2);
        if (split.length == 2) {
          author = split[1] + split[0];
        }
      } else {
        author = f;
      }
    }
    for (char code : codes.toCharArray()) {
      if (sdoc.containsKey("marc_" + tag + code)) {
        author += "|" + (String) sdoc.getFieldValue("marc_" + tag + code);
      }
    }
    return author;
  }

  private String getFieldPart(String tag, String codes) {
    String s = "";
    if (dataFields.containsKey(tag)) {
      for (char code : codes.toCharArray()) {
        if (sdoc.containsKey("marc_" + tag + code)) {
          s += "|" + (String) sdoc.getFieldValue("marc_" + tag + code);
        }
      }
    }
    return s;
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
