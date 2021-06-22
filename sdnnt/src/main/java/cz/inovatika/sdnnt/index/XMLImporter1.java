/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import static cz.inovatika.sdnnt.index.Indexer.getClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author alberto
 */
public class XMLImporter1 {

  public static final Logger LOGGER = Logger.getLogger(XMLImporter1.class.getName());
  JSONObject ret = new JSONObject();
  String collection = "imports";

  String import_id;
  String import_date;
  String import_url;
  String import_origin;

  int indexed;

  Map<String, String> fieldsMap = Map.ofEntries(
          new AbstractMap.SimpleEntry<>("NAME", "PRODUCTNAME"),
          new AbstractMap.SimpleEntry<>("ISBN", "ISBN"));
  List<String> elements = Arrays.asList("NAME", "EAN");

  public JSONObject fromFile(String uri, String origin, String itemName) {
    LOGGER.log(Level.INFO, "Processing {0}", uri);
    try {
      long start = new Date().getTime();
      // solr = new ConcurrentUpdateSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      import_date = now.format(DateTimeFormatter.ISO_INSTANT);
      import_url = uri;
      import_origin = origin;
      import_id = now.toEpochSecond() + "";
      File f = new File(uri);
      InputStream is = new FileInputStream(f);
      readXML(is, itemName);
      getClient().commit(collection);
      // solr.close();
      ret.put("indexed", indexed);
      ret.put("file", uri);
      ret.put("origin", origin);
      String ellapsed = DurationFormatUtils.formatDurationHMS(new Date().getTime() - start);
      ret.put("ellapsed", ellapsed);
      LOGGER.log(Level.INFO, "FINISHED {0}", indexed);
    } catch (XMLStreamException | SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    } 
    return ret;
  }

  private void readXML(InputStream is, String itemName) throws XMLStreamException {

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(is);
      NodeList nodes = doc.getElementsByTagName(itemName);
      for (int i = 0; i < nodes.getLength(); i++) {
        Node node = nodes.item(i);
        Map<String, String> item = new HashMap<>();
        NodeList fields = node.getChildNodes();
        for (int j = 0; j < fields.getLength(); j++) {
          Node field = fields.item(j);
          if (field.getNodeType() == Node.ELEMENT_NODE) {
//            System.out.println(field.getNodeType() + " -> " 
//                  + field.getNodeName() + " -> " + field.getTextContent());
            item.put(field.getNodeName(), field.getTextContent());

          }
        }
        if (item.containsKey("ISBN")) {

          ISBNValidator isbn = ISBNValidator.getInstance();
          String ean = item.get("ISBN");
          ean = isbn.validate(ean);
          if (ean != null) {
            item.put("EAN", ean);
          }  
        }
        addDedup(item);
        addFrbr(item);
        findInCatalog(item);
        toIndex(item);
      }
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private void toIndex(Map item) {
    try {
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("import_id", import_id);
      idoc.setField("import_date", import_date);
      idoc.setField("import_url", import_url);
      idoc.setField("import_origin", import_origin);

      idoc.setField("id", import_id + "_" + item.get("ITEM_ID"));
      idoc.setField("ean", item.get("EAN"));
      idoc.setField("name", item.get(fieldsMap.get("NAME")));
      if (item.containsKey(fieldsMap.get("AUTHOR"))) {
        idoc.setField("author", item.get(fieldsMap.get("AUTHOR")));
      }

      idoc.setField("identifiers", item.get("identifiers"));
      idoc.setField("na_vyrazeni", item.get("na_vyrazeni"));
      idoc.setField("hits_na_vyrazeni", item.get("hits_na_vyrazeni"));
      idoc.setField("catalog", item.get("catalog"));
      idoc.setField("num_hits", item.get("num_hits"));
      idoc.setField("hit_type", item.get("hit_type"));
      idoc.setField("item", new JSONObject(item).toString());

      getClient().add("imports", idoc);

      indexed++;
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private void addDedup(Map item) {
    item.put("dedup_fields", "");
  }

  private void addFrbr(Map item) {
    String frbr = "";
    if (item.containsKey(fieldsMap.get("AUTHOR"))) {
      frbr += item.get(fieldsMap.get("AUTHOR"));
    }

    frbr += "/" + item.get(fieldsMap.get("NAME"));

    item.put("frbr", MD5.normalize(frbr));
  }

  public void findInCatalog(Map item) {
    // JSONObject ret = new JSONObject();
    try {
      String title = "title:(" + ClientUtils.escapeQueryChars((String) item.get(fieldsMap.get("NAME"))) + ")";
      if (item.containsKey(fieldsMap.get("AUTHOR")) && !((String) item.get(fieldsMap.get("AUTHOR"))).isBlank()) {
        title = " OR (" + title + " AND author:(" + ClientUtils.escapeQueryChars((String) item.get(fieldsMap.get("AUTHOR"))) + "))";
      } else {
        title = " OR " + title;
      }
      String q = "ean:\"" + item.get("EAN") + "\""
              + " OR dedup_fields:\"" + item.get("dedup_fields") + "\""
      //        + " OR frbr:\"" + item.get("frbr") + "\""
              + title;

      SolrQuery query = new SolrQuery(q)
              .setRows(100)
              .setParam("q.op", "AND")
              // .setFields("*,score");
              .setFields("identifier,title,score,ean,frbr,marc_990a");
//      SolrDocumentList docs = getClient().query("catalog", query).getResults();
//      for (SolrDocument doc : docs) {
//      }
      QueryRequest qreq = new QueryRequest(query);
      NoOpResponseParser rParser = new NoOpResponseParser();
      rParser.setWriterType("json");
      qreq.setResponseParser(rParser);
      NamedList<Object> qresp = getClient().request(qreq, "catalog");
      JSONObject jresp = (new JSONObject((String) qresp.get("response"))).getJSONObject("response");
      JSONArray docs = jresp.getJSONArray("docs");
      item.put("catalog", docs.toString());
      List<String> identifiers = new ArrayList<>();
      List<String> na_vyrazeni = new ArrayList<>();
      boolean isEAN = false;
      for (Object o : docs) {
        JSONObject doc = (JSONObject) o;

        if (doc.has("ean")) {
          List<Object> eans = doc.getJSONArray("ean").toList();
          if (eans.contains(item.get("EAN"))) {
            isEAN = true;
            
            if (doc.has("marc_990a") && doc.getJSONArray("marc_990a").toList().contains("A")) {
              na_vyrazeni.add(doc.getString("identifier"));
            }
            identifiers.add(doc.getString("identifier"));
          }
        }
        if (!isEAN) {
          if (doc.optString("marc_990a").equals("A")) {
            na_vyrazeni.add(doc.getString("identifier"));
          }
          identifiers.add(doc.getString("identifier"));
        }

      }
      item.put("hit_type", isEAN ? "ean" : "noean");

      item.put("num_hits", isEAN ? identifiers.size() : jresp.getInt("numFound"));

      item.put("identifiers", identifiers);
      item.put("na_vyrazeni", na_vyrazeni);
      item.put("hits_na_vyrazeni", na_vyrazeni.size());

    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    // return ret;
  }

}
