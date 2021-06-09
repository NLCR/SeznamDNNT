/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import java.io.IOException;
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

      // Pridame info z zadosti
      for (Object o : ret.getJSONObject("response").getJSONArray("docs")) {
        JSONObject doc = (JSONObject) o;
        doc.put("zadost", findZadost(doc.getString("identifier")));
      }
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  private JSONObject findZadost(String identifier) {
    try {
      SolrClient solr = Indexer.getClient();
      SolrQuery query = new SolrQuery("identifiers:\"" + identifier + "\"")
              .addFilterQuery("state:waiting");
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
    SolrQuery query = new SolrQuery(q)
            .setRows(rows)
            .setParam("df", "fullText")
            .setFacet(true).addFacetField("item_type", "marc_990a", "marc_910a", "nakladatel")
            .setFacetMinCount(1)
            .setParam("json.nl", "arrntv")
            .setParam("stats", true)
            .setParam("stats.field","rokvydani")
            .setFields("*,raw:[json]");
    
    if (req.getParameter("sort") != null) {
      query.setParam("sort", req.getParameter("sort"));
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
