/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer;

import cz.inovatika.sdnnt.Options;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import static cz.inovatika.sdnnt.indexer.OAIHarvester.LOGGER;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
  
  List<String> dntSetFields = Arrays.asList("/dataFields/990","/dataFields/992","/dataFields/998","/dataFields/956","/dataFields/856");

  JSONObject ret = new JSONObject();

  public JSONObject updateByQuery(String query, String newValue) {
    Options opts = Options.getInstance();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {

      solr.commit("sdnnt");
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }
  
  /**
   * Save record in catalog. Generates diff path and index to history core
   * We store the patch in both orders, forward and backwards
   * @param id identifier in catalog
   * @param newRaw JSON representation of the record 
   * @param user User 
   * @return 
   */
  public JSONObject save(String id, JSONObject newRaw, String user) {
    Options opts = Options.getInstance();
    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      SolrQuery q = new SolrQuery("*").setRows(1)
              .addFilterQuery("identifier:" + id)
              .setFields("raw");
      SolrDocument docOld = solr.query("catalog", q).getResults().get(0);
      String oldRaw = (String) docOld.getFirstValue("raw");

      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(oldRaw);
      JsonNode target = mapper.readTree(newRaw.toString());

      JsonNode fwPatch = JsonDiff.asJson(source, target);
      JsonNode bwPatch = JsonDiff.asJson(target, source);
      
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
//      MarcRecord mr = MarcRecord.fromJSON(newRaw);      
//      mr.fillSolrDoc();
//      solr.add("catalog", mr.toSolrDoc());
//      solr.commit("catalog");
      
      //ret.put("newRecord", new JSONObject(JsonPatch.apply(fwPatch, source).toString()));
      //ret.put("newRaw", mr.toJSON());
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  /**
   * Compares record from DNT-ALL set (in sdnnt core) with SKC record (catalog core)
   * @param id
   * @return 
   */
  public JSONObject compare(String id) {
    Options opts = Options.getInstance();
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
      removeReplaceOpsForIgnoredFields(patch);
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
   * Merges record from source core with SKC record (catalog core)
   * @param sourceCore Core to merge from, i.e. sdnnt
   * @param id identifier in source core
   * @param user User
   * @return 
   */ 
  public JSONObject mergeId(String sourceCore, String id, String user) {
    Options opts = Options.getInstance();
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
        ret.append("errors", "Found more than one record in catalog: " + docsCat.stream().map(d -> (String)d.getFirstValue("identifier")).collect(Collectors.joining()));
        return ret;
      } else {
        SolrDocument docCat = docsCat.get(0);
        String jsCat = (String) docCat.getFirstValue("raw");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode source = mapper.readTree(jsCat);

        // As target the sdnnt record
        JsonNode target = mapper.readTree(jsDnt);
        JsonNode fwPatch = JsonDiff.asJson(source, target);
        removeReplaceOpsForIgnoredFields(fwPatch);
        JsonNode bwPatch = JsonDiff.asJson(target, source);
        removeReplaceOpsForIgnoredFields(bwPatch);

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
  //      MarcRecord mr = MarcRecord.fromJSON(newRaw);      
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
    List<SolrInputDocument> hDocs = new ArrayList();
    List<SolrInputDocument> cDocs = new ArrayList();
    try (SolrClient solr = new ConcurrentUpdateSolrClient.Builder(opts.getString("solr.host", "http://localhost:8983/solr/")).build()) {
      int indexed = 0;
      String cursorMark = CursorMarkParams.CURSOR_MARK_START;
      SolrQuery q = new SolrQuery("*").setRows(1000)
              .setSort("identifier", SolrQuery.ORDER.asc)
              .setFields("marc_035a,marc_040a,controlfield_008,raw");
      if (from != null) {
        q.addFilterQuery("datestamp:["+from+" TO NOW]");
      }
      boolean done = false;
      while (!done) { 
        q.setParam(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
        QueryResponse qr = solr.query(sourceCore, q);
        String nextCursorMark = qr.getNextCursorMark();
        SolrDocumentList docs = qr.getResults();
        for(SolrDocument doc : docs) {
          if (doc.getFirstValue("marc_035a") != null) {
            mergeRaw(solr, 
                    (String) doc.getFirstValue("raw"), 
                    (String) doc.getFirstValue("marc_035a"), 
                    (String) doc.getFirstValue("marc_040a"), 
                    (String) doc.getFirstValue("controlfield_008"), 
                    user, hDocs, cDocs);
          }
        } 
        
        if (!hDocs.isEmpty()) {
          solr.add("history", hDocs);
          solr.add("catalog", cDocs);
          hDocs.clear();
          cDocs.clear();
        }
        indexed += docs.size();
        if (cursorMark.equals(nextCursorMark)) {
          done = true;
        }
        cursorMark = nextCursorMark;
        LOGGER.log(Level.INFO, "Current indexed: {0}", indexed);
      }
      solr.commit("history");
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
  
  private void mergeRaw(SolrClient solr, String jsDnt, String sysno, String sigla, String controlfield_008, String user, List<SolrInputDocument> hDocs, List<SolrInputDocument> cDocs) {
    try {
      
      SolrQuery q2 = new SolrQuery("*").setRows(1)
              .addFilterQuery("controlfield_001:\"" + sysno.substring(sysno.lastIndexOf(")") + 1) + "\"")
              .addFilterQuery("marc_040a:" + sigla)
              .addFilterQuery("controlfield_008:\"" + controlfield_008 + "\"")
              .setFields("identifier,raw");
      SolrDocumentList docsCat = solr.query("catalog", q2).getResults();
      if (docsCat.getNumFound() == 0) {
        ret.append("errors", "Record " + sysno + " not found in catalog");
        return;
      } else if (docsCat.getNumFound() > 1) {
        // ret.append("errors", "Found more than one record in catalog: " + docsCat.stream().map(d -> (String)d.getFirstValue("identifier")).collect(Collectors.joining()));
        ret.append("errors", "Found more than one record in catalog: " + sysno);
        return;
      } 
      SolrDocument docCat = docsCat.get(0);
      String jsCat = (String) docCat.getFirstValue("raw");
      
      ObjectMapper mapper = new ObjectMapper();
      JsonNode source = mapper.readTree(jsCat);
      
      // As target the sdnnt record
      JsonNode target = mapper.readTree(jsDnt);
      JsonNode fwPatch = JsonDiff.asJson(source, target);
      removeReplaceOpsForIgnoredFields(fwPatch);
      JsonNode bwPatch = JsonDiff.asJson(target, source);
      removeReplaceOpsForIgnoredFields(bwPatch);
      
//      ret.put("forward_patch", new JSONArray(fwPatch.toString()));
//      ret.put("backward_patch", new JSONArray(bwPatch.toString()));
      
            // Insert in history
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("identifier", docCat.getFirstValue("identifier"));
      idoc.setField("user", user);
      idoc.setField("type", "app");
      idoc.setField("changes", ret.toString());
      hDocs.add(idoc);
      // solr.commit("history");
      
      // Update record in catalog
      MarcRecord mr = MarcRecord.fromJSON(JsonPatch.apply(fwPatch, source).toString());
      mr.fillSolrDoc();
      cDocs.add(mr.toSolrDoc());
      // solr.commit("catalog");

      // ret.put("catalog_new", new JSONObject(JsonPatch.apply(fwPatch, source).toString()));
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.append("errors", ex);
    }
  }

  private void removeReplaceOpsForIgnoredFields(Iterable jsonPatch) {
    Iterator<JsonNode> patchIterator = jsonPatch.iterator();
    while (patchIterator.hasNext()) {
      JsonNode patchOperation = patchIterator.next();
      JsonNode operationType = patchOperation.get("op");
      JsonNode pathName = patchOperation.get("path");
      // if (operationType.asText().equals("replace") && ignoredFields.contains(pathName.asText())) {
      if (!dntSetFields.contains(pathName.asText())) {
        patchIterator.remove();
      }
    }
  }
  
  
  
  public JSONObject test(String sourceCore, String id) {
    Options opts = Options.getInstance();
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
      MarcRecord mr = MarcRecord.fromJSON(json);
      mr.fillSolrDoc();
      
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
