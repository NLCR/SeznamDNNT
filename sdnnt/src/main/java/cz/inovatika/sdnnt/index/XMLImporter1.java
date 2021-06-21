/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.lang.time.DurationFormatUtils;
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

/**
 *
 * @author alberto
 */
public class XMLImporter {

  public static final Logger LOGGER = Logger.getLogger(XMLImporter.class.getName());
  JSONObject ret = new JSONObject();
  String collection = "imports";
  SolrClient solr;

  String import_id;
  String import_date;
  String import_url;
  String import_origin;

  int indexed;

//  Map<String, String> fieldsMap = Map.ofEntries(
//  new AbstractMap.SimpleEntry<>("name", "John"),
//  new AbstractMap.SimpleEntry<>("city", "budapest"),
//  new AbstractMap.SimpleEntry<>("zip", "000000"),
//  new AbstractMap.SimpleEntry<>("home", "1231231231")
//  );
  List<String> elements = Arrays.asList("NAME", "EAN", "AUTHOR");

  public JSONObject fromUrl(String url) {
    readUrl(url);
    return ret;
  }

  public JSONObject fromFile(String uri, String origin) {
    LOGGER.log(Level.INFO, "Processing {0}", uri);
    try {
      long start = new Date().getTime();
      // solr = new ConcurrentUpdateSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
      solr = getClient();
      ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
      import_date = now.format(DateTimeFormatter.ISO_INSTANT);
      import_url = uri;
      import_origin = origin;
      import_id = now.toEpochSecond() + "";
      File f = new File(uri);
      InputStream is = new FileInputStream(f);
      readXML(is);
      solr.commit(collection);
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
    } finally {
      try {
        solr.close();
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
    return ret;
  }

  private void readUrl(String url) {
    try (SolrClient solr = new ConcurrentUpdateSolrClient.Builder(Options.getInstance().getString("solr.host")).build()) {
      CloseableHttpClient client = HttpClients.createDefault();
      HttpGet httpGet = new HttpGet(url);
      try (CloseableHttpResponse response1 = client.execute(httpGet)) {
        final HttpEntity entity = response1.getEntity();
        if (entity != null) {
          try (InputStream is = entity.getContent()) {
            readXML(is);
          }
        }
      } catch (XMLStreamException | IOException exc) {
        LOGGER.log(Level.SEVERE, null, exc);
        ret.put("error", exc);
      }
      solr.commit(collection);
      solr.close();
    } catch (SolrServerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      ret.put("error", ex);
    }

  }

  private void readXML(InputStream is) throws XMLStreamException {

    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    XMLStreamReader reader = null;
    try {
      reader = inputFactory.createXMLStreamReader(is);
      readDocument(reader);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Reads OAI XML document
   *
   * @param reader
   * @return resuptionToken or null
   * @throws XMLStreamException
   * @throws IOException
   */
  private void readDocument(XMLStreamReader reader) throws XMLStreamException, IOException {
    while (reader.hasNext()) {
      int eventType = reader.next();
      switch (eventType) {
        case XMLStreamReader.START_ELEMENT:
          String elementName = reader.getLocalName();
          if (elementName.equals("SHOP")) {
            readShop(reader);
          } else if (elementName.equals("error")) {
            ret.put("error", reader.getElementText());
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          break;
      }
    }
    // throw new XMLStreamException("Premature end of file");
  }

  private void readShop(XMLStreamReader reader) throws XMLStreamException, IOException {
    while (reader.hasNext()) {
      int eventType = reader.next();
      switch (eventType) {
        case XMLStreamReader.START_ELEMENT:
          String elementName = reader.getLocalName();
          if (elementName.equals("ITEM")) {
            readItem(reader);
          } else if (elementName.equals("error")) {
            ret.put("error", reader.getElementText());
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          break;
      }
    }
    // throw new XMLStreamException("Premature end of file");
  }

  private void readItem(XMLStreamReader reader) throws XMLStreamException, IOException {
    // Format record
    /*
    <ITEM>
    <NAME>Tintin 8 - Žezlo krále Ottokara</NAME>
    <EAN>9788000046518</EAN>
    <VAT>10%</VAT>
    <PRICEVAT>169,00</PRICEVAT>
    <AVAILABILITY>1</AVAILABILITY>
    <AVAILABILITY_DETAIL>77</AVAILABILITY_DETAIL>
    <AMOUNT_CENTRAL_AVAILABLE>0</AMOUNT_CENTRAL_AVAILABLE>
    <DATEEXP>2017-08-31</DATEEXP>
    <REPRINTING_DATE p3:nil="true" xmlns:p3="http://www.w3.org/2001/XMLSchema-instance" />
    <TYPE>Kniha</TYPE>
    <SUBTITLE p3:nil="true" xmlns:p3="http://www.w3.org/2001/XMLSchema-instance" />
    <AUTHOR>Hergé</AUTHOR>
    <ANNOTATION>Neohrožený reportér se s pejskem Filutou tentokrát vydává do Syldávie, aby tam doprovázel podivného profesora a znalce starých pečetí. Přitom narazí na spiknutí proti syldavskému králi a v krkolomné honičce pronásleduje lupiče starého žezla, bez něhož prý žádný syldavský král nemůže vládnout.</ANNOTATION>
    <PUBLISHING>ALBATROS</PUBLISHING>
    <IMAGE>https://cdn.albatrosmedia.cz/Images/Product/38272824/?width=300&amp;height=450&amp;ts=636575456798130000</IMAGE>
    <IMAGE_LARGE>https://cdn.albatrosmedia.cz/Images/Product/38272824/?ts=636575456798130000</IMAGE_LARGE>
    <PAGES>64</PAGES>
    <BOOKBINDING>brožovaná lepená</BOOKBINDING>
    <DIMENSION>220 x 290 mm</DIMENSION>
    <RELEASE>0</RELEASE>
    <EDITION p3:nil="true" xmlns:p3="http://www.w3.org/2001/XMLSchema-instance" />
    <LANGUAGE>čeština</LANGUAGE>
    <GENRE>komiks</GENRE>
    <CATEGORY>Komiks</CATEGORY>
    <AGE>8</AGE>
    <SERIES>Tintinova dobrodružství</SERIES>
    <TRANSLATOR>Kateřina Vinšová</TRANSLATOR>
    <INTERPRETER />
  </ITEM>
     */

    Map<String, String> item = new HashMap<>();
    while (reader.hasNext()) {
      int eventType = reader.next();
      switch (eventType) {
        case XMLStreamReader.START_ELEMENT:
          String elementName = reader.getLocalName();
          String val = reader.getElementText();
//          System.out.println(elementName);
//          System.out.println(val);
          if (elements.contains(elementName)) {
            item.put(elementName, val);
          }

          break;
        case XMLStreamReader.END_ELEMENT:
          elementName = reader.getLocalName();
          if (elementName.equals("ITEM")) {
            addDedup(item);
            addFrbr(item);
            findInCatalog(item);
            toIndex(item);
            // ret.append("items", item);
            return;
          }
      }
    }
  }

  private void toIndex(Map item) {
    try {
      SolrInputDocument idoc = new SolrInputDocument();
      idoc.setField("import_id", import_id);
      idoc.setField("import_date", import_date);
      idoc.setField("import_url", import_url);
      idoc.setField("import_origin", import_origin);

      idoc.setField("id", import_id + "_" + item.get("EAN"));
      idoc.setField("ean", item.get("EAN"));
      idoc.setField("name", item.get("NAME"));
      if (item.containsKey("AUTHOR")) {
        idoc.setField("author", item.get("AUTHOR"));
      }

      idoc.setField("identifiers", item.get("identifiers"));
      idoc.setField("na_vyrazeni", item.get("na_vyrazeni"));
      idoc.setField("hits_na_vyrazeni", item.get("hits_na_vyrazeni"));
      idoc.setField("catalog", item.get("catalog"));
      idoc.setField("num_hits", item.get("num_hits"));
      idoc.setField("hit_type", item.get("hit_type"));
      idoc.setField("item", new JSONObject(item).toString());

      
      solr.add("imports", idoc);
      
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
    if (item.containsKey("AUTHOR")) {
      frbr += item.get("AUTHOR");
    }

    frbr += "/" + item.get("NAME");

    item.put("frbr", MD5.normalize(frbr));
  }

  public void findInCatalog(Map item) {
    // JSONObject ret = new JSONObject();
    try {
      String title = "title:(" + ClientUtils.escapeQueryChars((String) item.get("NAME")) + ")";
      if (item.containsKey("AUTHOR") && !((String) item.get("AUTHOR")).isBlank()) {
        title = " OR (" + title + " AND author:(" + ClientUtils.escapeQueryChars((String) item.get("AUTHOR")) + "))";
      } else {
        title = " OR " + title;
      }
      String q = "ean:\"" + item.get("EAN") + "\""
              + " OR dedup_fields:\"" + item.get("dedup_fields") + "\""
              + " OR frbr:\"" + item.get("frbr") + "\""
              + title;
      
      SolrQuery query = new SolrQuery(q)
              .setRows(20)
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
        if (doc.optString("marc_990a").equals("A")) {
          na_vyrazeni.add(doc.getString("identifier"));
        }

        if (doc.has("ean")) {
          List<Object> eans = doc.getJSONArray("ean").toList();
          if (eans.contains(item.get("EAN"))) {
            isEAN = true;
            identifiers.add(doc.getString("identifier"));
          }
        }
        if (!isEAN) {
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

  private void skipElement(XMLStreamReader reader, String name) throws XMLStreamException {

    while (reader.hasNext()) {
      int eventType = reader.next();
      switch (eventType) {
        case XMLStreamReader.END_ELEMENT:
          String elementName = reader.getLocalName();
          if (elementName.equals(name)) {
            //LOGGER.log(Level.INFO, "eventType: {0}, elementName: {1}", new Object[]{eventType, elementName});
            return;
          }
      }
    }
//    throw new XMLStreamException("Premature end of file");
  }

}
