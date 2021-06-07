/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.indexer.models.User;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
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
      //String pwdHashed =  DigestUtils.sha256Hex(json.getString("pwd"))
      ret.put("username", username);
      User user = findUser(json.getString("user"));
      req.getSession(true).setAttribute("user", user);

      ret = user.toJSONObject();
      // Pridat aktivni zadosti pokud existujou stav=open
      JSONArray docs = UserController.getZadost(username);
      if (docs != null && !docs.isEmpty()) {
        ret.put("zadost", docs);
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

  public static JSONObject register(String js) {

    User old = findUser((new JSONObject(js)).getString("username"));
    if (old != null) {
      return new JSONObject().put("error", "username_exists");
    }
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

  public static JSONObject resetPwd(HttpServletRequest req, String js) {
    String newPwd = generatePwd();
    String username = (new JSONObject(js)).optString("username", "null");
    User user = getUser(req);
    if (!"null".equals(username)) {
      // Toto muze jen admin
      if ("admin".equals(user.role)) {
        user = findUser(username);
      }
    }
    if (user != null) {
      // Ulozit user a nove heslo poslat mailem
      user.pwd = DigestUtils.sha256Hex(newPwd);
      save(user);
    }
    return new JSONObject().put("pwd", newPwd);
  }

  public static String generatePwd() {
    String upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true);
    String lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true);
    String numbers = RandomStringUtils.randomNumeric(2);
    String specialChar = RandomStringUtils.random(2, 33, 47, false, false);
    String totalChars = RandomStringUtils.randomAlphanumeric(2);
    String combinedChars = upperCaseLetters.concat(lowerCaseLetters)
            .concat(numbers)
            .concat(specialChar)
            .concat(totalChars);
    List<Character> pwdChars = combinedChars.chars()
            .mapToObj(c -> (char) c)
            .collect(Collectors.toList());
    Collections.shuffle(pwdChars);
    String password = pwdChars.stream()
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .toString();
    return password;
  }

  public static JSONObject save(String js) {
    try {
      User user = User.fromJSON(js);
      return save(user);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  private static JSONObject save(User user) {

    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      solr.addBean("users", user);
      solr.commit("users");
      solr.close();
      return user.toJSONObject();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return new JSONObject().put("error", ex);
    }
  }

  public static JSONArray getZadost(String username) {
    try (SolrClient solr = new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      SolrQuery query = new SolrQuery("user:\"" + username + "\"")
              .addFilterQuery("state:open")
              .setFields("id", "identifiers", "typ", "user", "state", "navrh", "poznamka", "pozadavek", "datum_zadani", "datum_vyrizeni", "formular")
              .setRows(2);
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
