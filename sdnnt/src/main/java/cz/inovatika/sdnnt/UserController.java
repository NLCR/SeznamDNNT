/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.indexer.models.User;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class UserController {

  public static final Logger LOGGER = Logger.getLogger(UserController.class.getName());

  public static JSONObject login(HttpServletRequest req) {
    JSONObject ret = new JSONObject();
    try {
      JSONObject json = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
      
      // TODO Authentication. Ted prihlasime kazdeho
      String username = json.getString("user");
      ret.put("username", username);
      User user = findUser(json.getString("user"));
      req.getSession(true).setAttribute("user", user);
      
      ret = user.toJSONObject();
      // Pridat aktivni zadost pokud existuje stav=open
      JSONArray docs = UserController.getZadost(username);
      if (docs != null && !docs.isEmpty()) {
        ret.put("zadost", docs.getJSONObject(0));
      }
      return ret;
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public static User dummy(String name) {
    JSONObject ret = new JSONObject();
    try {
      // TODO Authentication. Ted prihlasime kazdeho
      return findUser(name);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static JSONObject logout(HttpServletRequest req) {
    JSONObject ret = new JSONObject();
    try {
      req.getSession().invalidate();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }
    return ret;
  }

  public static User getUser(HttpServletRequest req) {
    return (User) req.getSession(true).getAttribute("user");
  }

  public static JSONObject getAll(HttpServletRequest req) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      SolrQuery query = new SolrQuery("*:*")
              .setRows(100);
      QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "users"); 
          solr.close();
          return new JSONObject((String) qresp.get("response")).getJSONObject("response");
      
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }

  public static User findUser(String username) {

    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      SolrQuery query = new SolrQuery("username:\"" + username + "\"")
              .setRows(1);
      List<User> users = solr.query("users", query).getBeans(User.class);
      solr.close();
      if (users.isEmpty()) {
        return null;
      } else {
        return users.get(0);
      }
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }
  
  

  public static JSONObject save(String js) {

    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      User user = User.fromJSON(js);
      
      solr.addBean("users", user);
      solr.commit("users");
      solr.close();
      return new JSONObject(js);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }
  

  public static JSONArray getZadost(String username) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      SolrQuery query = new SolrQuery("user:\"" + username + "\"")
              .addFilterQuery("state:open")
              .setFields("id","identifiers","typ","user","state","new_stav","poznamka","pozadavek","datum_zadani","datum_vyrizeni","formular")
              .setRows(1);
      QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "zadost"); 
          solr.close();
          return new JSONObject((String) qresp.get("response")).getJSONObject("response").optJSONArray("docs");
      
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return null;
    }
  }
  
}
