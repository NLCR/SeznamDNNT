/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class CatalogSearcher {

  public static final Logger LOGGER = Logger.getLogger(CatalogSearcher.class.getName());

  public JSONObject frbr(String id) {
    JSONObject ret = new JSONObject();
    try {
      SolrClient solr = Indexer.getClient();
      SolrQuery query = new SolrQuery("frbr:\"" + id + "\"");
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = solr.request(qreq, "catalog");
      ret = new JSONObject((String) qresp.get("response"));

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }
  public JSONObject search(HttpServletRequest req) {
    JSONObject ret = new JSONObject();
    try {
      SolrClient solr = Indexer.getClient();
      SolrQuery query = doQuery(req);
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = solr.request(qreq, "catalog");
      ret = new JSONObject((String) qresp.get("response"));
      if (ret.getJSONObject("response").getInt("numFound") > 0) {
        // Pridame info z zadosti
        List<String> ids = new ArrayList<>();
        for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
          JSONObject doc = (JSONObject) o;
          ids.add("\""+doc.getString("identifier")+"\"");
          //doc.put("zadost", findZadost(doc.getString("identifier")));
        }
        JSONArray zadosti = findZadosti(ids);
        ret.put("zadosti", zadosti);
      }
      
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  private JSONArray findZadosti(List<String> identifiers) {
    try {
      SolrClient solr = Indexer.getClient();
      String q = identifiers.toString().replace("[", "(").replace("]", ")").replaceAll(",", "");
           SolrQuery query = new SolrQuery("identifiers:" + q)
      //SolrQuery query = new SolrQuery("identifiers:\"" + identifier + "\"")
              .setFields("*,process:[json]");
  
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = solr.request(qreq, "zadost");
      return (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  private JSONObject findZadost(String identifier) {
    try {
      SolrClient solr = Indexer.getClient();
      SolrQuery query = new SolrQuery("identifiers:\"" + identifier + "\"")
              .setFields("*,process:[json]");
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = solr.request(qreq, "zadost");
      JSONArray docs = (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
      if (docs.isEmpty()) {
        return null;
      } else {
        return docs.optJSONObject(0);
      }
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  private SolrQuery doQuery(HttpServletRequest req) {
    String q = req.getParameter("q");
    if (q == null) {
      q = "*";
    }
    Options opts = Options.getInstance();
    int rows = opts.getClientConf().getInt("rows"); 
    if (req.getParameter("rows") != null) {
      rows = Integer.parseInt(req.getParameter("rows"));
    }
    int start = 0; 
    if (req.getParameter("page") != null) {
      start = Integer.parseInt(req.getParameter("page")) * rows;
    }
    SolrQuery query = new SolrQuery(q)
            .setRows(rows)
            .setStart(start) 
            .setFacet(true).addFacetField("item_type", "language", "marc_990a", "marc_910a", "nakladatel")
            .setFacetMinCount(1)
            .setParam("json.nl", "arrntv")
            .setParam("stats", true)
            .setParam("stats.field","rokvydani")
            .setParam("q.op", "AND")
            .setFields("*,raw:[json]");
    
    if (!Boolean.parseBoolean(req.getParameter("onlyCzech"))) {
      // Filtrujeme defaultne kdyz neni parametr a kdyz je true
      query.addFilterQuery("language:cze");
    }
    
    if (req.getParameter("q") != null) {
      query.setParam("df", "fullText");
    }
    if (req.getParameter("sort") != null) {
      query.setParam("sort", req.getParameter("sort"));
    }
    if (Boolean.parseBoolean(req.getParameter("onlySdnnt"))) {
      // Filtrujeme defaultne kdyz neni parametr a kdyz je true
      query.addFilterQuery("-marc_990a:NNN");
    }
    for (Object o : opts.getClientConf().getJSONArray("filterFields")) {
      String field = (String) o;
      if (req.getParameter(field) != null) {
        if (field.equals("rokvydani")) {
          query.addFilterQuery(field + ":[" + req.getParameter(field).replace(",", " TO ") + "]");
        } else {
          query.addFilterQuery(field + ":\"" + req.getParameter(field) + "\"");
        }
        
      }
    }
    return query;
  }

}
