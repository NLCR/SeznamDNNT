/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.indexer.models.User;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
      // query.addFilterQuery("language:cze");
    }
    
    if (req.getParameter("q") != null) {
      query.setParam("df", "fullText");
    }
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
    

      // Vseobecne filtry podle misto vydani (xr ) a roky
      
      // fq=fmt:BK%20AND%20place_of_pub:"xr%20"%20AND%20date1_int:%5B1910%20TO%202007%5D&fq=marc_338a:svazek&fq=-marc_245h:*&fq=marc_338b:nc&fq=marc_3382:rdacarrier
      int year = Calendar.getInstance().get(Calendar.YEAR);
      int fromYear = opts.getJSONObject("search").getInt("fromYear");
      int yearsBK = opts.getJSONObject("search").getInt("yearsBK");
      String bkDate = "((date1_int:["
              + fromYear 
              + " TO "
              + (year - yearsBK) 
              + "] AND -date2_int:*"
              + ") OR " + "(date1_int:["
              + fromYear 
              + " TO "
              + (year - yearsBK) 
              + "] AND date2_int:["
              + fromYear 
              + " TO "
              + (year - yearsBK) 
              + "]))";
      
      String bk = "(fmt:BK AND place_of_pub:\"xr \" AND "
              + bkDate 
              + " AND marc_338a:svazek AND marc_338b:nc AND marc_3382:rdacarrier AND -marc_245h:*)";
      
      
      int yearsSE = opts.getJSONObject("search").getInt("yearsSE");
      String seDate = "((date1_int:["
              + fromYear 
              + " TO "
              + (year - yearsSE) 
              + "] AND date2_int:9999"
              + ") OR " + "date2_int:["
              + fromYear 
              + " TO "
              + (year - yearsSE) 
              + "])";
      String se = "(fmt:SE AND place_of_pub:\"xr \" AND "
              + seDate 
              + " AND marc_338a:svazek AND marc_338b:nc AND marc_3382:rdacarrier AND -marc_245h:*)";
      
      
      query.addFilterQuery(bk + " OR " + se);
      
    // Filtry podle role
    User user = UserController.getUser(req);
    if (!Boolean.parseBoolean(req.getParameter("fullCatalog")) || user == null || "user".equals(user.role)) {
      // Filtrujeme defaultne kdyz neni parametr a kdyz je true
      // Z UI a podle user role
      query.addFilterQuery("-marc_990a:NNN");
    } else {
    }
     
    return query;
  }

}
