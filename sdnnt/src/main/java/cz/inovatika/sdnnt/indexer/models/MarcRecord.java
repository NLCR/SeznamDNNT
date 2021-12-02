package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.index.Indexer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

import java.text.SimpleDateFormat;

/**
 *
 * @author alberto
 */

//TODO: Must be rewritten as soon as possible;
//TODO: sdoc vs marcrecord - synchronization problems
public class MarcRecord {

  public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd");

  public static final Logger LOGGER = Logger.getLogger(MarcRecord.class.getName());

  // Header fields
  public String identifier;
  public String datestamp;
  public String setSpec;
  public String leader;

  // hlavni, verejny stav
  public List<String> dntstav = new ArrayList<>();
  public Date datum_stavu;

  @JsonIgnore
  public JSONArray historie_stavu = new JSONArray();

  // kuratorsky stav
  public List<String> kuratorstav = new ArrayList<>();
  public Date datum_krator_stavu;

  @JsonIgnore
  public JSONArray historie_kurator_stavu = new JSONArray();

  public String license;
  public List<String> licenseHistory;


  // for standaalone
  //public Date workflowDeadline;

  @JsonIgnore
  public JSONArray granularity; 


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
  //public SolrInputDocument sdoc = new SolrInputDocument();

  final public static List<String> tagsToIndex
          = Arrays.asList("015", "020", "022", "035", "040", "044", "100", "130", "240", "243",
                  "245", "246", "250", "260", "264", "338",
                  "700", "710", "711", "730",
                  "856", "990", "991", "992", "998", "956", "911", "910");

  public static MarcRecord fromRAWJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    MarcRecord mr = objectMapper.readValue(json, MarcRecord.class);

    return mr;
  }
  
  public static MarcRecord fromDoc(SolrDocument doc) throws JsonProcessingException {
    String rawJson = (String) doc.getFirstValue(RAW_FIELD);
    MarcRecord mr = fromRAWJSON(rawJson);
    // uff
    mr.toSolrDoc();
    MarcRecordUtilsToRefactor.syncFromDoc(doc, mr);
    return mr;
  }

  public static MarcRecord fromIndex(String identifier) throws JsonProcessingException, SolrServerException, IOException {
        return fromIndex(Indexer.getClient(),identifier);
  }
  public static MarcRecord fromIndex( SolrClient client, String identifier) throws JsonProcessingException, SolrServerException, IOException {
    SolrQuery q = new SolrQuery("*").setRows(1)
            .addFilterQuery(IDENTIFIER_FIELD+":\"" + identifier + "\"")
            .setFields(RAW_FIELD+" "+
                    DNTSTAV_FIELD+" "+
                    KURATORSTAV_FIELD+" "+
                    HISTORIE_STAVU_FIELD+" " +
                    HISTORIE_KURATORSTAVU_FIELD+" " +
                    DATUM_STAVU_FIELD+" "+
                    DATUM_KURATOR_STAV_FIELD+" "+
                    LICENSE_FIELD +" "+LICENSE_HISTORY_FIELD+" "+ GRANULARITY_FIELD+":[json]");
    return fromIndex(client,q);
  }

  // testable method
  static MarcRecord fromIndex( SolrClient client, SolrQuery q) throws SolrServerException, IOException {

    SolrDocumentList dlist = client.query("catalog", q).getResults();
    if (dlist.getNumFound() > 0) {
      SolrDocument doc = dlist.get(0);;
      //.get(0);
      String json = (String) doc.getFirstValue(RAW_FIELD);
      ObjectMapper objectMapper = new ObjectMapper();
      MarcRecord mr = objectMapper.readValue(json, MarcRecord.class);

      if (doc.containsKey(LEGACY_STAV_FIELD)) {
        mr.dntstav =  doc.getFieldValues(LEGACY_STAV_FIELD).stream().map(Object::toString).collect(Collectors.toList());
      } else {
        mr.dntstav = new ArrayList<>();
      }
      if (doc.containsKey(KURATORSTAV_FIELD)) {
        mr.kuratorstav = doc.getFieldValues(KURATORSTAV_FIELD).stream().map(Object::toString).collect(Collectors.toList());
      }

      if (doc.containsKey(DATUM_STAVU_FIELD)) {
        mr.datum_stavu = (Date) doc.getFirstValue(DATUM_STAVU_FIELD);
      }

      if (doc.containsKey(DATUM_KURATOR_STAV_FIELD)) {
        mr.datum_krator_stavu = (Date) doc.getFirstValue(DATUM_KURATOR_STAV_FIELD);
      }

      if (doc.containsKey(HISTORIE_STAVU_FIELD)) {
        mr.historie_stavu =  new JSONArray(doc.getFieldValue(HISTORIE_STAVU_FIELD).toString());
      } else {
        mr.historie_stavu = new JSONArray();
      }

      if (doc.containsKey(HISTORIE_KURATORSTAVU_FIELD)) {
        mr.historie_kurator_stavu =  new JSONArray(doc.getFieldValue(HISTORIE_KURATORSTAVU_FIELD).toString());
      } else if (doc.containsKey(HISTORIE_STAVU_FIELD)){
        mr.historie_kurator_stavu = new JSONArray(doc.getFieldValue(HISTORIE_STAVU_FIELD).toString());
      } else {
        mr.historie_stavu = new JSONArray();
      }

      if (doc.containsKey(LICENSE_FIELD)) {
        mr.license = (String) doc.getFirstValue(LICENSE_FIELD);
      }
      mr.licenseHistory = doc.getFieldValues(LICENSE_HISTORY_FIELD) != null ? doc.getFieldValues(LICENSE_HISTORY_FIELD).stream().map(Object::toString).collect(Collectors.toList()): new ArrayList<>();


      if (doc.containsKey(GRANULARITY_FIELD)) {
        JSONArray ja = new JSONArray( doc.getFieldValue(GRANULARITY_FIELD).toString());
        mr.granularity =  ja;
      } else {
        // mr.granularity = new JSONArray();
      }
      return mr;
    } else return null;
  }

  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    json.put(IDENTIFIER_FIELD, identifier);
    json.put(DATESTAMP_FIELD, datestamp);
    json.put(SET_SPEC_FIELD, setSpec);
    json.put(LEADER_FIELD, leader);

    json.put(DNTSTAV_FIELD, dntstav);
    json.put(KURATORSTAV_FIELD, kuratorstav);
    json.put(HISTORIE_STAVU_FIELD, historie_stavu);


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
      
      // Pridame dnt pole
      xml.append("<marc:datafield tag=\"990\" ind1=\" \" ind2=\" \">");
      
      for (String sk : dntstav) {
          xml.append("<marc:subfield code=\"a\" >").append(sk).append("</marc:subfield>");
      }
      xml.append("</marc:datafield>");

      xml.append("</marc:record>");
      xml.append("</metadata>");
    }

    return xml.toString();
  }


  public SolrInputDocument toSolrDoc() {
    SolrInputDocument sdoc = new SolrInputDocument();
    if (sdoc.isEmpty()) {
      MarcRecordUtilsToRefactor.fillSolrDoc(sdoc, this.dataFields, tagsToIndex);
    }
    sdoc.setField(IDENTIFIER_FIELD, identifier);
    sdoc.setField(DATESTAMP_FIELD, datestamp);
    sdoc.setField(SET_SPEC_FIELD, setSpec);
    sdoc.setField(LEADER_FIELD, leader);
    sdoc.setField(RAW_FIELD, toJSON().toString());

    sdoc.setField(HISTORIE_STAVU_FIELD, historie_stavu.toString());
    sdoc.setField(HISTORIE_KURATORSTAVU_FIELD, historie_kurator_stavu.toString());

    sdoc.setField(LICENSE_FIELD, license);
    sdoc.setField(LICENSE_HISTORY_FIELD, licenseHistory);

    sdoc.setField(DATUM_STAVU_FIELD, datum_stavu);
    sdoc.setField(DATUM_KURATOR_STAV_FIELD, datum_krator_stavu);

    if (granularity != null ) {
      if (sdoc.containsKey(GRANULARITY_FIELD)) {
        sdoc.removeField(GRANULARITY_FIELD);
      }
      for (int i = 0; i<granularity.length(); i++)  sdoc.addField(GRANULARITY_FIELD, granularity.getJSONObject(i).toString());
    }
    
    // Control fields
    for (String cf : controlFields.keySet()) {
      sdoc.addField("controlfield_" + cf, controlFields.get(cf));
    }

    sdoc.setField(RECORD_STATUS_FIELD, leader.substring(5, 6));
    sdoc.setField(TYPE_OF_RESOURCE_FIELD, leader.substring(6, 7));
    sdoc.setField(ITEM_TYPE_FIELD, leader.substring(7, 8));

    MarcRecordUtilsToRefactor.setFMT(sdoc, leader.substring(6, 7), leader.substring(7, 8));

    if (dntstav != null && !dntstav.isEmpty()) {
      sdoc.setField(DNTSTAV_FIELD, dntstav);
    } else {
      if (!sdoc.containsKey(DNTSTAV_FIELD)) {
          MarcRecordUtilsToRefactor.addStavFromMarc(sdoc, dataFields);
          this.dntstav = sdoc.getFieldValues(DNTSTAV_FIELD) != null  ?  new ArrayList(sdoc.getFieldValues(DNTSTAV_FIELD)) : null;
      }
    }

    if (kuratorstav != null && !kuratorstav.isEmpty()) {
      sdoc.setField(KURATORSTAV_FIELD, kuratorstav);
    } else {
      if (!sdoc.containsKey(KURATORSTAV_FIELD)) {
        sdoc.setField(KURATORSTAV_FIELD, dntstav);
        sdoc.setField(HISTORIE_KURATORSTAVU_FIELD, historie_stavu.toString());
      }
    }

    if (sdoc.containsKey(MARC_264_B)) {
      String val = (String) sdoc.getFieldValue(MARC_264_B);
      sdoc.setField(NAKLADATEL_FIELD, nakladatelFormat(val));
    } else if (sdoc.containsKey(MARC_260_B)) {
      String val = (String) sdoc.getFieldValue(MARC_260_B);
      sdoc.setField(NAKLADATEL_FIELD, nakladatelFormat(val));
    }

    if (sdoc.containsKey(MARC_910_A)) {
      List<String> collected = sdoc.getFieldValues(MARC_910_A).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
      collected.forEach(it-> sdoc.addField(SIGLA_FIELD, it));
    } else if (sdoc.containsKey(MARC_040_A)) {
      List<String> collected = sdoc.getFieldValues(MARC_040_A).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
      collected.forEach(it-> sdoc.addField(SIGLA_FIELD, it));
    }

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

    MarcRecordUtilsToRefactor.setIsProposable(sdoc);

    sdoc.setField("title_sort", sdoc.getFieldValue("marc_245a"));

    //245a (název): 245b (podnázev). 245n (číslo dílu/části), 245p (název části/dílu) / 245c (autoři, překlad, ilustrátoři apod.)
    String nazev = "";
    if (sdoc.containsKey("marc_245a")) {
      nazev += sdoc.getFieldValue("marc_245a") + " ";
    }
    if (sdoc.containsKey("marc_245b")) {
      nazev += sdoc.getFieldValue("marc_245b") + " ";
    }
    if (sdoc.containsKey("marc_245p")) {
      nazev += sdoc.getFieldValue("marc_245p") + " ";
    }
    if (sdoc.containsKey("marc_245c")) {
      nazev += sdoc.getFieldValue("marc_245c") + " ";
    }
    if (sdoc.containsKey("marc_245i")) {
      nazev += sdoc.getFieldValue("marc_245i") + " ";
    }
    if (sdoc.containsKey("marc_245n")) {
      nazev += sdoc.getFieldValue("marc_245n") + " ";
    }
    sdoc.setField("nazev", nazev.trim());
    MarcRecordUtilsToRefactor.addRokVydani(sdoc);

    return sdoc;
  }

  public static String nakladatelFormat(String val) {
    if (val != null) {
      String trimmed = val.trim();
      AtomicReference<String> retVal = new AtomicReference(trimmed);
      Arrays.asList(";", ",", ":").forEach(postfix-> {
        if (trimmed.endsWith(postfix)) {
          retVal.set(trimmed.substring(0, trimmed.length()-1));
        }
      });
      return retVal.get().trim();
    } else return null;
  }

  //  // pouze pro reducki viditelnosti
//  public void enhanceState(List<String> newStates, String user) {
//    List<String> statesArray = this.dntstav != null ? new ArrayList<>(this.dntstav) : new ArrayList<>();
//    statesArray.addAll(newStates);
//    changedState( user, statesArray, null, newStates.toArray(new String[newStates.size()]));
//    // sync solr doc
//    toSolrDoc();
//  }
//
//  public void enhanceState(String newState, String user) {
//    List<String> statesArray = this.dntstav != null ? new ArrayList<>(this.dntstav) : new ArrayList<>();
//    statesArray.add(newState);
//    changedState( user, statesArray,null,newState);
//    // sync solr doc
//    toSolrDoc();
//  }

  public void setKuratorStav(String kstav, String pstav, String license, String user, String poznamka) {
    CuratorItemState curatorItemState = CuratorItemState.valueOf(kstav);
    changedState( user, pstav, curatorItemState.name(), license, poznamka);
    toSolrDoc();
  }

  public void setLicense(String l)  {
    String oldLicense = this.license;
    this.license = l;
    if (licenseHistory == null) {
      licenseHistory = new ArrayList<>();
    }
    licenseHistory.add(oldLicense);
  }

  public void setGranularity(JSONArray granularity, String comment, String user) {
    this.granularity = granularity;
  }


  private void changedState( String user, String publicState, String kuratorState, String license, String comment) {
    toSolrDoc();
    Date now = Calendar.getInstance().getTime();
    if (this.dntstav == null || (publicState != null && !this.dntstav.isEmpty())) {
      this.dntstav = Arrays.asList(publicState);
      this.datum_stavu = now;
      JSONObject h = new JSONObject().put("stav", publicState).
              put("date", FORMAT.format(datum_stavu)).
              put("user", user).
              put("comment", comment);

      if (license != null) {  h.put("license", license); }
      this.historie_stavu.put(h);

    }

    this.kuratorstav = Arrays.asList(kuratorState);
    this.datum_krator_stavu = now;

    JSONObject kh = new JSONObject().put("stav", kuratorState).put("date", FORMAT.format(datum_krator_stavu)).put("user", user).put("comment", comment);
    if (license != null) {  kh.put("license", license); }

    this.historie_kurator_stavu.put(kh);

    changeLicenseIfNeeded(kuratorState, license);
  }

  private void changeLicenseIfNeeded(String nState, String license) {
    CuratorItemState cstate = CuratorItemState.valueOf(nState);
    String oldLicense = this.license;
      switch (cstate) {
        case PA:
        case A:
          if (licenseHistory == null) { licenseHistory = new ArrayList<>(); }
          if (this.license != null) {
              this.license = license;
          } else {
              this.license = License.dnnto.name();
          }
        break;

        case NLX:
          if (licenseHistory == null) { licenseHistory = new ArrayList<>(); }
          if (this.license == null) {
            this.license = license;
          }
        break;

        default:
          if (licenseHistory == null) {  licenseHistory = new ArrayList<>(); }
          if (this.license != null) {
            licenseHistory.add(oldLicense);
            this.license = null;
          }
        break;
      }
  }


}
