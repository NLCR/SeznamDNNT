/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import cz.inovatika.sdnnt.index.utils.GranularityUtils;
import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.index.utils.SurviveFieldUtils;
import cz.inovatika.sdnnt.index.utils.torefactor.MarcRecordUtilsToRefactor;
import cz.inovatika.sdnnt.indexer.models.Import;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cz.inovatika.sdnnt.model.workflow.document.DocumentProxy;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.HistoryImpl;
import cz.inovatika.sdnnt.services.impl.SKCUpdateSupportServiceImpl;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class Indexer {

  public static final Logger LOGGER = Logger.getLogger(Indexer.class.getName());

  static List<String> dntSetFields = Arrays.asList("/dataFields/990", "/dataFields/992", "/dataFields/998");
  static List<String> identifierFields = Arrays.asList("/identifier", "/datestamp", "/setSpec",
          "/controlFields/001", "/controlFields/003", "/controlFields/005", "/controlFields/008");

  private static SolrClient solr;

  public static SolrClient getClient() {
    if (solr == null) {
      solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host", "http://localhost:8983/solr/")).build();
    }
    return solr;
  }

  public static void closeClient() {
    try {
      solr.close();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  
  public static JSONObject add(String collection, List<SolrInputDocument> recs, boolean merge, boolean update, String user) {
      return add(collection, recs, merge,update,user,getClient());
  }
  public static JSONObject add(String collection, List<SolrInputDocument> recs, boolean merge, boolean update, String user, SKCUpdateSupportServiceImpl updatesupportService) {
      return add(collection, recs, merge,update,user,getClient(), updatesupportService);
  }

  public static JSONObject add(String collection, List<SolrInputDocument> recs, boolean merge, boolean update, String user, SolrClient client) {
      return add(collection, recs, merge,update,user,client, null);
  }
  
  public static JSONObject add(String collection, List<SolrInputDocument> recs, boolean merge, boolean update, String user, SolrClient client, SKCUpdateSupportServiceImpl updatesupportService) {
    JSONObject ret = new JSONObject();
    try {
      if (update) {
        for (SolrInputDocument rec : recs) {
          SolrDocumentList docs = findById((String) rec.getFieldValue("identifier"), client);
          if (docs.getNumFound() == 0) {
            LOGGER.log(Level.INFO, "Record " + rec.getFieldValue("identifier") + " not found in catalog. It is new");
            client.add("catalog", rec);
          } else {
              LOGGER.log(Level.INFO, "Record " + rec.getFieldValue("identifier") + " found in catalog. Updating");
            String rawJSON =   (String) rec.getFieldValue("raw");
            SolrInputDocument hDoc = new SolrInputDocument();
            SolrInputDocument cDoc = mergeWithHistory(
                    (String) rec.getFieldValue("raw"),
                    docs.get(0), hDoc,
                    user, update, ret);
            if (cDoc != null) {
                SurviveFieldUtils.surviveFields(docs.get(0), cDoc);
                client.add("catalog", cDoc);
                client.add("history", hDoc);

                MarcRecord origJSON = MarcRecord.fromRAWJSON(rawJSON);
                MarcRecordUtilsToRefactor.marcFields(cDoc, origJSON.dataFields, MarcRecord.tagsToIndex);
            }

            
            if (updatesupportService != null && cDoc != null) {
                Object identifier = cDoc.getFieldValue(MarcRecordFields.IDENTIFIER_FIELD);
                if (cDoc.containsKey(MarcRecordFields.DNTSTAV_FIELD)) {
                    
                    // check format and place
                    Object fieldValue = cDoc.getFieldValue(MarcRecordFields.FMT_FIELD);
                    if (fieldValue == null || !Arrays.asList("BK","SE").contains(fieldValue.toString().trim().toUpperCase())) {
                        LOGGER.log(Level.INFO, "Record " + rec.getFieldValue("identifier") + " not BK or SE !");
                        updatesupportService.updateDeleteInfo(Arrays.asList(identifier.toString()));
                    }
                    
                    Object placeOfPub = cDoc.getFieldValue("place_of_pub");
                    if (placeOfPub == null || !placeOfPub.toString().trim().toLowerCase().equals("xr")) {
                        LOGGER.log(Level.INFO, "Record " + rec.getFieldValue("identifier") + " not xr !");
                        updatesupportService.updateDeleteInfo(Arrays.asList(identifier.toString()));
                    }
                }
            }
          }
        }
      } else if (merge) {
        for (SolrInputDocument rec : recs) {
          SolrDocumentList docs = find((String) rec.getFieldValue("raw"), client);
          if (docs == null) {

          } else if (docs.getNumFound() == 0) {
            LOGGER.log(Level.WARNING, "Record " + rec.getFieldValue("identifier") + " not found in catalog");
            ret.append("errors", "Record " + rec.getFieldValue("identifier") + " not found in catalog");
          } else if (docs.getNumFound() > 1) {
            LOGGER.log(Level.WARNING, "For" + rec.getFieldValue("identifier") + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
            ret.append("errors", "For" + rec.getFieldValue("identifier") + " found more than one record in catalog: " + docs.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
          }

          List<SolrInputDocument> hDocs = new ArrayList();
          List<SolrInputDocument> cDocs = new ArrayList();
          for (SolrDocument doc : docs) {
            SolrInputDocument hDoc = new SolrInputDocument();

            SolrInputDocument cDoc = mergeWithHistory(
                    (String) rec.getFieldValue("raw"),
                    doc, hDoc,
                    user, update, ret);
            
            // Nechame puvodni hodnoty "DNT" poli
            SurviveFieldUtils.surviveFields(doc, cDoc);
            
            if (cDoc != null) {
              hDocs.add(hDoc);
              cDocs.add(cDoc);
            }
          }

          if (!cDocs.isEmpty()) {
            client.add("history", hDocs);
            client.add("catalog", cDocs);
            hDocs.clear();
            cDocs.clear();
          }

        }
      } else {
          client.add(collection, recs);
      }
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ret;
  }

  // keepDNTFields = true => zmena vsech poli krome DNT (990, 992)
  // keepDNTFields = false => zmena pouze DNT (990, 992) poli
  static SolrInputDocument mergeWithHistory(String jsTarget,
          SolrDocument docCat, SolrInputDocument historyDoc,
          String user, boolean keepDNTFields, JSONObject ret) {

    try {
      String jsCat = (String) docCat.getFirstValue("raw");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(jsCat);

      // As target the sdnnt record
      JsonNode target = mapper.readTree(jsTarget);

      JsonNode fwPatch = JsonDiff.asJson(source, target);
      removeOpsForDNTFields(fwPatch, keepDNTFields);
      if (new JSONArray(fwPatch.toString()).length() > 0) {
        JsonNode bwPatch = JsonDiff.asJson(target, source);
        removeOpsForDNTFields(bwPatch, keepDNTFields);

        historyDoc.setField("identifier", docCat.getFirstValue("identifier"));
        historyDoc.setField("user", user);
        historyDoc.setField("type", "app");
        JSONObject changes = new JSONObject()
                .put("forward_patch", new JSONArray(fwPatch.toString()))
                .put("backward_patch", new JSONArray(bwPatch.toString()));
        historyDoc.setField("changes", changes.toString());

        // Create record in catalog
        MarcRecord mr = MarcRecord.fromRAWJSON(JsonPatch.apply(fwPatch, source).toString());

        return mr.toSolrDoc();
      } else {
        LOGGER.log(Level.FINE, "No changes detected in {0}", target.at("/identifier").asText());
        return null;
      }
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "error merging {0}", docCat.getFirstValue("identifier"));
      LOGGER.log(Level.SEVERE, null, ex);
      ret.append("errors", "error merging " + docCat.getFirstValue("identifier"));
      return null;
    }
  }

  public static SolrDocumentList findById(String identifier) {
      return findById(identifier, getClient());
  }

  public static SolrDocumentList findById(String identifier, SolrClient client) {
      // JSONObject ret = new JSONObject();
      try {
        SolrQuery query = new SolrQuery("*")
                .addFilterQuery("identifier:\"" + identifier + "\"")
                .setRows(1)
                .setFields("*");
        return client.query("catalog", query).getResults();

      } catch (SolrServerException | IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return null;
      }
    }
  public static SolrDocumentList find(String source, SolrClient client) {
      // JSONObject ret = new JSONObject();
      try {

        MarcRecord mr = MarcRecord.fromRAWJSON(source);
        SolrInputDocument sdoc = mr.toSolrDoc();
        String q = "(controlfield_001:\"" + sdoc.getFieldValue("controlfield_001") + "\""
                + " AND marc_040a:\"" + sdoc.getFieldValue("marc_040a") + "\""
                + " AND controlfield_008:\"" + sdoc.getFieldValue("controlfield_008") + "\")"
                + " OR marc_020a:\"" + sdoc.getFieldValue("marc_020a") + "\""
                + " OR marc_015a:\"" + sdoc.getFieldValue("marc_015a") + "\""
                + " OR dedup_fields:\"" + sdoc.getFieldValue("dedup_fields") + "\"";

        SolrQuery query = new SolrQuery(q)
                .setRows(20)
                .setFields("*,score");
        return client.query("catalog", query).getResults();
//        QueryRequest qreq = new QueryRequest(query);
//        NoOpResponseParser rParser = new NoOpResponseParser();
//        rParser.setWriterType("json");
//        qreq.setResponseParser(rParser);
//        NamedList<Object> qresp = solr.request(qreq, "catalog");
//        return new JSONObject((String) qresp.get("response"));

      } catch (SolrServerException | IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        return null;
      }
  }
  public static SolrDocumentList find(String source) {
      return find(source, getClient());
  }

  public static JSONObject approveInImport(String identifier, String newRaw, String user) {
    JSONObject ret = new JSONObject();
    try {

      Indexer.changeStav(identifier,  user);
      Import impNew = Import.fromJSON(newRaw);
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("id:\"" + impNew.id + "\"");
      Import impOld = getClient().query("imports", q).getBeans(Import.class).get(0);
      new HistoryImpl(getClient()).log(identifier, impOld.toJSONString(), impNew.toJSONString(), user, "import", null);

      // Update record in imports
      ret = Import.approve(impNew, identifier, user);

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  // zmeni viditelnost - musi but dntstav A nebo PA -> finalni dntstav A NZ
  public static JSONObject reduceVisbilityState(String identifier, String navrh, String user) throws IOException, SolrServerException {
    return reduceVisbilityState(identifier, navrh, user,  MarcRecord.fromIndex(identifier),getClient());
  }

  public static JSONObject reduceVisbilityState(String identifier, String navrh, String user,MarcRecord mr, SolrClient client) {
    throw new UnsupportedOperationException("unsupported");
//    JSONObject ret = new JSONObject();
//    try {
//      mr.toSolrDoc();
//      String oldRaw = mr.toJSON().toString();
//      List<String> oldStav = mr.dntstav;
//      DocumentWorkflow.valueOf(navrh).reduce(mr, user, (chanedRecord, ident, oldstates, newstates)->{
//
//        LOGGER.info(String.format("Changing state for '%s', old state %s, new state %s, document sync %s", ident, oldstates.toString(), newstates.toString(), new StringBuilder().append(chanedRecord.dntstav).append(":").append(chanedRecord.sdoc.getFieldValues(MarcRecordFields.DNTSTAV_FIELD))));
//
//        new HistoryImpl(client).log(identifier, oldRaw, mr.toJSON().toString(), user, "catalog");
//        try {
//          client.add("catalog", mr.sdoc);
//        } catch (SolrServerException | IOException ex) {
//          LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
//          ret.put("error", ex);
//        }
//      });
//    } finally {
//      SolrJUtilities.quietCommit(client, "catalog");
//    }
//    return ret;
  }


  /**
   * Posila email o zmenu stavu zaznamu podle nastaveni uzivatelu
   *
   * @return
   */
  public static Map<User, List<Map<String,String>>>  checkNotifications(UserController controler,  String interval) {

    try {
      List<User> usersByNotificationInterval = controler.findUsersByNotificationInterval(interval);
      for (User u :  usersByNotificationInterval) {
        LOGGER.log(Level.INFO, String.format("Sending notifications: %s for user  %s",interval, u.getJmeno()+" "+u.getPrijmeni()+"("+u.getEmail()));

      }

    } catch (UserControlerException e) {
      e.printStackTrace();
    }


    JSONObject ret = new JSONObject();

    String fqCatalog;
    String fqJoin = "{!join fromIndex=notifications from=identifier to=identifier} user:*";
    switch(interval) {
      case "den":
        fqCatalog = "datum_stavu:[NOW/DAY-1DAY TO NOW]";
        break;
      case "tyden":
        fqCatalog = "datum_stavu:[NOW/DAY-7DAYS TO NOW]";
        break;
      default:
        fqCatalog = "datum_stavu:[NOW/MONTH-1MONTH TO NOW]";
    }
    try {
      //Map<String, String> mails = new HashMap<>();

      Map<User, List<Map<String,String>>> mails = new HashMap<>();


      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      SolrQuery q = new SolrQuery("*").setRows(1000)
              .setSort("identifier", SolrQuery.ORDER.desc)
              .addFilterQuery(fqCatalog)
              .addFilterQuery(fqJoin)
              .setFields("identifier,datum_stavu,nazev,dntstav");
      boolean done = false;
      while (!done) {
        q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse qr = getClient().query("catalog", q);
        String nextCursorMark = qr.getNextCursorMark();
        SolrDocumentList docs = qr.getResults();


        for (SolrDocument doc : docs) {

          Collection<Object> dntstav = doc.getFieldValues("dntstav");
          Map<String,String> map = new HashMap<>();
          map.put("nazev", (String) doc.getFirstValue("nazev"));
          map.put("dntstav", dntstav.size() == 1 ? (String)new ArrayList<>(dntstav).get(0): dntstav.toString());
          map.put("identifier", doc.getFieldValue("identifier").toString());


          // Dotazujeme user
          SolrQuery qUser = new SolrQuery("*").setRows(1)
              .addFilterQuery("{!join fromIndex=notifications from=user to=username} identifier:\"" + doc.getFirstValue("identifier") + "\"");
          QueryResponse uResponse = getClient().query("users", qUser);
          List<User> users = uResponse.getResults().stream().map(User::fromSolrDocument).collect(Collectors.toList());
          for (User user : users) {
            // Pridame udaje o zaznamu pro uzivatel
            if (mails.containsKey(user)) {
              mails.get(user).add(map);
            } else {
              mails.put(user, new ArrayList<>(Arrays.asList(map)));
            }
          }
        }


        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
      }
      LOGGER.log(Level.INFO, "checkNotifications finished");
      return  mails;
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
      return new HashMap<>();
    }
  }

  //TODO: Move and do Junit tests
  public static JSONObject changeStavDirect(SolrClient solrClient, String identifier, String newStav, String licence, String poznamka, JSONArray granularityChange, String user) throws IOException, SolrServerException {
    JSONObject ret = new JSONObject();
    try {
      MarcRecord mr = MarcRecord.fromIndex(solrClient, identifier);
      List<String> previous = mr.kuratorstav;
      
      JSONObject before = mr.toJSON();
      // sync to solr doc
      SolrInputDocument sdoc = mr.toSolrDoc();
      CuratorItemState kstav = CuratorItemState.valueOf(newStav);
      PublicItemState pstav = kstav.getPublicItemState(new DocumentProxy(mr, null));
      if (pstav != null && pstav.equals(PublicItemState.A) || pstav.equals(PublicItemState.PA)) {
        mr.license = licence;
      } else if (pstav != null && pstav.equals(PublicItemState.NL)) {
        mr.license = License.dnntt.name();
      } else {
          // delete licenses
          mr.license = null;
          licence = null;
      }
      // zapis do historie
      mr.setKuratorStav(kstav.name(), pstav.name(), licence, user, poznamka, granularityChange);

      // musi se menit i v ramci granularity
      if (!granularityChange.isEmpty()) {
        JSONArray granularityFromIndex = mr.granularity;
        int commonIndex = Math.min(granularityFromIndex.length(), granularityChange.length());
        for (int i = 0; i < commonIndex; i++) {
          JSONObject fromParam = granularityChange.getJSONObject(i);
          JSONObject fromIndex = granularityFromIndex.getJSONObject(i);
          if (!GranularityUtils.eqGranularityObject(fromParam, fromIndex)) {
            String formatted = MarcRecord.FORMAT.format(new Date());
            mr.historie_granulovaneho_stavu.put(HistoryObjectUtils.historyObjectGranularityField(fromParam, user, poznamka, formatted));
          }
        }
        // pridano
        if (granularityChange.length() > commonIndex) {
          for (int i = commonIndex; i < granularityChange.length(); i++) {
            String formatted = MarcRecord.FORMAT.format(new Date());
            mr.historie_granulovaneho_stavu.put(HistoryObjectUtils.historyObjectGranularityField(granularityChange.getJSONObject(i), user, poznamka, formatted));
          }
        }
        
        // zmenit stavy granularity
        mr.setGranularity(granularityChange, poznamka, user);
      }

      ChangeProcessStatesUtility.granularityChange(mr, previous, kstav);
      
      
      solrClient.add("catalog", mr.toSolrDoc());
      new HistoryImpl(solrClient).log(mr.identifier, before.toString(), mr.toJSON().toString(), user, DataCollections.catalog.name(), null);
    } finally {
      SolrJUtilities.quietCommit(solrClient, "catalog");
    }
    return ret;
  }

  
  //TODO: Move and do Junit tests
  public static JSONObject changeStavDirect(String identifier, String newStav, String licence, String poznamka, JSONArray granularity, String user) throws IOException, SolrServerException {
    return changeStavDirect(getClient(), identifier, newStav, licence, poznamka, granularity, user);
  }

  //TODO: Move and do Junit tests
  public static JSONObject changeStav(String identifier, String user ) {
    return changeStav(identifier, user,getClient());
  }

  //TODO: Move and do Junit tests
  public static JSONObject changeStav(String identifier,  String user, SolrClient client) {
    try {
      MarcRecord mr = MarcRecord.fromIndex(identifier);
      return changeStav(identifier,  user, mr, client);
    } catch (SolrServerException | IOException e) {
      LOGGER.log(Level.SEVERE,e.getMessage(),e);
      return new JSONObject();
    }
  }

  public static JSONObject changeStav(String identifier,  String user, MarcRecord mr, SolrClient client ) {
    JSONObject ret = new JSONObject();
    try {
      // sync to solr doc
      mr.toSolrDoc();
      String oldRaw = mr.toJSON().toString();


//      // workflow
//      DocumentWorkflow.valueOf(navrh).change(mr, user,(changedRecord, ident, oldstates, newstates)->{
//        try {
//          LOGGER.info(String.format("Changing state for '%s', old state %s, new state %s, document sync %s", ident, oldstates.toString(), newstates.toString(),new StringBuilder().append(changedRecord.dntstav).append(":").append(changedRecord.sdoc.getFieldValues(MarcRecordFields.DNTSTAV_FIELD))));
//          new HistoryImpl(client).log(identifier, oldRaw, mr.toJSON().toString(), user, "catalog");
//          client.add("catalog", mr.sdoc);
//        } catch (SolrServerException| IOException ex) {
//          LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
//          ret.put("error", ex);
//        }
//      });
    } finally {
      SolrJUtilities.quietCommit(client,"catalog" );
    }
    return ret;
  }

  /**
   * Save record in catalog. Generates diff path and index to history core We
   * store the patch in both orders, forward and backwards
   *
   * @param id identifier in catalog
   * @param newRaw JSON representation of the record
   * @param user User
   * @return
   */
  public JSONObject save(String id, JSONObject newRaw, String user) {
    Options opts = Options.getInstance();
    JSONObject ret = new JSONObject();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:\"" + id + "\"")
              .setFields("raw");
      SolrDocument docOld = solr.query("catalog", q).getResults().get(0);
      String oldRaw = (String) docOld.getFirstValue("raw");

      new HistoryImpl(getClient()).log(id, oldRaw, newRaw.toString(), user, "catalog", null);

      // Update record in catalog
      MarcRecord mr = MarcRecord.fromRAWJSON(newRaw.toString());
      //mr.toSolrDoc();
      solr.add("catalog", mr.toSolrDoc());
      solr.commit("catalog");

      ret = new JSONObject(newRaw);
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public static JSONObject reindex(String dest, String filter, String collection, boolean cleanStav) {
    JSONObject ret = new JSONObject();
    int indexed = 0;
    try (SolrClient solr = new ConcurrentUpdateSolrClient.Builder(dest).build()) {
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      SolrQuery q = new SolrQuery("*").setRows(1000)
              .setSort("identifier", SolrQuery.ORDER.desc)
              .addFilterQuery(filter)
              .setFields("raw,dntstav,datum_stavu,license,license_history,historie_stavu");
      List<SolrInputDocument> idocs = new ArrayList<>();
      boolean done = false;
      while (!done) {
        q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse qr = getClient().query(collection, q);
        String nextCursorMark = qr.getNextCursorMark();
        SolrDocumentList docs = qr.getResults();
        for (SolrDocument doc : docs) {
          String oldRaw = (String) doc.getFirstValue("raw");

          MarcRecord mr = MarcRecord.fromRAWJSON(oldRaw);
          SolrInputDocument idoc = mr.toSolrDoc();
          if (!cleanStav) {
            idoc.addField("dntstav", doc.getFieldValue("dntstav"));
            idoc.addField("datum_stavu", doc.getFieldValue("datum_stavu"));
            idoc.addField("historie_stavu", doc.getFieldValue("historie_stavu"));
            idoc.addField("license", doc.getFieldValue("license"));
            idoc.addField("license_history", doc.getFieldValue("license_history"));
            idoc.addField("granularity", doc.getFieldValue("granularity"));
          } else {
            idoc.removeField("dntstav");
            idoc.removeField("datum_stavu");
            idoc.removeField("historie_stavu");
            idoc.removeField("license");
            idoc.removeField("license_history");
            idoc.removeField("granularity");
          }

          idocs.add(idoc);
        }

        if (!idocs.isEmpty()) {
          solr.add(collection, idocs);
          solr.commit(collection);
          indexed += idocs.size();
          idocs.clear();
          LOGGER.log(Level.INFO, "Curently reindexed: {0}", indexed);
        }
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
      }
      solr.commit(collection);
      solr.close();
      LOGGER.log(Level.INFO, "Reindex finished: {0}", indexed);
      ret.put("reindex", indexed);
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public static JSONObject reindexId(String id) {
    JSONObject ret = new JSONObject();
    try {
      SolrClient solr = getClient();
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:\"" + id + "\"")
              .setFields("raw");
      SolrDocument docOld = solr.query("catalog", q).getResults().get(0);
      String oldRaw = (String) docOld.getFirstValue("raw");

      // Update record in catalog
      MarcRecord mr = MarcRecord.fromRAWJSON(oldRaw);
      solr.add("catalog", mr.toSolrDoc());
      solr.commit("catalog");
      ret = mr.toJSON();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   * Compares record from DNT-ALL set (in sdnnt core) with SKC record (catalog
   * core)
   *
   * @param id
   * @return
   */
  public JSONObject compare(String id) {
    Options opts = Options.getInstance();
    JSONObject ret = new JSONObject();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:" + id)
              .setFields("marc_035a,raw");
      SolrDocument docDnt = solr.query("sdnnt", q).getResults().get(0);
      String sysno = (String) docDnt.getFirstValue("marc_035a");
      String jsDnt = (String) docDnt.getFirstValue("raw");
      sysno = sysno.substring(sysno.lastIndexOf(")") + 1);

      SolrQuery q2 = new SolrQuery("*").setRows(1)
              .addFilterQuery("controlfield_001:" + sysno)
              .setFields("raw");
      SolrDocument docCat = solr.query("catalog", q2).getResults().get(0);
      String jsCat = (String) docCat.getFirstValue("raw");
      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(jsCat);
      JsonNode target = mapper.readTree(jsDnt);

      JsonNode patch = JsonDiff.asJson(source, target);
      ret.put("diff", new JSONArray(patch.toString()));
      removeOpsForDNTFields(patch, false);
      ret.put("catalog", new JSONObject(jsCat));
      ret.put("sdnnt", new JSONObject(jsDnt));
      ret.put("patch", new JSONArray(patch.toString()));

      ret.put("catalog_new", new JSONObject(JsonPatch.apply(patch, source).toString()));
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   * Compares two records
   *
   * @param sourceRaw
   * @param targetRaw
   * @return
   */
  public static JSONObject compare(String sourceRaw, String targetRaw) {
    JSONObject ret = new JSONObject();
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(sourceRaw);
      JsonNode target = mapper.readTree(targetRaw);

      JsonNode patch = JsonDiff.asJson(source, target);
      removeOpsForIdenfiersFields(patch);
      ret.put("diff", new JSONArray(patch.toString()));
      // ret.put("patch", new JSONArray(patch.toString()));

    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   * Merges record from source core with SKC record (catalog core)
   *
   * @param sourceCore Core to merge from, i.e. sdnnt
   * @param id identifier in source core
   * @param user User
   * @return
   */
  public JSONObject mergeId(String sourceCore, String id, String user) {
    Options opts = Options.getInstance();
    JSONObject ret = new JSONObject();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:" + id)
              .setFields("marc_035a,raw");

      SolrDocumentList docs = solr.query(sourceCore, q).getResults();
      if (docs.getNumFound() == 0) {
        ret.put("error", "Record not found in sndt");
        return ret;
      }
      SolrDocument docDnt = docs.get(0);
      String sysno = (String) docDnt.getFirstValue("marc_035a");
      String jsDnt = (String) docDnt.getFirstValue("raw");
      sysno = sysno.substring(sysno.lastIndexOf(")") + 1);

      SolrQuery q2 = new SolrQuery("*").setRows(1)
              .addFilterQuery("controlfield_001:" + sysno)
              .setFields("identifier,raw");
      SolrDocumentList docsCat = solr.query("catalog", q2).getResults();
      if (docsCat.getNumFound() == 0) {
        ret.append("errors", "Record " + id + " not found in catalog");
        return ret;
      } else if (docsCat.getNumFound() > 1) {
        ret.append("errors", "Found more than one record in catalog: " + docsCat.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining()));
        return ret;
      } else {
        SolrDocument docCat = docsCat.get(0);
        String jsCat = (String) docCat.getFirstValue("raw");

        // addToHistory(id, jsCat, jsDnt, user, "app");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode source = mapper.readTree(jsCat);

        // As target the sdnnt record
        JsonNode target = mapper.readTree(jsDnt);
        JsonNode fwPatch = JsonDiff.asJson(source, target);
        removeOpsForDNTFields(fwPatch, false);
        JsonNode bwPatch = JsonDiff.asJson(target, source);
        removeOpsForDNTFields(bwPatch, false);

        ret.put("forward_patch", new JSONArray(fwPatch.toString()));
        ret.put("backward_patch", new JSONArray(bwPatch.toString()));

        // Insert in history
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField("identifier", id);
        idoc.setField("user", user);
        idoc.setField("type", "app");
        idoc.setField("changes", ret.toString());
        solr.add("history", idoc);
        solr.commit("history");

        // Update record in catalog
        //      MarcRecord mr = MarcRecord.fromRAWJSON(newRaw);
        //      mr.fillSolrDoc();
        //      solr.add("catalog", mr.toSolrDoc());
        //      solr.commit("catalog");
        ret.put("catalog_new", new JSONObject(JsonPatch.apply(fwPatch, source).toString()));
      }
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   * Merges records if source core with SKC records (catalog core)
   *
   * @param sourceCore Core to merge from, i.e. sdnnt
   * @param user User
   * @return
   */
  public JSONObject mergeCore(String sourceCore, String user) {
    return mergeCore(sourceCore, user, null);
  }

  public JSONObject mergeCore(String sourceCore, String user, String from) {
    long start = new Date().getTime();
    Options opts = Options.getInstance();
    JSONObject ret = new JSONObject();
    List<SolrInputDocument> hDocs = new ArrayList();
    List<SolrInputDocument> cDocs = new ArrayList();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      int indexed = 0;
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      SolrQuery q = new SolrQuery("*").setRows(1000)
              .setSort("identifier", SolrQuery.ORDER.asc)
              .setFields("identifier,raw");
      if (from != null) {
        q.addFilterQuery("datestamp:[" + from + " TO NOW]");
      }
      boolean done = false;
      List<SolrInputDocument> idocs = new ArrayList<>();
      while (!done) {
        q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse qr = solr.query(sourceCore, q);
        String nextCursorMark = qr.getNextCursorMark();
        SolrDocumentList docs = qr.getResults();
        for (SolrDocument doc : docs) {

          SolrInputDocument idoc = new SolrInputDocument();

          for (String name : doc.getFieldNames()) {
            idoc.addField(name, doc.getFieldValue(name));
          }
          idocs.add(idoc);

//            String oldRaw = (String) doc.getFirstValue("raw");
//            MarcRecord mr = MarcRecord.fromRAWJSON(oldRaw);
//            idocs.add(mr.toSolrDoc());
        }

        add("catalog", idocs, true, false, user);
        idocs.clear();
//        for (SolrDocument doc : docs)
//          // if (doc.getFirstValue("marc_035a") != null) {
//            SolrInputDocument hDoc = new SolrInputDocument();
//            SolrInputDocument cDoc = mergeWithHistory((String) doc.getFirstValue("raw"), doc, hDoc, user, false, ret);
//            if (cDoc != null) {
//              hDocs.add(hDoc);
//              cDocs.add(cDoc);
//            }
//          // }
//        }
//
//        if (!cDocs.isEmpty()) {
////          solr.add("history", hDocs);
//          solr.add("catalog", cDocs);
//          hDocs.clear();
//          cDocs.clear();
//        }
        indexed += docs.size();
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
        LOGGER.log(Level.INFO, "Current indexed: {0}", indexed);
      }
      // solr.commit("history");
      solr.commit("catalog");
      ret.put("indexed", indexed);
      String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
      ret.put("ellapsed", ellapsed);
      solr.close();
      LOGGER.log(Level.INFO, "mergecore FINISHED. Indexed {0} in {1}", new Object[]{ellapsed, indexed});
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   *
   * @param jsDnt
   * @param identifier
   * @param sysno
   * @param sigla
   * @param controlfield_008
   * @param dedup
   * @param user
   * @param historyDoc
   * @param ret
   * @return doc for catalog
   */
  private static SolrInputDocument mergeRaw2(String jsDnt,
          String identifier, String sysno, String sigla, String controlfield_008,
          String dedup,
          String user, SolrInputDocument historyDoc,
          JSONObject ret) {
    try {
      if (jsDnt == null) {
        LOGGER.log(Level.WARNING, "Record {0} has empty raw", identifier);
        return null;
      }
      SolrQuery q2 = new SolrQuery("*").setRows(1)
              .addFilterQuery("controlfield_001:\"" + sysno.substring(sysno.lastIndexOf(")") + 1) + "\"")
              .addFilterQuery("marc_040a:" + sigla)
              .addFilterQuery("controlfield_008:\"" + controlfield_008 + "\"")
              .setFields("identifier,raw");
      SolrDocumentList docsCat = getClient().query("catalog", q2).getResults();
      if (docsCat.getNumFound() == 0) {
        // ret.append("errors", "Record " + identifier + " with sysno: " + sysno + " not found in catalog");
        LOGGER.log(Level.WARNING, "Record {0} with sysno: {1} not found in catalog", new Object[]{identifier, sysno});
        return null;
      } else if (docsCat.getNumFound() > 1) {
        LOGGER.log(Level.WARNING, "Found more than one record in catalog: {0} -> {1}", new Object[]{identifier, docsCat.stream().map(d -> (String) d.getFirstValue("identifier")).collect(Collectors.joining())});
        // ret.append("errors", "Found more than one record in catalog: " + identifier + " with sysno: " + sysno);
        return null;
      }
      SolrDocument docCat = docsCat.get(0);
      String jsCat = (String) docCat.getFirstValue("raw");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(jsCat);

      // As target the sdnnt record
      JsonNode target = mapper.readTree(jsDnt);

      JsonNode fwPatch = JsonDiff.asJson(source, target);
      removeOpsForDNTFields(fwPatch, false);
      JsonNode bwPatch = JsonDiff.asJson(target, source);
      removeOpsForDNTFields(bwPatch, false);

//      ret.put("forward_patch", new JSONArray(fwPatch.toString()));
//      ret.put("backward_patch", new JSONArray(bwPatch.toString()));
      historyDoc.setField("identifier", docCat.getFirstValue("identifier"));
      historyDoc.setField("user", user);
      historyDoc.setField("type", "app");
      historyDoc.setField("changes", ret.toString());

      // Create record in catalog
      MarcRecord mr = MarcRecord.fromRAWJSON(JsonPatch.apply(fwPatch, source).toString());
      return mr.toSolrDoc();

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.append("errors", ex);
      return null;
    }
  }

  // keepDNTFields = true => zmena vsech poli krome DNT (990, 992)
  // keepDNTFields = false => zmena pouze DNT (990, 992) poli
  private static void removeOpsForDNTFields(Iterable jsonPatch, boolean keep) {
    Iterator<JsonNode> patchIterator = jsonPatch.iterator();
    while (patchIterator.hasNext()) {
      JsonNode patchOperation = patchIterator.next();
      // JsonNode operationType = patchOperation.get("op");
      JsonNode pathName = patchOperation.get("path");
      // if (operationType.asText().equals("replace") && ignoredFields.contains(pathName.asText())) {
      if (!dntSetFields.contains(pathName.asText()) && !keep) {
        patchIterator.remove();
      } else if (dntSetFields.contains(pathName.asText()) && keep) {
        patchIterator.remove();
      }
    }
  }

  private static void removeOpsForIdenfiersFields(Iterable jsonPatch) {
    Iterator<JsonNode> patchIterator = jsonPatch.iterator();
    while (patchIterator.hasNext()) {
      JsonNode patchOperation = patchIterator.next();
      JsonNode operationType = patchOperation.get("op");
      JsonNode pathName = patchOperation.get("path");
      // if (operationType.asText().equals("replace") && ignoredFields.contains(pathName.asText())) {
      if (identifierFields.contains(pathName.asText())) {
        patchIterator.remove();
      }
    }
  }

  public static JSONObject followRecord(String identifier, String user, NotificationInterval interval, boolean follow) {
    JSONObject ret = new JSONObject();
    try {
      if (follow) {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.addField("id", user + "_" + identifier);
        idoc.addField("identifier", identifier);
        idoc.addField("user", user);
        if (interval != null) {
          idoc.addField("periodicity", interval.name());
        } else {
          idoc.addField("periodicity", NotificationInterval.mesic.name());
        }
        getClient().add("notifications", idoc, 10);
      } else {
        getClient().deleteById("notifications", user + "_" + identifier, 10);
      }
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public JSONObject test(String sourceCore, String id) {
    Options opts = Options.getInstance();
    JSONObject ret = new JSONObject();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:" + id)
              .setFields("raw");

      SolrDocumentList docs = solr.query(sourceCore, q).getResults();
      if (docs.getNumFound() == 0) {
        ret.put("error", "Record not found in " + sourceCore);
        return ret;
      }
      SolrDocument docDnt = docs.get(0);
      String json = (String) docDnt.getFirstValue("raw");
      MarcRecord mr = MarcRecord.fromRAWJSON(json);
      // mr.fillSolrDoc();

      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(json);

      JsonNode target = mapper.readTree(mr.toJSON().toString());
      JsonNode fwPatch = JsonDiff.asJson(source, target);

      ret.put("diff", new JSONArray(fwPatch.toString()));
      ret.put("raw", mr.toJSON());

      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }
}
