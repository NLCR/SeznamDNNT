package cz.inovatika.sdnnt.indexer.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.index.MD5;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.indexer.models.utils.MarcRecordUtils;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
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
  public String fmt;
  
  // hlavni, verejny stav
  public List<String> dntstav = new ArrayList<>();
  public List<String> previousDntstav = new ArrayList<>();
  public Date datum_stavu;
  
  @JsonIgnore
  public JSONArray historie_stavu = new JSONArray();
  @JsonIgnore
  public JSONArray historie_kurator_stavu = new JSONArray();
  @JsonIgnore
  public JSONArray historie_granulovaneho_stavu = new JSONArray();

  // kuratorsky stav
  public List<String> kuratorstav = new ArrayList<>();
  public List<String> previousKuratorstav = new ArrayList<>();
  public Date datum_krator_stavu;

  // masterlinks
  public JSONArray masterlinks;
  //masterlinksdisabled
  public Boolean masterLinksDisabled;
  
  // licence
  public String license;
  // historie licenci
  public List<String> licenseHistory;

  @JsonIgnore
  public MarcRecordFlags recordsFlags;

  @JsonIgnore
  public JSONArray granularity; 

  
  

  public boolean isDeleted = false;
  
  // followers
  public List<String> followers = new ArrayList<>();

  /** EUIPO stuf **/
  // idEuipo
  public List<String> idEuipo = new ArrayList<>();
  public List<String> idEuipoLastactive = new ArrayList<>();
  public List<String> idEuipoCanceled = new ArrayList<>();
  
  // exports ids
  public List<String> idEuipoExport = new ArrayList<>();

  // canceled exports ids
  public String idEuipoActiveExport = null;

  // exports facets - only make sure that facest is set during switching object
  public List<String> exportsFacets = new ArrayList<>();

  
  // digital librarires
  public List<String> digitalLibraries = new ArrayList<>();
  
  public Map<String, String> controlFields = new HashMap();

  public Map<String, List<DataField>> dataFields = new HashMap();
  //public SolrInputDocument sdoc = new SolrInputDocument();

  final public static List<String> tagsToIndex
          = Arrays.asList("015", "020", "022", "035", "040", "044", "100", "130", "240", "243",
                  "245", "246", "250", "260", "264", "338",
                  "700", "710", "711", "730",
                  "856", "990", "991", "992", "998", "956", "911", "910",
                  "996"
                  );

  public static MarcRecord fromRAWJSON(String json) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    MarcRecord mr = objectMapper.readValue(json, MarcRecord.class);
    return mr;
  }
  
  // do not use it; delete 
  public static MarcRecord fromDocDep(SolrDocument doc) throws JsonProcessingException {
    String rawJson = (String) doc.getFirstValue(RAW_FIELD);
    MarcRecord mr = fromRAWJSON(rawJson);
    // uff 
    //TODO: Chnage it 
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
                    FMT_FIELD+" "+
                    KURATORSTAV_FIELD+" "+
                    HISTORIE_STAVU_FIELD+" " +
                    HISTORIE_KURATORSTAVU_FIELD+" " +
                    HISTORIE_GRANULOVANEHOSTAVU_FIELD+" " +
                    DATUM_STAVU_FIELD+" "+
                    DATUM_KURATOR_STAV_FIELD+" "+
                    FLAG_PUBLIC_IN_DL+" "+
                    LICENSE_FIELD +" "+LICENSE_HISTORY_FIELD+" "+" "+FOLLOWERS+" "+" "+DIGITAL_LIBRARIES+" "+
                    GRANULARITY_FIELD+":[json]",
                    MASTERLINKS_FIELD+":[json]",
                    MASTERLINKS_DISABLED_FIELD,
                    ID_EUIPO,
                    ID_EUIPO_CANCELED,
                    ID_EUIPO_LASTACTIVE,
                    ID_EUIPO_EXPORT,
                    ID_EUIPO_EXPORT_ACTIVE,
                    
                    EXPORT

                    
                    );

    return fromIndex(client,q);
  }

  // testable method
  static MarcRecord fromIndex( SolrClient client, SolrQuery q) throws SolrServerException, IOException {

    SolrDocumentList dlist = client.query(DataCollections.catalog.name(), q).getResults();
    if (dlist.getNumFound() > 0) {
      SolrDocument doc = dlist.get(0);;
      //.get(0);
      MarcRecord mr = fromSolrDoc(doc);

      return mr;
    } else return null;
  }

  public static MarcRecord fromSolrDoc(SolrDocument doc) throws JsonProcessingException, JsonMappingException {
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

      if (doc.containsKey(HISTORIE_GRANULOVANEHOSTAVU_FIELD)) {
        mr.historie_granulovaneho_stavu =  new JSONArray(doc.getFieldValue(HISTORIE_GRANULOVANEHOSTAVU_FIELD).toString());
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


      if (doc.containsKey(HISTORIE_GRANULOVANEHOSTAVU_FIELD)) {
        JSONArray jsonArray = new JSONArray(doc.getFieldValue(HISTORIE_GRANULOVANEHOSTAVU_FIELD).toString());
        mr.historie_granulovaneho_stavu = jsonArray;
      }
      
      if (doc.containsKey(FMT_FIELD)) {
    	  mr.fmt = (String) doc.getFieldValue(FMT_FIELD);
      }

      // flags
      mr.recordsFlags = MarcRecordFlags.fromSolrDoc(doc);

      if (doc.containsKey(FOLLOWERS)) {
          List<String> collected = doc.getFieldValues(FOLLOWERS).stream().map(Object::toString).collect(Collectors.toList());
          mr.followers = collected;
      }
      
      if (doc.containsKey(DIGITAL_LIBRARIES)) {
          List<String> collected = doc.getFieldValues(DIGITAL_LIBRARIES).stream().map(Object::toString).collect(Collectors.toList());
          mr.digitalLibraries = collected;
      }

      /** granularity stuff */
      if (doc.containsKey(MASTERLINKS_FIELD)) {
          JSONArray ma = new JSONArray( doc.getFieldValue(MASTERLINKS_FIELD).toString());
          mr.masterlinks =  ma;
      }

      
      if (doc.containsKey(MASTERLINKS_DISABLED_FIELD)) {
          Boolean disabled = Boolean.valueOf(doc.getFieldValue(MASTERLINKS_DISABLED_FIELD).toString());
          mr.masterLinksDisabled =  disabled;
      }
      
      /** euipo stuff */
      if (doc.containsKey(ID_EUIPO)) {
          mr.idEuipo = (List<String>) doc.getFieldValue(ID_EUIPO);
      }


      /** euipo stuff */
      if (doc.containsKey(ID_EUIPO_CANCELED)) {
          mr.idEuipoCanceled = (List<String>) doc.getFieldValue(ID_EUIPO_CANCELED);
      }

      
      /** euipo stuff */
      if (doc.containsKey(ID_EUIPO_LASTACTIVE)) {
          mr.idEuipoLastactive = (List<String>) doc.getFieldValue(ID_EUIPO_LASTACTIVE);
      }
      
      if (doc.containsKey(ID_EUIPO_EXPORT)) {
          mr.idEuipoExport = (List<String>) doc.getFieldValue(ID_EUIPO_EXPORT);
      }

      if (doc.containsKey(ID_EUIPO_EXPORT_ACTIVE)) {
          mr.idEuipoActiveExport =  doc.getFirstValue(ID_EUIPO_EXPORT_ACTIVE).toString();
      }

      if (doc.containsKey(EXPORT)) {
          mr.exportsFacets = (List<String>) doc.getFieldValue(EXPORT);
      }

      return mr;
}

  // raw field json
  public JSONObject rawJSON() {
    JSONObject json = new JSONObject();
    json.put(IDENTIFIER_FIELD, identifier);
    json.put(DATESTAMP_FIELD, datestamp);
    json.put(SET_SPEC_FIELD, setSpec);
    json.put(LEADER_FIELD, leader);

    json.put("controlFields", controlFields);

    json.put("dataFields", dataFields);
    return json;
  }

  // TODO: remove
  public JSONObject toJSON() {
    JSONObject json = new JSONObject();
    json.put(IDENTIFIER_FIELD, identifier);
    json.put(DATESTAMP_FIELD, datestamp);
    json.put(SET_SPEC_FIELD, setSpec);
    json.put(LEADER_FIELD, leader);

    json.put(DNTSTAV_FIELD, dntstav);
    json.put(KURATORSTAV_FIELD, kuratorstav);
    json.put(HISTORIE_STAVU_FIELD, historie_stavu);
    json.put(HISTORIE_GRANULOVANEHOSTAVU_FIELD, historie_granulovaneho_stavu);
    json.put(GRANULARITY_FIELD, granularity);
    
    json.put(MASTERLINKS_FIELD, masterlinks);
    json.put(MASTERLINKS_DISABLED_FIELD, masterLinksDisabled);
    

    json.put("controlFields", controlFields);

    json.put("dataFields", dataFields);
    return json;
  }

  public String toXml(boolean onlyIdentifiers, boolean displaydeleted) {
    StringBuilder xml = new StringBuilder();
    
    if (!onlyIdentifiers) {

      xml.append("<metadata>");
      xml.append("<marc:record xmlns:marc=\"http://www.loc.gov/MARC21/slim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd\">");

      xml.append("<marc:leader>").append(leader).append("</marc:leader>");
      xml.append("<marc:controlfield tag=\"001\">").append(controlFields.get("001")).append("</marc:controlfield>");
      xml.append("<marc:controlfield tag=\"003\">").append(controlFields.get("003")).append("</marc:controlfield>");
      xml.append("<marc:controlfield tag=\"008\">").append(controlFields.get("008")).append("</marc:controlfield>");

      List<String> keys = new ArrayList<String>(dataFields.keySet());
      Collections.sort(keys);
      
      //Filtrujeme 956 -> granularita
      keys = keys.stream()
              .filter(x -> !"956".equals(x) && !"990".equals(x) && !"992".equals(x))
              .collect(Collectors.toList());

      for (Object key : keys) {
        for (DataField df : dataFields.get(key)) {
          xml.append("<marc:datafield tag=\"" + key + "\" ind1=\"" + df.ind1 + "\" ind2=\"" + df.ind2 + "\">");
          ArrayList<String> keys2 = new ArrayList<String>(df.subFields.keySet());
          ArrayList<SubField> subs = new ArrayList<SubField>();
          
          for (Object sk : keys2) {
            for (SubField sf : df.subFields.get(sk)) {
              subs.add(sf);
            }
          }
          Collections.sort(subs, new Comparator<SubField>(){
              @Override
              public int compare(
                SubField o1, SubField o2) {
                  return o1.index - o2.index;
              }
          });
          
          for (SubField sf : subs) {
              xml.append("<marc:subfield code=\"" + sf.code + "\" >")
                      .append(StringEscapeUtils.escapeXml(sf.value)).append("</marc:subfield>");
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
      
//      <marc:datafield tag="992" ind1=" " ind2=" ">
//<marc:subfield code="s">A</marc:subfield>
//<marc:subfield code="a">20200101</marc:subfield>
//<marc:subfield code="b">batch</marc:subfield>
//</marc:datafield>
//        String stav, date, user, comment, license;
      for (int i = 0; i < historie_stavu.length(); i++) {
        xml.append("<marc:datafield tag=\"992\" ind1=\" \" ind2=\" \">");
        JSONObject h = historie_stavu.getJSONObject(i);
          xml.append("<marc:subfield code=\"s\" >").append(h.optString("stav")).append("</marc:subfield>");
          xml.append("<marc:subfield code=\"a\" >").append(h.optString("date")).append("</marc:subfield>");
          xml.append("<marc:subfield code=\"b\" >").append(h.optString("user")).append("</marc:subfield>");
          if (h.has("comment")) {
            xml.append("<marc:subfield code=\"k\" >").append(h.optString("comment")).append("</marc:subfield>");
          }
          if (h.has("license")) {
            xml.append("<marc:subfield code=\"l\" >").append(h.optString("license")).append("</marc:subfield>");
          }
        xml.append("</marc:datafield>");
      }

      // master links 
      if (this.masterlinks != null && !this.masterLinksDisabled) {
          for (int i = 0; i<masterlinks.length(); i++) {
                  
              xml.append("<marc:datafield tag=\"956\" ind1=\" \" ind2=\" \">");
                JSONObject gr = masterlinks.getJSONObject(i);
                  xml.append("<marc:subfield code=\"u\" >").append(gr.optString("link")).append("</marc:subfield>");
                  if (this.dntstav != null && this.dntstav.size() > 0) {
                      String stav = this.dntstav.get(0);
                      xml.append("<marc:subfield code=\"9\" >").append(stav).append("</marc:subfield>");
                  }
              xml.append("</marc:datafield>");
          }
      }
      
      // Granularita
      if (granularity != null) {
        // u, 9, x, y
        for (int i = 0; i<granularity.length(); i++) {
        xml.append("<marc:datafield tag=\"956\" ind1=\" \" ind2=\" \">");
          JSONObject gr = granularity.getJSONObject(i);
            xml.append("<marc:subfield code=\"u\" >").append(gr.optString("link")).append("</marc:subfield>");
            if (gr.has("stav")) {
                xml.append("<marc:subfield code=\"9\" >").append(gr.get("stav")).append("</marc:subfield>");
            }
            if (gr.has("cislo")) {
              xml.append("<marc:subfield code=\"x\" >").append(gr.optString("cislo")).append("</marc:subfield>");
            }
            if (gr.has("rocnik")) {
              xml.append("<marc:subfield code=\"y\" >").append(gr.optString("rocnik")).append("</marc:subfield>");
            }
        xml.append("</marc:datafield>");
        }
      }

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
    
    MarcRecordUtils.derivedIdentifiers(identifier, sdoc);
    
    sdoc.setField(DATESTAMP_FIELD, datestamp);
    sdoc.setField(SET_SPEC_FIELD, setSpec);
    sdoc.setField(LEADER_FIELD, leader);
    sdoc.setField(RAW_FIELD, rawJSON().toString());

    sdoc.setField(HISTORIE_STAVU_FIELD, historie_stavu.toString());
    sdoc.setField(HISTORIE_KURATORSTAVU_FIELD, historie_kurator_stavu.toString());
    sdoc.setField(HISTORIE_GRANULOVANEHOSTAVU_FIELD, historie_granulovaneho_stavu.toString());

    sdoc.setField(LICENSE_FIELD, license);
    sdoc.setField(LICENSE_HISTORY_FIELD, licenseHistory);

    sdoc.setField(DATUM_STAVU_FIELD, datum_stavu);
    sdoc.setField(DATUM_KURATOR_STAV_FIELD, datum_krator_stavu);


    if (granularity != null) {
      if (sdoc.containsKey(GRANULARITY_FIELD)) {
        sdoc.removeField(GRANULARITY_FIELD);
      }
      for (int i = 0; i < granularity.length(); i++)
        sdoc.addField(GRANULARITY_FIELD, granularity.getJSONObject(i).toString());
    }

    if (masterlinks != null) {
        if (sdoc.containsKey(MASTERLINKS_FIELD)) {
          sdoc.removeField(MASTERLINKS_FIELD);
        }
        for (int i = 0; i < masterlinks.length(); i++) sdoc.addField(MASTERLINKS_FIELD, masterlinks.getJSONObject(i).toString());
    }
    
    if (masterLinksDisabled != null) {
        sdoc.addField(MASTERLINKS_DISABLED_FIELD, this.masterLinksDisabled);
    }
    

    // Control fields
    for (String cf : controlFields.keySet()) {
      sdoc.addField("controlfield_" + cf, controlFields.get(cf));
    }

    if (leader != null) {
      sdoc.setField(RECORD_STATUS_FIELD, leader.substring(5, 6));
      sdoc.setField(TYPE_OF_RESOURCE_FIELD, leader.substring(6, 7));
      sdoc.setField(ITEM_TYPE_FIELD, leader.substring(7, 8));

      MarcRecordUtilsToRefactor.setFMT(sdoc, leader.substring(6, 7), leader.substring(7, 8));
    }


    if (dntstav != null && !dntstav.isEmpty()) {
      sdoc.setField(DNTSTAV_FIELD, dntstav);
    } else {
      if (!sdoc.containsKey(DNTSTAV_FIELD)) {
        MarcRecordUtilsToRefactor.addStavFromMarc(sdoc, dataFields);
        this.dntstav = sdoc.getFieldValues(DNTSTAV_FIELD) != null ? new ArrayList(sdoc.getFieldValues(DNTSTAV_FIELD)) : null;
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
      collected.forEach(it -> sdoc.addField(SIGLA_FIELD, it));
    } else if (sdoc.containsKey(MARC_040_A)) {
      List<String> collected = sdoc.getFieldValues(MARC_040_A).stream().map(Object::toString).map(String::trim).collect(Collectors.toList());
      collected.forEach(it -> sdoc.addField(SIGLA_FIELD, it));
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
    	  // TODO:
      }
      try {
        sdoc.setField("date2_int", Integer.parseInt(date2));
      } catch (NumberFormatException ex) {
    	  // TODO:
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

    // store flags
    if (this.recordsFlags != null) this.recordsFlags.enhanceDoc(sdoc);
    
    // followers
    if (this.followers != null && !this.followers.isEmpty()) {
        followers.stream().forEach(f-> {
            sdoc.addField(FOLLOWERS, f);
        });
    }
    
    if (this.digitalLibraries != null && !this.digitalLibraries.isEmpty()) {
        digitalLibraries.stream().forEach(f-> {
            sdoc.addField(DIGITAL_LIBRARIES, f);
        });
    }

    // euipo stuff
    if (this.idEuipo != null && !this.idEuipo.isEmpty()) {
        Set<String> uniqSet = new LinkedHashSet<>(this.idEuipo);
        uniqSet.stream().forEach(id-> {
            sdoc.addField(ID_EUIPO, id);
        });
    }

    if (this.idEuipoCanceled != null && !this.idEuipoCanceled.isEmpty()) {
        Set<String> uniqSet = new LinkedHashSet<>(this.idEuipoCanceled);
        uniqSet.stream().forEach(id-> {
            sdoc.addField(ID_EUIPO_CANCELED, id);
        });
    }

    if (this.idEuipoLastactive != null && !this.idEuipoLastactive.isEmpty()) {
        Set<String> uniqSet = new LinkedHashSet<>(this.idEuipoLastactive);
        uniqSet.stream().forEach(id-> {
            sdoc.addField(ID_EUIPO_LASTACTIVE, id);
        });
    }

    
    if (this.idEuipoExport != null && !this.idEuipoExport.isEmpty()) {
        Set<String> uniqSet = new LinkedHashSet<>(this.idEuipoExport);
        uniqSet.stream().forEach(export-> {
            sdoc.addField(ID_EUIPO_EXPORT, export);
        });
    }
    
    if (this.exportsFacets != null && !this.exportsFacets.isEmpty()) {
        Set<String> uniqSet = new LinkedHashSet<>(this.exportsFacets);
        uniqSet.stream().forEach(export-> {
            sdoc.addField(EXPORT, export);
        });
    }

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


  public void setKuratorStav(String kstav, String pstav, String license, String user, String poznamka, JSONArray granularity) {
    CuratorItemState curatorItemState = CuratorItemState.valueOf(kstav);
    changedState( user, pstav, curatorItemState.name(), license, poznamka, granularity);
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


  private void changedState( String user, String publicState, String kuratorState, String license, String comment, JSONArray granularity) {
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
        case NL:
          this.license = License.dnntt.name();
        break;

        case NLX:
          if (licenseHistory == null) { licenseHistory = new ArrayList<>(); }
          if (this.license == null) {
            this.license = license;
          }
        break;

        case PX:
        // keep license
        break;
        case DX:
        // keep license
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
